package com.yourapp.health;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Ultra-lightweight health and ping controller for Render and keep-alive pingers (e.g. cron-job.org).
 * Requires zero authentication, database access, or WhatsApp processing.
 */
@RestController
public class HealthCheckController {

    @GetMapping(value = {"/health", "/"}, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }
}