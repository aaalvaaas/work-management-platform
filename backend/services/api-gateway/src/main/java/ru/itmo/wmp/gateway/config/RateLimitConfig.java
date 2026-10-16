package ru.itmo.wmp.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;

@Configuration
public class RateLimitConfig {
    private static final String ANONYMOUS_PREFIX = "ip:";
    private static final String USER_PREFIX = "user:";
    private static final String UNKNOWN_CLIENT = "ip:unknown";

    @Bean
    public KeyResolver clientKeyResolver() {
        return exchange -> ReactiveSecurityContextHolder.getContext()
            .map(SecurityContext::getAuthentication)
            .filter(Authentication::isAuthenticated)
            .map(authentication -> USER_PREFIX + authentication.getName())
            .switchIfEmpty(Mono.fromSupplier(() -> clientAddress(exchange)));
    }

    private String clientAddress(ServerWebExchange exchange) {
        InetSocketAddress address = exchange.getRequest().getRemoteAddress();

        if (address == null || address.getAddress() == null) {
            return UNKNOWN_CLIENT;
        }

        return ANONYMOUS_PREFIX + address.getAddress().getHostAddress();
    }
}
