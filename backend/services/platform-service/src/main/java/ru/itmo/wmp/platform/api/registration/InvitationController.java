package ru.itmo.wmp.platform.api.registration;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import ru.itmo.wmp.platform.api.registration.mapper.RegistrationMapper;
import ru.itmo.wmp.platform.application.registration.ParticipantInvitationService;
import ru.itmo.wmp.platform.config.security.JwtPrincipal;
import ru.itmo.wmp.platform.dto.registration.CreateInvitationRequest;
import ru.itmo.wmp.platform.dto.registration.InvitationResponse;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/invitations")
@RequiredArgsConstructor
public class InvitationController {
    private final ParticipantInvitationService participantInvitationService;
    private final RegistrationMapper registrationMapper;

    @PostMapping
    @PreAuthorize("@accessGuard.isOrganizationAdmin(#organizationId, authentication)")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "Invite a participant",
        description = "Sent by the organization manager or its administrator"
    )
    public InvitationResponse invite(
        @PathVariable Long organizationId,
        @Valid @RequestBody CreateInvitationRequest request,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return registrationMapper.toResponse(
            participantInvitationService.invite(
                organizationId,
                request.isuNumber(),
                JwtPrincipal.userId(jwt)
            )
        );
    }
}
