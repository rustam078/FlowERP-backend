package com.skr.erp.common.constants;

// ADMIN has full access inside its portfolio; STAFF is restricted by ROLE_PERMISSIONS;
// SUPER_ADMIN belongs to no portfolio and can switch into any portfolio with full rights.
public final class Role {

    public static final String SUPER_ADMIN = "SUPER_ADMIN";
    public static final String ADMIN = "ADMIN";
    public static final String STAFF = "STAFF";

    private Role() {
    }

    // Roles a portfolio admin may assign to users of its portfolio.
    public static boolean isValid(String role) {
        return ADMIN.equals(role) || STAFF.equals(role);
    }

    // Full access inside the current portfolio (no per-module permission check).
    public static boolean hasFullAccess(String role) {
        return ADMIN.equals(role) || SUPER_ADMIN.equals(role);
    }
}
