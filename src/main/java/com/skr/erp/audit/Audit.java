package com.skr.erp.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a controller endpoint whose calls are written to the audit trail (request, response,
 * and the before/after of every row it changed). Only marked endpoints are audited.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Audit {

    // Readable action name shown in the audit log, e.g. "Sale created".
    String value();
}
