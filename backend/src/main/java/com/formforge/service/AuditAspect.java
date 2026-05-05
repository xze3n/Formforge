package com.formforge.service;

import com.formforge.config.AuditContextHolder;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * AOP aspect that intercepts all {@link Audited}-annotated service methods and
 * records an {@link com.formforge.model.AuditLog} entry via {@link AuditLogService}.
 *
 * <p>The aspect fires <em>after</em> the method completes (success or failure) so
 * the outcome is always captured. It runs in its own {@code REQUIRES_NEW}
 * transaction (delegated to {@link AuditLogService#log}).
 */
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditLogService auditLogService;

    @Around("@annotation(audited)")
    public Object audit(ProceedingJoinPoint pjp, Audited audited) throws Throwable {
        boolean success = true;
        String details = null;

        // Auto-detect resource ID from first Long argument
        String resourceId = null;
        Object[] args = pjp.getArgs();
        if (args.length > 0 && args[0] instanceof Long l) {
            resourceId = l.toString();
        }

        try {
            return pjp.proceed();
        } catch (Exception ex) {
            success = false;
            details = ex.getClass().getSimpleName() + ": " + ex.getMessage();
            throw ex;
        } finally {
            AuditContextHolder.AuditContext ctx = AuditContextHolder.get();
            auditLogService.log(
                    ctx != null ? ctx.getUserId()   : null,
                    ctx != null ? ctx.getUsername()  : null,
                    ctx != null ? ctx.getUserRole()  : null,
                    audited.action(),
                    audited.resourceType().isBlank() ? null : audited.resourceType(),
                    resourceId,
                    details,
                    ctx != null ? ctx.getIpAddress() : null,
                    success
            );
        }
    }
}
