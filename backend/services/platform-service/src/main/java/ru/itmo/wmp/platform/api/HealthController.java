package ru.itmo.wmp.platform.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController {
    @Value("${spring.application.name}")
    private String applicationName;

    @Value("${app.version}")
    private String version;

    @GetMapping("/api/v1/health")
    public Map<String, String> health() {
        return Map.of(
            "status", "UP",
            "service", applicationName,
            "version", version
        );
    }
}
