package com.formforge.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * In-process caching configuration using Caffeine.
 *
 * <h3>Caches defined</h3>
 * <table border="1">
 *   <tr><th>Name</th><th>TTL</th><th>Max entries</th><th>Purpose</th></tr>
 *   <tr>
 *     <td>{@code permissionDistribution}</td>
 *     <td>5 min</td><td>1 000</td>
 *     <td>Caches the result of the M:N permission-distribution aggregate query.
 *         The first request per 5-minute window pays the DB cost; all subsequent
 *         requests are served from memory in &lt;1 ms regardless of load.</td>
 *   </tr>
 * </table>
 *
 * <p>TTL is write-based (expires 5 minutes after the entry was last written),
 * which means the cache self-invalidates without any explicit eviction call.
 * A {@code @Scheduled} task in {@link com.formforge.service.PermissionStatsService}
 * proactively refreshes the materialized view at the same cadence so that the
 * DB-level aggregate is always consistent with the latest user data.
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /** Name used in {@code @Cacheable} / {@code @CacheEvict} annotations. */
    public static final String PERMISSION_DISTRIBUTION_CACHE = "permissionDistribution";

    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.setCacheNames(List.of(PERMISSION_DISTRIBUTION_CACHE));
        manager.setCaffeine(
                Caffeine.newBuilder()
                        .expireAfterWrite(5, TimeUnit.MINUTES)
                        .maximumSize(1_000)
                        .recordStats()          // enables cache hit/miss metrics in logs
        );
        return manager;
    }
}
