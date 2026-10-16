package ru.itmo.wmp.platform.api.registration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import ru.itmo.wmp.platform.IntegrationTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import ru.itmo.wmp.platform.application.notification.RegistrationNotification;
import ru.itmo.wmp.platform.application.notification.RegistrationNotifier;
import ru.itmo.wmp.platform.domain.organization.Organization;
import ru.itmo.wmp.platform.domain.organization.OrganizationParticipant;
import ru.itmo.wmp.platform.domain.organization.ParticipantStatus;
import ru.itmo.wmp.platform.dto.auth.LoginRequest;
import ru.itmo.wmp.platform.dto.registration.CompleteRegistrationRequest;
import ru.itmo.wmp.platform.dto.registration.StartRegistrationRequest;
import ru.itmo.wmp.platform.infrastructure.persistance.organization.JpaOrganizationParticipantRepository;
import ru.itmo.wmp.platform.infrastructure.persistance.organization.JpaOrganizationRepository;
import ru.itmo.wmp.platform.infrastructure.persistance.user.JpaUserRepository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@IntegrationTest
public class RegistrationControllerIntegrationTest {
    private static final String ISU_NUMBER = "465824";
    private static final String PASSWORD = "Str0ngPassword!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JpaUserRepository userRepository;

    @Autowired
    private JpaOrganizationRepository organizationRepository;

    @Autowired
    private JpaOrganizationParticipantRepository participantRepository;

    @MockitoBean
    private RegistrationNotifier registrationNotifier;

    @Test
    void shouldRejectIsuNumberMissingInParticipantLists() throws Exception {
        mockMvc.perform(startRequest(ISU_NUMBER))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error").value("REGISTRATION_NOT_ALLOWED"));
    }

    @Test
    void shouldStartRegistrationAndHideFullEmail() throws Exception {
        participant("Мегафакультет", ISU_NUMBER);

        mockMvc.perform(startRequest(ISU_NUMBER))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.email").value("i***@niuitmo.ru"))
            .andExpect(jsonPath("$.expiresAt").exists());
    }

    @Test
    void shouldReturnDataPreparedByOrganization() throws Exception {
        participant("Мегафакультет", ISU_NUMBER);

        mockMvc.perform(get("/api/v1/registration/{token}", startAndCaptureToken(ISU_NUMBER)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.isuNumber").value(ISU_NUMBER))
            .andExpect(jsonPath("$.lastName").value("Иванов"))
            .andExpect(jsonPath("$.firstName").value("Иван"))
            .andExpect(jsonPath("$.email").value("i***@niuitmo.ru"))
            .andExpect(jsonPath("$.organizations[0]").value("Мегафакультет"));
    }

    @Test
    void shouldRejectUnknownToken() throws Exception {
        mockMvc.perform(get("/api/v1/registration/{token}", "not-a-real-token"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("INVALID_INVITATION"));
    }

    @Test
    void shouldCreateAccountLinkedToEveryOrganizationOfTheSamePerson() throws Exception {
        OrganizationParticipant first = participant("Мегафакультет", ISU_NUMBER);
        OrganizationParticipant second = participant("Студсовет", ISU_NUMBER);

        mockMvc.perform(completeRequest(startAndCaptureToken(ISU_NUMBER), PASSWORD))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.isuNumber").value(ISU_NUMBER))
            .andExpect(jsonPath("$.lastName").value("Иванов"))
            .andExpect(jsonPath("$.status").value("ACTIVE"))
            .andExpect(jsonPath("$.globalRoles").isEmpty());

        Long userId = userRepository.findByIsuNumber(ISU_NUMBER).orElseThrow().getId();

        assertEquals(
            userId,
            participantRepository.findById(first.getId()).orElseThrow().getUser().getId()
        );

        assertEquals(
            userId,
            participantRepository.findById(second.getId()).orElseThrow().getUser().getId()
        );
    }

    @Test
    void shouldAllowLoginAfterRegistration() throws Exception {
        participant("Мегафакультет", ISU_NUMBER);

        mockMvc.perform(completeRequest(startAndCaptureToken(ISU_NUMBER), PASSWORD))
            .andExpect(status().isCreated());

        mockMvc.perform(
            post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new LoginRequest(ISU_NUMBER, PASSWORD)))
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").exists());
    }

    @Test
    void shouldRejectSecondUseOfTheSameToken() throws Exception {
        participant("Мегафакультет", ISU_NUMBER);

        String token = startAndCaptureToken(ISU_NUMBER);

        mockMvc.perform(completeRequest(token, PASSWORD))
            .andExpect(status().isCreated());

        mockMvc.perform(completeRequest(token, PASSWORD))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("INVALID_INVITATION"));
    }

    @Test
    void shouldRejectRegistrationWhenAccountAlreadyExists() throws Exception {
        participant("Мегафакультет", ISU_NUMBER);

        mockMvc.perform(completeRequest(startAndCaptureToken(ISU_NUMBER), PASSWORD))
            .andExpect(status().isCreated());

        mockMvc.perform(startRequest(ISU_NUMBER))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value("ALREADY_REGISTERED"));
    }

    @Test
    void shouldRejectRegistrationForExcludedParticipant() throws Exception {
        OrganizationParticipant participant = participant("Мегафакультет", ISU_NUMBER);

        participant.setStatus(ParticipantStatus.INACTIVE);
        participantRepository.save(participant);

        mockMvc.perform(startRequest(ISU_NUMBER))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.error").value("REGISTRATION_NOT_ALLOWED"));
    }

    @Test
    void shouldRejectTooShortPassword() throws Exception {
        participant("Мегафакультет", ISU_NUMBER);

        mockMvc.perform(completeRequest(startAndCaptureToken(ISU_NUMBER), "short"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    private String startAndCaptureToken(String isuNumber) throws Exception {
        mockMvc.perform(startRequest(isuNumber))
            .andExpect(status().isAccepted());

        ArgumentCaptor<RegistrationNotification> captor =
            ArgumentCaptor.forClass(RegistrationNotification.class);

        verify(registrationNotifier).sendEmailConfirmation(captor.capture());

        return captor.getValue().token();
    }

    private MockHttpServletRequestBuilder startRequest(String isuNumber) throws Exception {
        return post("/api/v1/registration/start")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(new StartRegistrationRequest(isuNumber)));
    }

    private MockHttpServletRequestBuilder completeRequest(
        String token,
        String password
    ) throws Exception {
        return post("/api/v1/registration/complete")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(
                new CompleteRegistrationRequest(token, password)
            ));
    }

    private OrganizationParticipant participant(String organizationName, String isuNumber) {
        Organization organization = new Organization();

        organization.setCode(codeOf(organizationName));
        organization.setName(organizationName);

        Organization saved = organizationRepository.save(organization);

        OrganizationParticipant participant = new OrganizationParticipant();

        participant.setOrganization(saved);
        participant.setIsuNumber(isuNumber);
        participant.setLastName("Иванов");
        participant.setFirstName("Иван");
        participant.setEmail("ivanov@niuitmo.ru");
        participant.setPosition("Специалист");
        participant.setStatus(ParticipantStatus.ACTIVE);

        return participantRepository.save(participant);
    }

    private String codeOf(String name) {
        return "org-" + Math.abs(name.hashCode()) % 100000;
    }

}
