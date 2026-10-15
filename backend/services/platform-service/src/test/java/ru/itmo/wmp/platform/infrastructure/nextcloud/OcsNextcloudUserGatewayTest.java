package ru.itmo.wmp.platform.infrastructure.nextcloud;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import ru.itmo.wmp.platform.application.nextcloud.NextcloudUser;
import ru.itmo.wmp.platform.config.properties.NextcloudProperties;
import ru.itmo.wmp.platform.exception.nextcloud.NextcloudException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

public class OcsNextcloudUserGatewayTest {
    private static final String BASE_URL = "http://nextcloud.test";
    private static final String USERS_URL = BASE_URL + "/ocs/v2.php/cloud/users";

    private MockRestServiceServer server;
    private OcsNextcloudUserGateway gateway;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();

        server = MockRestServiceServer.bindTo(builder).build();

        gateway = new OcsNextcloudUserGateway(
            new NextcloudProperties(true, BASE_URL, "admin", "admin-password"),
            builder
        );
    }

    @Test
    void shouldCreateUserWithIsuNumberAsUid() {
        server.expect(requestTo(USERS_URL))
            .andExpect(method(org.springframework.http.HttpMethod.POST))
            .andExpect(header("OCS-APIRequest", "true"))
            .andExpect(content().string(containsString("userid=465824")))
            .andExpect(content().string(containsString("email=465824%40niuitmo.ru")))
            .andRespond(withSuccess(ocs("ok", 100), MediaType.APPLICATION_JSON));

        gateway.create(user(), "generated-password");

        server.verify();
    }

    @Test
    void shouldUpdateProfileWhenUserAlreadyExists() {
        server.expect(requestTo(USERS_URL))
            .andRespond(withSuccess(ocs("failure", 102), MediaType.APPLICATION_JSON));

        server.expect(requestTo(USERS_URL + "/465824"))
            .andExpect(method(org.springframework.http.HttpMethod.PUT))
            .andExpect(content().string(containsString("key=displayname")))
            .andRespond(withSuccess(ocs("ok", 200), MediaType.APPLICATION_JSON));

        server.expect(requestTo(USERS_URL + "/465824"))
            .andExpect(content().string(containsString("key=email")))
            .andRespond(withSuccess(ocs("ok", 200), MediaType.APPLICATION_JSON));

        gateway.create(user(), "generated-password");

        server.verify();
    }

    @Test
    void shouldDisableUser() {
        server.expect(requestTo(USERS_URL + "/465824/disable"))
            .andExpect(method(org.springframework.http.HttpMethod.PUT))
            .andRespond(withSuccess(ocs("ok", 200), MediaType.APPLICATION_JSON));

        gateway.disable("465824");

        server.verify();
    }

    @Test
    void shouldEnableUser() {
        server.expect(requestTo(USERS_URL + "/465824/enable"))
            .andRespond(withSuccess(ocs("ok", 200), MediaType.APPLICATION_JSON));

        gateway.enable("465824");

        server.verify();
    }

    @Test
    void shouldTreatKnownUserAsExisting() {
        server.expect(requestTo(USERS_URL + "/465824"))
            .andExpect(method(org.springframework.http.HttpMethod.GET))
            .andRespond(withSuccess(ocs("ok", 100), MediaType.APPLICATION_JSON));

        assertTrue(gateway.exists("465824"));
    }

    @Test
    void shouldTreatUnknownUserAsMissing() {
        server.expect(requestTo(USERS_URL + "/465824"))
            .andRespond(withSuccess(ocs("failure", 998), MediaType.APPLICATION_JSON));

        assertFalse(gateway.exists("465824"));
    }

    @Test
    void shouldFailOnUnauthorizedResponse() {
        server.expect(requestTo(USERS_URL))
            .andRespond(withSuccess(ocs("failure", 997), MediaType.APPLICATION_JSON));

        NextcloudException exception = assertThrows(
            NextcloudException.class,
            () -> gateway.create(user(), "generated-password")
        );

        assertThat(exception.getMessage(), containsString("997"));
    }

    @Test
    void shouldFailOnUnexpectedResponseBody() {
        server.expect(requestTo(USERS_URL))
            .andRespond(withSuccess("{}", MediaType.APPLICATION_JSON));

        assertThrows(
            NextcloudException.class,
            () -> gateway.create(user(), "generated-password")
        );
    }

    private NextcloudUser user() {
        return new NextcloudUser("465824", "Иванов Иван", "465824@niuitmo.ru");
    }

    private String ocs(String status, int statuscode) {
        return """
            {"ocs":{"meta":{"status":"%s","statuscode":%d,"message":"test"},"data":{}}}
            """.formatted(status, statuscode);
    }
}
