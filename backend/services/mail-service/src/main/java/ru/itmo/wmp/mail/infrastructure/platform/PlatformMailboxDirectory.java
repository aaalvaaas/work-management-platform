package ru.itmo.wmp.mail.infrastructure.platform;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import ru.itmo.wmp.mail.application.mailbox.MailboxConnection;
import ru.itmo.wmp.mail.application.mailbox.MailboxDirectory;
import ru.itmo.wmp.mail.config.properties.PlatformProperties;

import java.util.List;

@Slf4j
@Component
public class PlatformMailboxDirectory implements MailboxDirectory {
    private static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";
    private static final String MAILBOXES_PATH = "/internal/v1/mailboxes";
    private static final int ERROR_LIMIT = 500;

    private final RestClient restClient;

    public PlatformMailboxDirectory(
        PlatformProperties platformProperties,
        RestClient.Builder restClientBuilder
    ) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();

        requestFactory.setConnectTimeout(platformProperties.timeout());
        requestFactory.setReadTimeout(platformProperties.timeout());

        this.restClient = restClientBuilder
            .baseUrl(platformProperties.baseUrl())
            .requestFactory(requestFactory)
            .defaultHeader(INTERNAL_TOKEN_HEADER, platformProperties.internalKey())
            .build();
    }

    @Override
    public void reportCheck(Long mailboxId, boolean successful, String error) {
        try {
            restClient.post()
                .uri(MAILBOXES_PATH + "/{mailboxId}/checks", mailboxId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(new CheckPayload(successful, shorten(error)))
                .retrieve()
                .toBodilessEntity();
        } catch (RestClientException exception) {
            log.warn(
                "Unable to report the state of mailbox {}: {}",
                mailboxId,
                exception.getMessage()
            );
        }
    }

    @Override
    public List<MailboxConnection> activeMailboxes() {
        try {
            MailboxConnection[] mailboxes = restClient.get()
                .uri(MAILBOXES_PATH)
                .retrieve()
                .body(MailboxConnection[].class);

            return mailboxes == null ? List.of() : List.of(mailboxes);
        } catch (RestClientException exception) {
            log.warn("Unable to read mailboxes from platform service: {}", exception.getMessage());

            return List.of();
        }
    }

    private String shorten(String error) {
        if (error == null) {
            return null;
        }

        return error.length() <= ERROR_LIMIT ? error : error.substring(0, ERROR_LIMIT);
    }

    private record CheckPayload(boolean successful, String error) {
    }
}
