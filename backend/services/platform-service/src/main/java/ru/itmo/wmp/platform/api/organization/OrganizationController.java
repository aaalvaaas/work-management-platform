package ru.itmo.wmp.platform.api.organization;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import ru.itmo.wmp.platform.api.organization.mapper.OrganizationMapper;
import ru.itmo.wmp.platform.application.organization.OrganizationService;
import ru.itmo.wmp.platform.config.security.AccessGuard;
import ru.itmo.wmp.platform.config.security.JwtPrincipal;
import ru.itmo.wmp.platform.domain.organization.Organization;
import ru.itmo.wmp.platform.domain.user.User;
import ru.itmo.wmp.platform.dto.organization.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/organizations")
@RequiredArgsConstructor
public class OrganizationController {
    private final OrganizationService organizationService;
    private final OrganizationMapper organizationMapper;
    private final AccessGuard accessGuard;

    @GetMapping
    @Operation(
        summary = "List organizations",
        description = "Platform administrator sees every organization, other users see their own"
    )
    public List<OrganizationResponse> getOrganizations(
        @AuthenticationPrincipal Jwt jwt,
        Authentication authentication
    ) {
        List<Organization> organizations = accessGuard.isPlatformAdmin(authentication)
            ? organizationService.findAll()
            : organizationService.findForUser(JwtPrincipal.userId(jwt));

        return organizations.stream()
            .map(this::toResponse)
            .toList();
    }

    @PostMapping
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "Create organization"
    )
    public OrganizationResponse create(
        @Valid @RequestBody CreateOrganizationRequest request
    ) {
        return toResponse(organizationService.create(request.code(), request.name()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@accessGuard.isOrganizationMember(#id, authentication)")
    @Operation(
        summary = "Get organization"
    )
    public OrganizationResponse getById(
        @PathVariable Long id
    ) {
        return toResponse(organizationService.getById(id));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("@accessGuard.isOrganizationManager(#id, authentication)")
    @Operation(
        summary = "Rename organization"
    )
    public OrganizationResponse update(
        @PathVariable Long id,
        @Valid @RequestBody UpdateOrganizationRequest request
    ) {
        return toResponse(organizationService.update(id, request.name(), null));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @Operation(
        summary = "Change organization status"
    )
    public OrganizationResponse updateStatus(
        @PathVariable Long id,
        @Valid @RequestBody UpdateOrganizationStatusRequest request
    ) {
        return toResponse(organizationService.update(id, null, request.status()));
    }

    @PutMapping("/{id}/manager")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @Operation(
        summary = "Assign organization manager",
        description = "Replaces the current manager, only the platform administrator can do it"
    )
    public OrganizationRoleResponse assignManager(
        @PathVariable Long id,
        @Valid @RequestBody AssignOrganizationRoleRequest request,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return organizationMapper.toResponse(
            organizationService.assignManager(id, request.isuNumber(), JwtPrincipal.userId(jwt))
        );
    }

    @GetMapping("/{id}/roles")
    @PreAuthorize("@accessGuard.isOrganizationAdmin(#id, authentication)")
    @Operation(
        summary = "List organization roles"
    )
    public List<OrganizationRoleResponse> getRoles(
        @PathVariable Long id
    ) {
        return organizationService.rolesOf(id)
            .stream()
            .map(organizationMapper::toResponse)
            .toList();
    }

    @PutMapping("/{id}/admins")
    @PreAuthorize("@accessGuard.isOrganizationManager(#id, authentication)")
    @Operation(
        summary = "Grant organization administrator role",
        description = "Only an active participant of the organization can become its administrator"
    )
    public OrganizationRoleResponse grantAdmin(
        @PathVariable Long id,
        @Valid @RequestBody AssignOrganizationRoleRequest request,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return organizationMapper.toResponse(
            organizationService.grantAdmin(id, request.isuNumber(), JwtPrincipal.userId(jwt))
        );
    }

    @DeleteMapping("/{id}/admins/{isuNumber}")
    @PreAuthorize("@accessGuard.isOrganizationManager(#id, authentication)")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
        summary = "Revoke organization administrator role"
    )
    public void revokeAdmin(
        @PathVariable Long id,
        @PathVariable String isuNumber
    ) {
        organizationService.revokeAdmin(id, isuNumber);
    }

    @GetMapping("/{id}/participants")
    @PreAuthorize("@accessGuard.isOrganizationAdmin(#id, authentication)")
    @Operation(
        summary = "List organization participants"
    )
    public List<ParticipantResponse> getParticipants(
        @PathVariable Long id
    ) {
        return organizationService.participantsOf(id)
            .stream()
            .map(organizationMapper::toResponse)
            .toList();
    }

    private OrganizationResponse toResponse(Organization organization) {
        String managerIsuNumber = organizationService.managerOf(organization.getId())
            .map(User::getIsuNumber)
            .orElse(null);

        return organizationMapper.toResponse(organization, managerIsuNumber);
    }
}
