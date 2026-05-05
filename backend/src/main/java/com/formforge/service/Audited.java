package com.formforge.service;

import com.formforge.model.AuditAction;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a service method for automatic audit logging via {@link AuditAspect}.
 * <p>
 * If the method's first argument is a {@code Long}, it is recorded as the
 * resource ID automatically.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Audited {
    AuditAction action();
    String resourceType() default "";
}
