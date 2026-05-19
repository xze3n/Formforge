package com.formforge.controller;

import com.formforge.dto.PermissionDistributionDto;
import com.formforge.service.PermissionStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST endpoint exposing the permission-distribution statistic at three URL paths
 * that demonstrate the performance difference between implementation strategies.
 *
 * <p>All three endpoints are secured by the global JWT filter — a valid
 * {@code Authorization: Bearer <token>} header is required for every request.
 *
 * <table border="1">
 *   <tr><th>Path</th><th>Strategy</th><th>Behaviour</th></tr>
 *   <tr>
 *     <td>{@code GET /api/stats/permission-distribution/naive}</td>
 *     <td>Live JOIN</td>
 *     <td>Executes a 3-table M:N join on every request. Latency grows with
 *         dataset size; saturates the DB connection pool under DDoS.</td>
 *   </tr>
 *   <tr>
 *     <td>{@code GET /api/stats/permission-distribution/cached}</td>
 *     <td>Caffeine cache</td>
 *     <td>Same query but result is cached for 5 minutes. After the first
 *         request all concurrent threads return from memory in &lt;1 ms.</td>
 *   </tr>
 *   <tr>
 *     <td>{@code GET /api/stats/permission-distribution/materialized}</td>
 *     <td>Materialized view</td>
 *     <td>Reads from the pre-aggregated PostgreSQL view; no computation at
 *         query time. Combined with Caffeine this is the fastest option.</td>
 *   </tr>
 * </table>
 *
 * <p>These three paths are the JMeter benchmark targets.
 */
@RestController
@RequestMapping("/api/stats/permission-distribution")
@RequiredArgsConstructor
public class StatsController {

    private final PermissionStatsService statsService;

    /**
     * Naive baseline — full live M:N join, no caching.
     *
     * <p>Use this endpoint in JMeter's DDoS thread group to observe latency
     * degradation and connection-pool exhaustion under sustained concurrent load.
     */
    @GetMapping("/naive")
    public ResponseEntity<List<PermissionDistributionDto>> naive() {
        return ResponseEntity.ok(statsService.naiveDistribution());
    }

    /**
     * Caffeine-cached response — 5-minute TTL, served from in-process memory.
     *
     * <p>Under a 500-thread DDoS the DB receives at most one query per cache
     * window; all other requests return instantly from the heap.
     */
    @GetMapping("/cached")
    public ResponseEntity<List<PermissionDistributionDto>> cached() {
        return ResponseEntity.ok(statsService.cachedDistribution());
    }

    /**
     * Reads from the pre-aggregated {@code mv_permission_distribution}
     * materialized view (migration V8). Combined with Caffeine this endpoint
     * achieves the lowest possible latency — the DB does an indexed scan on a
     * pre-computed aggregate table with no GROUP BY / COUNT at query time.
     */
    @GetMapping("/materialized")
    public ResponseEntity<List<PermissionDistributionDto>> materialized() {
        return ResponseEntity.ok(statsService.materializedDistribution());
    }
}
