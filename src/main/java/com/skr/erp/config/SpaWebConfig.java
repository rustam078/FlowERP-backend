package com.skr.erp.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

// Serves the React single-page app: a real file is returned as-is, and any other
// (non-API) route falls back to index.html so client-side routing and page refresh work.
@Configuration
public class SpaWebConfig implements WebMvcConfigurer {

    // Build assets have content-hashed names, so a cached copy can never be stale.
    private static final CacheControl HASHED_ASSETS = CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic().immutable();

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/assets/**")
                .addResourceLocations("classpath:/static/assets/")
                .setCacheControl(HASHED_ASSETS);
        // index.html (and app routes) must be revalidated on every load, or the app window
        // keeps showing the previous build (e.g. a new menu item missing until refresh).
        registry.addResourceHandler("/**")
                .addResourceLocations("classpath:/static/")
                .setCacheControl(CacheControl.noCache())
                .resourceChain(true)
                .addResolver(new SpaResourceResolver());
    }

    // Returns the requested static file when it exists, else the SPA shell for app routes.
    private static class SpaResourceResolver extends PathResourceResolver {

        private final Resource index = new ClassPathResource("static/index.html");

        @Override
        protected Resource getResource(@NonNull String path, @NonNull Resource location) throws IOException {
            if (path.isEmpty()) {
                return index;
            }
            Resource requested = location.createRelative(path);
            if (requested.exists() && requested.isReadable()) {
                return requested;
            }
            return isAppRoute(path) ? index : null;
        }

        // Only extension-less, non-API paths map to the SPA shell; everything else 404s.
        private boolean isAppRoute(String path) {
            if (path.startsWith("api/") || path.startsWith("actuator/")
                    || path.startsWith("v3/") || path.startsWith("swagger-ui/")
                    || path.startsWith("webjars/")) {
                return false;
            }
            return !path.contains(".");
        }
    }
}
