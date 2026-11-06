package com.skr.erp.common.constants;

// Two fixed roles. ADMIN always has full access; STAFF is restricted by ROLE_PERMISSIONS.
public final class Role {

    public static final String ADMIN = "ADMIN";
    public static final String STAFF = "STAFF";

    private Role() {
    }

    public static boolean isValid(String role) {
        return ADMIN.equals(role) || STAFF.equals(role);
    }
}
