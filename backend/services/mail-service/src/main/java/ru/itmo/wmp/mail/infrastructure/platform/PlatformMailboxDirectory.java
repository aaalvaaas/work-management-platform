package ru.itmo.wmp.mail.infrastructure.platform;

import lombok.extern.slf4j.Slf4j;
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
}
