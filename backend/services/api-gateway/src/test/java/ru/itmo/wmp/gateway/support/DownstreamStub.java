package ru.itmo.wmp.gateway.support;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;

public final class DownstreamStub {
    private static final String RESPONSE_BODY = "{\"stub\":true}";

    private final HttpServer server;
    private final List<String> receivedAuthorizationHeaders = new CopyOnWriteArrayList<>();

    private DownstreamStub(HttpServer server) {
        this.server = server;
    }

    public static DownstreamStub start(RSAKey signingKey) {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);

            DownstreamStub stub = new DownstreamStub(server);

            server.createContext("/.well-known/jwks.json", exchange ->
                stub.respond(exchange, new JWKSet(signingKey).toPublicJWKSet().toString()));

            server.createContext("/", exchange -> {
                stub.receivedAuthorizationHeaders.add(
                    String.valueOf(exchange.getRequestHeaders().getFirst("Authorization"))
                );

                stub.respond(exchange, RESPONSE_BODY);
            });

            server.setExecutor(Executors.newCachedThreadPool());
            server.start();

            return stub;
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    public String baseUrl() {
        return "http://localhost:" + server.getAddress().getPort();
    }

    public String jwkSetUri() {
        return baseUrl() + "/.well-known/jwks.json";
    }

    public List<String> receivedAuthorizationHeaders() {
        return receivedAuthorizationHeaders;
    }

    private void respond(HttpExchange exchange, String body) throws IOException {
        byte[] content = body.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, content.length);

        try (var stream = exchange.getResponseBody()) {
            stream.write(content);
        }
    }
}
