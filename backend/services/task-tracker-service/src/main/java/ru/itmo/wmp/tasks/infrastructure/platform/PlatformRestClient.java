package ru.itmo.wmp.tasks.infrastructure.platform;

import org.springframework.http.HttpHeaders;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import ru.itmo.wmp.tasks.application.BlockSummary;
import ru.itmo.wmp.tasks.application.OrganizationMembership;
import ru.itmo.wmp.tasks.application.OrganizationSummary;
import ru.itmo.wmp.tasks.application.ParticipantSummary;
import ru.itmo.wmp.tasks.application.PlatformGateway;
import ru.itmo.wmp.tasks.config.properties.PlatformProperties;
import ru.itmo.wmp.tasks.config.security.JwtPrincipal;
import ru.itmo.wmp.tasks.exception.PlatformUnavailableException;
import ru.itmo.wmp.tasks.exception.TaskAccessDeniedException;
import ru.itmo.wmp.tasks.exception.TaskValidationException;

@Component
public class PlatformRestClient implements PlatformGateway {
    private static final String BEARER = "Bearer ";
    private static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";

    private final RestClient restClient;
    private final String internalKey;

    public PlatformRestClient(
        PlatformProperties platformProperties,
        RestClient.Builder restClientBuilder
    ) {
        this.internalKey = platformProperties.internalKey();

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();

        requestFactory.setConnectTimeout(platformProperties.timeout());
        requestFactory.setReadTimeout(platformProperties.timeout());

        this.restClient = restClientBuilder
            .baseUrl(platformProperties.baseUrl())
            .requestFactory(requestFactory)
            .build();
    }

    @Override
    public OrganizationMembership membershipOf(Long organizationId) {
        return get(
            "/api/v1/organizations/{organizationId}/membership",
            OrganizationMembership.class,
            organizationId
        );
    }

    @Override
    public OrganizationSummary organizationOf(Long organizationId) {
        OrganizationPayload organization = JwtPrincipal.currentToken().isPresent()
            ? get("/api/v1/organizations/{organizationId}", OrganizationPayload.class, organizationId)
            : getInternal(
                "/internal/v1/organizations/{organizationId}",
                OrganizationPayload.class,
                organizationId
            );

        return new OrganizationSummary(
            organization.id(),
            organization.code(),
            organization.name()
        );
    }

    @Override
    public BlockSummary blockOf(Long organizationId, Long blockId) {
        BlockPayload block = get(
            "/api/v1/organizations/{organizationId}/blocks/{blockId}",
            BlockPayload.class,
            organizationId,
            blockId
        );

        return new BlockSummary(block.id(), block.organizationId(), block.name());
    }

    @Override
    public ParticipantSummary requireActiveParticipant(Long organizationId, String isuNumber) {
        ParticipantPayload participant = get(
            "/api/v1/organizations/{organizationId}/participants/{isuNumber}",
            ParticipantPayload.class,
            organizationId,
            isuNumber
        );

        if (!"ACTIVE".equals(participant.status())) {
            throw new TaskValidationException(
                "Participant " + isuNumber + " is not active in organization " + organizationId
            );
        }

        return new ParticipantSummary(
            participant.isuNumber(),
            participant.userId(),
            participant.lastName() + " " + participant.firstName()
        );
    }

    private <T> T get(String path, Class<T> type, Object... variables) {
        try {
            return restClient.get()
                .uri(path, variables)
                .header(HttpHeaders.AUTHORIZATION, BEARER + token())
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                    throw clientError(response.getStatusCode(), path);
                })
                .body(type);
        } catch (TaskAccessDeniedException | TaskValidationException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new PlatformUnavailableException(path, exception);
        }
    }

    private <T> T getInternal(String path, Class<T> type, Object... variables) {
        try {
            return restClient.get()
                .uri(path, variables)
                .header(INTERNAL_TOKEN_HEADER, internalKey)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (request, response) -> {
                    throw clientError(response.getStatusCode(), path);
                })
                .body(type);
        } catch (TaskAccessDeniedException | TaskValidationException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new PlatformUnavailableException(path, exception);
        }
    }

    private RuntimeException clientError(HttpStatusCode status, String path) {
        if (status.value() == 403 || status.value() == 401) {
            return new TaskAccessDeniedException("Platform service refused " + path);
        }

        return new TaskValidationException("Platform service answered " + status + " for " + path);
    }

    private String token() {
        return JwtPrincipal.currentToken()
            .orElseThrow(() -> new TaskAccessDeniedException("Request carries no access token"));
    }

    private record BlockPayload(Long id, Long organizationId, String name, String status) {
    }

    private record OrganizationPayload(Long id, String code, String name, String status) {
    }

    private record ParticipantPayload(
        Long id,
        String isuNumber,
        String lastName,
        String firstName,
        String status,
        Long userId
    ) {
    }
}
