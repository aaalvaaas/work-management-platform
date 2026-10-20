package ru.itmo.wmp.platform.api.internal;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.itmo.wmp.platform.application.organization.OrganizationService;
import ru.itmo.wmp.platform.domain.organization.Organization;
import ru.itmo.wmp.platform.dto.organization.InternalOrganizationResponse;

@RestController
@RequestMapping("/internal/v1/organizations")
@RequiredArgsConstructor
public class InternalOrganizationController {
    private final OrganizationService organizationService;

    @GetMapping("/{organizationId}")
    @Operation(
        summary = "Organization details for services",
        description = "Protected by the internal key and never routed through the gateway"
    )
    public InternalOrganizationResponse organization(@PathVariable Long organizationId) {
        Organization organization = organizationService.getById(organizationId);

        return new InternalOrganizationResponse(
            organization.getId(),
            organization.getCode(),
            organization.getName(),
            organization.getStatus().name()
        );
    }
}
