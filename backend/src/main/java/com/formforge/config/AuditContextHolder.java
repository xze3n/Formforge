package com.formforge.config;

import lombok.Data;

/**
 * Thread-local carrier for the current request's audit context.
 * Populated by {@link AuditFilter} from the X-User-* headers sent by the frontend.
 */
public final class AuditContextHolder {

    private AuditContextHolder() {}

    @Data
    public static class AuditContext {
        private Long userId;
        private String username;
        private String userRole;
        private String ipAddress;
    }

    private static final ThreadLocal<AuditContext> HOLDER = new ThreadLocal<>();

    public static void set(AuditContext ctx)  { HOLDER.set(ctx); }
    public static AuditContext get()           { return HOLDER.get(); }
    public static void clear()                 { HOLDER.remove(); }
}
