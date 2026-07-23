package ru.itmo.wmp.platform.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.itmo.wmp.platform.config.properties.AppProperties;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class HealthController {
    private final AppProperties appProperties;

    public HealthController(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of(
            "status", "UP",
            "version", appProperties.version()
        );
    }
}
