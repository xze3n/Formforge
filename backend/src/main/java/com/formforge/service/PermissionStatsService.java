package com.formforge.service;

import com.formforge.config.CacheConfig;
import com.formforge.dto.PermissionDistributionDto;
import com.formforge.repository.PermissionStatsRepository;
import com.formforge.repository.PermissionStatsRepository.PermissionDistributionProjection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Provides permission-distribution statistics via three implementations:
 *
 * <ol>
 *   <li><b>Naive</b>   – runs the full 2-hop M:N join every request (no cache).</li>
 *   <li><b>Cached</b>  – same query but wrapped in a 5-minute Caffeine cache.</li>
 *   <li><b>Materialized</b> – reads from the pre-aggregated PostgreSQL materialized
 *       view created by migration V8; the view is refreshed every 10 minutes.</li>
 * </ol>
 *
 * <p>The three methods are exposed by {@link com.formforge.controller.StatsController}
 * at distinct URL paths so that JMeter can benchmark them independently and
 * demonstrate the latency improvements at scale.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionStatsService {

    private final PermissionStatsRepository repository;
    private final JdbcTemplate              jdbc;

    // ─── 1. Naive (no cache) ─────────────────────────────────────────────

    /**
     * Executes the full 3-table join on every invocation.
     *
     * <p>With 10 000+ users this query takes tens of milliseconds. Under
     * concurrent load (DDoS scenario) every thread hits the DB simultaneously,
     * saturating the connection pool and causing latency to spike.
     */
    @Transactional(readOnly = true)
    public List<PermissionDistributionDto> naiveDistribution() {
        log.debug("[Stats] naive – executing live M:N join query");
        return mapProjections(repository.findPermissionDistribution());
    }

    // ─── 2. Cached (Caffeine, 5-minute TTL) ──────────────────────────────

    /**
     * Returns the same data as {@link #naiveDistribution()} but caches the
     * result in Caffeine for {@code 5} minutes.
     *
     * <p>The first call per TTL window hits the DB once; all subsequent calls
     * are served from the in-process cache in sub-millisecond time. Under a
     * DDoS with 500 concurrent threads the DB receives at most one query per
     * 5-minute window instead of 500 simultaneous queries.
     */
    @Cacheable(CacheConfig.PERMISSION_DISTRIBUTION_CACHE)
    @Transactional(readOnly = true)
    public List<PermissionDistributionDto> cachedDistribution() {
        log.debug("[Stats] cached – cache miss, executing live query");
        return mapProjections(repository.findPermissionDistribution());
    }

    // ─── 3. Materialized view ────────────────────────────────────────────

    /**
     * Reads from the pre-aggregated {@code mv_permission_distribution}
     * materialized view. No GROUP BY / COUNT computation occurs at query time;
     * PostgreSQL performs an indexed scan on the view's unique index.
     *
     * <p>Latency is nearly constant regardless of how many users or join rows
     * exist, because all aggregation work was done at the last refresh.
     */
    @Transactional(readOnly = true)
    public List<PermissionDistributionDto> materializedDistribution() {
        log.debug("[Stats] materialized – reading from mv_permission_distribution");
        return mapProjections(repository.findPermissionDistributionFromMaterializedView());
    }

    // ─── Scheduled MV refresh ────────────────────────────────────────────

    /**
     * Refreshes the PostgreSQL materialized view every 10 minutes so that
     * newly seeded or registered users are reflected without requiring a
     * full re-deploy.
     *
     * <p>Uses {@code CONCURRENTLY} so reads on the view are never blocked.
     * Also evicts the Caffeine cache so the next {@link #cachedDistribution()}
     * call picks up fresh data from the refreshed view.
     */
    @Scheduled(fixedDelayString = "PT10M")
    @CacheEvict(value = CacheConfig.PERMISSION_DISTRIBUTION_CACHE, allEntries = true)
    public void refreshMaterializedView() {
        try {
            log.info("[Stats] Refreshing mv_permission_distribution …");
            jdbc.execute("REFRESH MATERIALIZED VIEW CONCURRENTLY mv_permission_distribution");
            log.info("[Stats] mv_permission_distribution refreshed.");
        } catch (Exception e) {
            log.warn("[Stats] MV refresh failed (non-fatal): {}", e.getMessage());
        }
    }

    // ─── Mapping helper ──────────────────────────────────────────────────

    private static List<PermissionDistributionDto> mapProjections(
            List<PermissionDistributionProjection> rows) {
        return rows.stream()
                .map(r -> new PermissionDistributionDto(
                        r.getPermission_id(),
                        r.getPermission_name(),
                        r.getUser_count()  != null ? r.getUser_count()  : 0L,
                        r.getRole_count()  != null ? r.getRole_count()  : 0L,
                        r.getRole_names()))
                .toList();
    }
}
