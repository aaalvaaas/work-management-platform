package ru.itmo.wmp.platform.api.registration;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.itmo.wmp.platform.api.registration.mapper.RegistrationRequestMapper;
import ru.itmo.wmp.platform.application.registration.RegistrationRequestService;
import ru.itmo.wmp.platform.config.security.JwtPrincipal;
import ru.itmo.wmp.platform.domain.registration.RegistrationRequestStatus;
import ru.itmo.wmp.platform.dto.registration.RegistrationRequestResponse;
import ru.itmo.wmp.platform.dto.registration.RejectRegistrationRequest;

import java.util.List;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/registration-requests")
@PreAuthorize("@accessGuard.isOrganizationAdmin(#organizationId, authentication)")
@RequiredArgsConstructor
public class RegistrationRequestController {
    private final RegistrationRequestService registrationRequestService;
    private final RegistrationRequestMapper registrationRequestMapper;

    @GetMapping
    @Operation(
        summary = "List registration requests",
        description = "Returns requests of the organization, optionally filtered by status"
    )
    public List<RegistrationRequestResponse> list(
        @PathVariable Long organizationId,
        @RequestParam(required = false) RegistrationRequestStatus status
    ) {
        return registrationRequestMapper.toResponses(
            registrationRequestService.findForOrganization(organizationId, status)
        );
    }

    @PostMapping("/{requestId}/approve")
    @Operation(
        summary = "Approve a registration request",
        description = "Creates the account and adds the person to the participant list"
    )
    public RegistrationRequestResponse approve(
        @PathVariable Long organizationId,
        @PathVariable Long requestId,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return registrationRequestMapper.toResponse(
            registrationRequestService.approve(organizationId, requestId, JwtPrincipal.userId(jwt))
        );
    }

    @PostMapping("/{requestId}/reject")
    @Operation(
        summary = "Reject a registration request"
    )
    public RegistrationRequestResponse reject(
        @PathVariable Long organizationId,
        @PathVariable Long requestId,
        @Valid @RequestBody(required = false) RejectRegistrationRequest request,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return registrationRequestMapper.toResponse(
            registrationRequestService.reject(
                organizationId,
                requestId,
                request == null ? null : request.comment(),
                JwtPrincipal.userId(jwt)
            )
        );
    }
}
