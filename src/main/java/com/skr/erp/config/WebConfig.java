package com.skr.erp.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// Registers the auth/RBAC interceptor for all /api endpoints (public ones opt out inside the interceptor).
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final PermissionInterceptor permissionInterceptor;

    // First in line: the request's portfolio must be known before open-session-in-view opens the
    // Hibernate session, because the session takes its portfolio filter at that moment.
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(permissionInterceptor)
                .addPathPatterns("/api/**")
                .order(Ordered.HIGHEST_PRECEDENCE);
    }
}
