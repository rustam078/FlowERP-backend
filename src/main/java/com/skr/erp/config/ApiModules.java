package com.skr.erp.config;

/** Maps an /api path to its app module (used by permissions and the audit log). */
public final class ApiModules {

    private ApiModules() {
    }

    // First path segment after /api/, e.g. "sales" for /api/sales/123.
    public static String firstSegment(String path) {
        String p = path.startsWith("/api/") ? path.substring(5) : path;
        int slash = p.indexOf('/');
        return slash >= 0 ? p.substring(0, slash) : p;
    }

    // Permission module of a path; null for paths outside the module permission matrix.
    public static String moduleForPath(String path) {
        return switch (firstSegment(path)) {
            case "sales", "customers", "qr" -> "sales";
            case "inventory" -> "inventory";
            case "products", "product-material-cost" -> "products";
            case "investments", "vendors" -> "investment";
            case "production" -> "production";
            case "employees" -> "employees";
            case "dashboard" -> "dashboard";
            case "revenue" -> "revenue";
            case "settings", "users" -> "settings";
            default -> null;
        };
    }
}
