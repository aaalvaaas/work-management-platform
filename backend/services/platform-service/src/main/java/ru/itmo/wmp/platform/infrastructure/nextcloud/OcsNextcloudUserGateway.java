package ru.itmo.wmp.platform.infrastructure.nextcloud;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import ru.itmo.wmp.platform.application.nextcloud.NextcloudUser;
import ru.itmo.wmp.platform.application.nextcloud.NextcloudUserGateway;
import ru.itmo.wmp.platform.config.properties.NextcloudProperties;
import ru.itmo.wmp.platform.exception.nextcloud.NextcloudException;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "nextcloud", name = "enabled", havingValue = "true")
public class OcsNextcloudUserGateway implements NextcloudUserGateway {
    private static final String USERS_PATH = "/ocs/v2.php/cloud/users";
    private static final String OCS_API_REQUEST_HEADER = "OCS-APIRequest";
    private static final String DISPLAY_NAME_KEY = "displayname";
    private static final String EMAIL_KEY = "email";

    private final RestClient restClient;

    public OcsNextcloudUserGateway(
        NextcloudProperties nextcloudProperties,
        RestClient.Builder restClientBuilder
    ) {
        this.restClient = restClientBuilder
            .baseUrl(nextcloudProperties.baseUrl())
            .defaultHeader(OCS_API_REQUEST_HEADER, "true")
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeaders(headers -> headers.setBasicAuth(
                nextcloudProperties.adminUser(),
                nextcloudProperties.adminPassword()
            ))
            .build();
    }

    @Override
    public boolean exists(String uid) {
        OcsResponse.Meta meta = meta(get(USERS_PATH + "/" + uid), "lookup user " + uid);

        if (OcsStatus.isSuccess(meta.statuscode())) {
            return true;
        }

        if (OcsStatus.isMissingUser(meta.statuscode())) {
            return false;
        }

        throw failure("lookup user " + uid, meta);
    }

    @Override
    public void create(NextcloudUser user, String password) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();

        form.add("userid", user.uid());
        form.add("password", password);
        form.add("displayName", user.displayName());
        form.add("email", user.email());

        OcsResponse.Meta meta = meta(
            post(USERS_PATH, form),
            "create user " + user.uid()
        );

        if (meta.statuscode() == OcsStatus.USER_ALREADY_EXISTS) {
            log.info("Nextcloud user {} already exists, updating profile", user.uid());

            update(user);

            return;
        }

        if (!OcsStatus.isSuccess(meta.statuscode())) {
            throw failure("create user " + user.uid(), meta);
        }
    }

    @Override
    public void update(NextcloudUser user) {
        edit(user.uid(), DISPLAY_NAME_KEY, user.displayName());
        edit(user.uid(), EMAIL_KEY, user.email());
    }

    @Override
    public void enable(String uid) {
        changeState(uid, "enable");
    }

    @Override
    public void disable(String uid) {
        changeState(uid, "disable");
    }

    private void edit(String uid, String key, String value) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();

        form.add("key", key);
        form.add("value", value);

        OcsResponse.Meta meta = meta(
            put(USERS_PATH + "/" + uid, form),
            "update " + key + " of user " + uid
        );

        if (!OcsStatus.isSuccess(meta.statuscode())) {
            throw failure("update " + key + " of user " + uid, meta);
        }
    }

    private void changeState(String uid, String action) {
        OcsResponse.Meta meta = meta(
            put(USERS_PATH + "/" + uid + "/" + action, new LinkedMultiValueMap<>()),
            action + " user " + uid
        );

        if (!OcsStatus.isSuccess(meta.statuscode())) {
            throw failure(action + " user " + uid, meta);
        }
    }

    private ResponseEntity<OcsResponse> get(String path) {
        try {
            return restClient.get()
                .uri(path)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> {
                })
                .toEntity(OcsResponse.class);
        } catch (RestClientException exception) {
            throw new NextcloudException("Nextcloud is not reachable on " + path, exception);
        }
    }

    private ResponseEntity<OcsResponse> post(String path, MultiValueMap<String, String> form) {
        try {
            return restClient.post()
                .uri(path)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> {
                })
                .toEntity(OcsResponse.class);
        } catch (RestClientException exception) {
            throw new NextcloudException("Nextcloud is not reachable on " + path, exception);
        }
    }

    private ResponseEntity<OcsResponse> put(String path, MultiValueMap<String, String> form) {
        try {
            return restClient.put()
                .uri(path)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (request, response) -> {
                })
                .toEntity(OcsResponse.class);
        } catch (RestClientException exception) {
            throw new NextcloudException("Nextcloud is not reachable on " + path, exception);
        }
    }

    private OcsResponse.Meta meta(ResponseEntity<OcsResponse> response, String operation) {
        OcsResponse body = response.getBody();

        if (body == null || body.ocs() == null || body.ocs().meta() == null) {
            throw new NextcloudException(
                "Nextcloud returned an unexpected response on " + operation
                    + ", http status " + response.getStatusCode()
            );
        }

        return body.ocs().meta();
    }

    private NextcloudException failure(String operation, OcsResponse.Meta meta) {
        return new NextcloudException(
            "Nextcloud failed to " + operation
                + ": statuscode " + meta.statuscode()
                + ", message " + meta.message()
        );
    }
}
