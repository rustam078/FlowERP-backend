package com.skr.erp.audit;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.Map;

/**
 * Default log level pattern that prints the request id on every log line: "INFO [3f2a91c4] ...".
 * Added with the lowest priority, so a logging.pattern.level set in application properties still wins.
 */
public class RequestIdLogPattern implements EnvironmentPostProcessor {

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        environment.getPropertySources().addLast(new MapPropertySource("requestIdLogPattern",
                Map.of("logging.pattern.level", "%5p [%X{" + AuditRequestFilter.REQUEST_ID_KEY + ":--------}]")));
    }
}
