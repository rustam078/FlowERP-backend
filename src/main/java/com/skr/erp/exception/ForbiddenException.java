package com.skr.erp.exception;

// Thrown when the logged-in user's role is not permitted the requested module+action (HTTP 403).
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
