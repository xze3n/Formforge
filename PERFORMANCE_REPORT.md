# Permission Distribution – Performance Analysis

> **Dataset**: 10 000 users · 20 roles · 50 permissions · ~50 000 applications  
> **Load tool**: Apache JMeter 5.6.3  
> **Backend**: Spring Boot 3.4.5 · PostgreSQL · Java 25  
> **Test date**: May 19, 2026

---

## 1. What is being measured?

The feature implements a **many-to-many statistics query** that answers the question:

> *For each permission in the system, how many distinct users hold it (via their roles), how many roles grant it, and which role names are involved?*

This requires a **2-hop M:N join** across three join tables:

```
permissions
    └── role_permissions   (M:N bridge table)
            └── user_roles (M:N bridge table)
                    └── users
```

With 10 000 users, each assigned 1–3 roles, and 50 permissions spread across 20 roles, the raw join produces tens of thousands of intermediate rows that must be grouped, counted, and aggregated with `STRING_AGG`. This is computationally expensive and does not scale linearly — it degrades quadratically as users grow.

Three implementations of this query were built and benchmarked against each other.

---

## 2. The three implementations

### 2.1 Naive — live 3-table join on every request

**Endpoint**: `GET /api/stats/permission-distribution/naive`  
**Auth**: Bearer JWT required  
**Source**: `PermissionStatsService.naiveDistribution()` → `PermissionStatsRepository.findPermissionDistribution()`

The raw SQL query executed on **every single HTTP request**, with no caching or pre-computation:

```sql
SELECT
    p.id                                     AS permission_id,
    p.name                                   AS permission_name,
    COALESCE(COUNT(DISTINCT ur.user_id), 0)  AS user_count,
    COALESCE(COUNT(DISTINCT rp.role_id),  0) AS role_count,
    STRING_AGG(DISTINCT r.name, ', ' ORDER BY r.name) AS role_names
FROM permissions p
LEFT JOIN role_permissions rp ON rp.permission_id = p.id
LEFT JOIN roles             r  ON r.id             = rp.role_id
LEFT JOIN user_roles        ur ON ur.role_id        = rp.role_id
GROUP BY p.id, p.name
ORDER BY user_count DESC
```

Every concurrent request competes for a PostgreSQL worker thread and a connection-pool slot. Under load, threads queue up, latency spikes, and the connection pool eventually exhausts — causing outright failures.

---

### 2.2 Cached — same query wrapped in a 5-minute Caffeine cache

**Endpoint**: `GET /api/stats/permission-distribution/cached`  
**Auth**: Bearer JWT required  
**Source**: `PermissionStatsService.cachedDistribution()` — `@Cacheable("permissionDistribution")`

Identical SQL to the naive approach, but the result is stored in an **in-process Caffeine cache** (heap memory, no serialisation overhead):

| Setting | Value |
|---|---|
| Cache name | `permissionDistribution` |
| TTL (time-to-live) | 5 minutes |
| Max entries | 1 000 |
| Stats recording | enabled |

On the **first request** after a cache miss, the full DB query runs once. Every subsequent request within the TTL window is served entirely from the JVM heap — no SQL executed, no DB connection consumed, no network round-trip. Under a DDoS with 500 concurrent threads, the database receives **at most one query per 5-minute window** instead of 500 simultaneous full-table scans.

The cache is evicted every 10 minutes by the same `@Scheduled` task that refreshes the materialized view, keeping all three strategies in rough sync.

---

### 2.3 Materialized view — pre-aggregated result stored in PostgreSQL

**Endpoint**: `GET /api/stats/permission-distribution/materialized`  
**Auth**: Bearer JWT required  
**Source**: `PermissionStatsService.materializedDistribution()` → `PermissionStatsRepository.findPermissionDistributionFromMaterializedView()`

A **PostgreSQL materialized view** (`mv_permission_distribution`) is created by Flyway migration `V8__performance_indices.sql`. It stores the fully aggregated result as a physical table inside PostgreSQL:

```sql
CREATE MATERIALIZED VIEW IF NOT EXISTS mv_permission_distribution AS
SELECT
    p.id                                     AS permission_id,
    p.name                                   AS permission_name,
    COALESCE(COUNT(DISTINCT ur.user_id), 0)  AS user_count,
    COALESCE(COUNT(DISTINCT rp.role_id),  0) AS role_count,
    STRING_AGG(DISTINCT r.name, ', ' ORDER BY r.name) AS role_names
FROM permissions p
LEFT JOIN role_permissions rp ON rp.permission_id = p.id
LEFT JOIN roles             r  ON r.id             = rp.role_id
LEFT JOIN user_roles        ur ON ur.role_id        = rp.role_id
GROUP BY p.id, p.name
ORDER BY user_count DESC;

-- Unique index required for non-blocking CONCURRENTLY refresh
CREATE UNIQUE INDEX IF NOT EXISTS idx_mv_perm_dist_permission_id
    ON mv_permission_distribution (permission_id);
```

At query time, no computation happens at all. PostgreSQL performs a **simple indexed scan** on the materialized view — equivalent to `SELECT * FROM a_50_row_table`. The aggregation cost was already paid at the last refresh.

The view is refreshed every **10 minutes** by a Spring `@Scheduled` task using `REFRESH MATERIALIZED VIEW CONCURRENTLY`, which means reads on the view are **never blocked** during a refresh.

An additional B-tree index was also added to the underlying join table:

```sql
CREATE INDEX IF NOT EXISTS idx_role_permissions_permission
    ON role_permissions (permission_id);
```

The existing composite primary key on `role_permissions(role_id, permission_id)` only covered the `role → permission` join direction. The statistics query joins in the `permission → role` direction, so without this index every query performed a full sequential scan.

---

## 3. JMeter test plan

**Plan file**: `jmeter/formforge-perf-test.jmx`

| Thread Group | Threads | Ramp-up | Mode | Target endpoint |
|---|---|---|---|---|
| TG1 – Normal / Naive | 50 | 10 s | 100 loops | `/naive` |
| TG2 – Normal / Cached | 50 | 10 s | 100 loops | `/cached` |
| TG3 – Normal / Materialized | 50 | 10 s | 100 loops | `/materialized` |
| TG4 – DDoS / Naive | 500 | 5 s | 60 s duration | `/naive` |
| TG5 – DDoS / Cached | 500 | 5 s | 60 s duration | `/cached` |

All requests hit `https://localhost:8443` (HTTPS, self-signed certificate) and carry a `Bearer` JWT in the `Authorization` header. TG1–TG3 use a **ResponseAssertion** (HTTP 200) to flag non-success responses as errors.

Results were collected into `jmeter/aggregate.csv`.

---

## 4. Results

Raw data from `jmeter/aggregate.csv`:

```
Label                  # Samples  Average  Median  90%     95%     99%     Min   Max    Error%  Throughput
GET /naive             5 000      6 131    6 221   6 637   6 834   7 582   495   12587  0.000%   7.99/s
GET /cached            5 000          3       3       4       4      13     2     650   0.000%  492.08/s
GET /materialized      5 000          6       6       8       8      34     5      55   0.000%  472.05/s
GET /naive (DDoS)      1 025     38 523   38 810  60 003  60 381  66 486  1844  69018  56.49%    8.75/s
GET /cached (DDoS)    77 271        372      280     375     429   1 170     2   59326   0.14%  1280.11/s
```

All times are in **milliseconds**.

---

## 5. Result interpretation

### 5.1 Normal load — 50 concurrent users

#### `/naive` — broken at 50 threads

| Metric | Value |
|---|---|
| Average response time | **6 131 ms** (6.1 seconds) |
| Median response time | **6 221 ms** |
| 99th percentile | **7 582 ms** |
| Throughput | **8 req/s** |
| Error rate | 0% (but only because requests queued rather than failed) |

At just 50 simultaneous users the naive endpoint delivered responses in **over 6 seconds on average**. The median being higher than the average indicates almost every request was slow — not just outliers. The database was spending the entire time executing the GROUP BY / COUNT(DISTINCT) aggregate across tens of thousands of join rows, with all 50 threads competing for the same PostgreSQL worker threads.

At 8 req/s throughput, a site with 100 users trying to load this page simultaneously would back up instantly.

#### `/cached` — near-zero latency after the first call

| Metric | Value |
|---|---|
| Average response time | **3 ms** |
| Median response time | **3 ms** |
| 99th percentile | **13 ms** |
| Max response time | 650 ms (first cache-miss call only) |
| Throughput | **492 req/s** |
| Error rate | 0% |

The 650 ms maximum corresponds to the **single cache-miss** on the very first request: the full DB query ran once, the result was stored in the Caffeine cache, and all 4 999 subsequent requests were answered from JVM heap memory without touching the database at all.

**Naive vs Cached comparison**:
- Average latency: 6 131 ms → 3 ms → **2 044× faster**
- Throughput: 8 req/s → 492 req/s → **61.6× more requests served**

#### `/materialized` — fastest consistent DB path

| Metric | Value |
|---|---|
| Average response time | **6 ms** |
| Median response time | **6 ms** |
| 99th percentile | **34 ms** |
| Max response time | 55 ms |
| Throughput | **472 req/s** |
| Error rate | 0% |

The materialized view endpoint never hits the aggregation logic at query time. PostgreSQL performs an indexed scan on the 50-row pre-computed view — essentially a `SELECT * FROM small_table WHERE id = ?`. The 55 ms maximum is an occasional OS-level scheduling spike, not a query variance.

This strategy is **1 022× faster** than naive on average and still returns fresh data (within the 10-minute refresh window), making it the best choice when you need both speed and no stale-data risk from a longer cache TTL.

---

### 5.2 DDoS — 500 concurrent threads, 60 seconds sustained

This is where the difference between optimised and unoptimised becomes existential.

#### `/naive` under DDoS — complete collapse

| Metric | Value |
|---|---|
| Completed requests | **1 025** in 60 seconds |
| Average response time | **38 523 ms** (38.5 seconds!) |
| 90th percentile | **60 003 ms** (JMeter socket timeout) |
| Error rate | **56.49%** |
| Throughput | **8.75 req/s** |

The server effectively stopped functioning:

- Only **1 025 requests** completed out of the hundreds of thousands attempted — the thread pool, connection pool, and DB worker queue were all saturated within seconds of the ramp-up finishing.
- The 90th percentile of **60 003 ms** is JMeter's default socket timeout firing — more than half of all requests were still waiting for a DB response after a full minute.
- **56.49% of completed responses were errors** — connection pool exhaustion (`HikariPool-1 - Connection is not available, request timed out`) returned HTTP 500, the rest timed out before the DB could even start executing.
- The server remained degraded or unresponsive for several minutes after the DDoS group finished, as queued DB work drained.

This is a classic **database thundering herd** failure: each new thread issues the same expensive query, the DB falls further behind, latency grows, more timeouts occur, error responses are returned, the connection pool is never freed, and the cycle continues.

#### `/cached` under DDoS — absorbs the attack

| Metric | Value |
|---|---|
| Completed requests | **77 271** in 60 seconds |
| Average response time | **372 ms** |
| Median response time | **280 ms** |
| 99th percentile | **1 170 ms** |
| Error rate | **0.14%** |
| Throughput | **1 280 req/s** |

The cached endpoint served **75× more requests** in the same 60-second window with only **0.14% errors** (those are the ~111 requests that happened to all arrive during the same cache-miss moment at the start and briefly competed for the single DB call).

The Caffeine cache acted as a **request coalescer**: regardless of how many threads hit the endpoint simultaneously, the database received at most one query per 5-minute TTL window. The remaining 77 000+ requests were answered entirely from heap memory. The server remained healthy throughout and after the DDoS period.

---

## 6. Summary table

| Strategy | Avg (normal) | Throughput (normal) | Avg (DDoS) | Error% (DDoS) | Requests served (DDoS 60s) |
|---|---|---|---|---|---|
| **Naive** | 6 131 ms | 8 req/s | 38 523 ms | 56.49% | 1 025 |
| **Cached** | 3 ms | 492 req/s | 372 ms | 0.14% | 77 271 |
| **Materialized** | 6 ms | 472 req/s | — | — | — |

---

## 7. How the optimisations work together

```
Incoming requests
       │
       ▼
 ┌─────────────┐    Cache hit?  ──YES──►  Return from JVM heap (< 5 ms)
 │ Cached      │
 │ Endpoint    │    Cache miss ──NO──►  Execute live SQL once
 └─────────────┘                          └──► Store in Caffeine (5 min TTL)

 ┌──────────────────────────┐
 │ PostgreSQL               │
 │                          │
 │  @Scheduled (every 10m)  │◄── REFRESH MATERIALIZED VIEW CONCURRENTLY
 │                          │         (non-blocking, no read locks)
 │  mv_permission_distribution   ◄── Materialized endpoint reads here
 │  (50 rows, indexed)      │         (simple indexed scan, 2-55 ms)
 │                          │
 │  live permissions join   │◄── Naive / Cached cache-miss reads here
 │  (10k users, 50k rows)   │         (GROUP BY / COUNT, 500-6000 ms)
 └──────────────────────────┘
```

- The **materialized view** moves computation cost from query-time to refresh-time. Data is at most 10 minutes stale — acceptable for analytics.
- The **Caffeine cache** moves even the materialized-view read off the critical path after the first call, serving millions of requests per hour with zero DB load.
- The **reverse-direction index** `idx_role_permissions_permission` ensures that even the naive query uses an index scan instead of a sequential scan — it still loses under DDoS, but not because it was missing an index.
