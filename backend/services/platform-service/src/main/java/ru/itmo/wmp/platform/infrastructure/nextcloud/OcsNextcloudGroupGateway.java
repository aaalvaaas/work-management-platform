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
import ru.itmo.wmp.platform.application.nextcloud.NextcloudGroupGateway;
import ru.itmo.wmp.platform.config.properties.NextcloudProperties;
import ru.itmo.wmp.platform.exception.nextcloud.NextcloudException;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "nextcloud", name = "enabled", havingValue = "true")
public class OcsNextcloudGroupGateway implements NextcloudGroupGateway {
    private static final String GROUPS_PATH = "/ocs/v2.php/cloud/groups";
    private static final String USERS_PATH = "/ocs/v2.php/cloud/users";
    private static final String OCS_API_REQUEST_HEADER = "OCS-APIRequest";
    private static final int GROUP_ALREADY_EXISTS = 102;

    private final RestClient restClient;

    public OcsNextcloudGroupGateway(
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
    public void ensureGroup(String groupId) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();

        form.add("groupid", groupId);

        OcsResponse.Meta meta = meta(post(GROUPS_PATH, form), "create group " + groupId);

        if (OcsStatus.isSuccess(meta.statuscode()) || meta.statuscode() == GROUP_ALREADY_EXISTS) {
            return;
        }

        throw failure("create group " + groupId, meta);
    }

    @Override
    public void addUserToGroup(String uid, String groupId) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();

        form.add("groupid", groupId);

        OcsResponse.Meta meta = meta(
            post(USERS_PATH + "/" + uid + "/groups", form),
            "add " + uid + " to group " + groupId
        );

        if (!OcsStatus.isSuccess(meta.statuscode())) {
            throw failure("add " + uid + " to group " + groupId, meta);
        }
    }

    @Override
    public void removeUserFromGroup(String uid, String groupId) {
        OcsResponse.Meta meta = meta(
            delete(USERS_PATH + "/" + uid + "/groups?groupid=" + groupId),
            "remove " + uid + " from group " + groupId
        );

        if (!OcsStatus.isSuccess(meta.statuscode())) {
            throw failure("remove " + uid + " from group " + groupId, meta);
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

    private ResponseEntity<OcsResponse> delete(String path) {
        try {
            return restClient.delete()
                .uri(path)
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
            "Nextcloud refused to " + operation + ": " + meta.statuscode() + " " + meta.message()
        );
    }
}
