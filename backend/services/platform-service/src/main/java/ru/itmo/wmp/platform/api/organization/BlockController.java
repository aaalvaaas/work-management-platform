package ru.itmo.wmp.platform.api.organization;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import ru.itmo.wmp.platform.api.organization.mapper.BlockMapper;
import ru.itmo.wmp.platform.application.organization.BlockService;
import ru.itmo.wmp.platform.config.security.JwtPrincipal;
import ru.itmo.wmp.platform.domain.organization.Block;
import ru.itmo.wmp.platform.domain.user.User;
import ru.itmo.wmp.platform.dto.organization.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/organizations/{organizationId}/blocks")
@RequiredArgsConstructor
public class BlockController {
    private final BlockService blockService;
    private final BlockMapper blockMapper;

    @GetMapping
    @PreAuthorize("@accessGuard.isOrganizationMember(#organizationId, authentication)")
    @Operation(
        summary = "List organization blocks"
    )
    public List<BlockResponse> getBlocks(
        @PathVariable Long organizationId
    ) {
        return blockService.findByOrganization(organizationId)
            .stream()
            .map(this::toResponse)
            .toList();
    }

    @PostMapping
    @PreAuthorize("@accessGuard.isOrganizationAdmin(#organizationId, authentication)")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "Create block"
    )
    public BlockResponse create(
        @PathVariable Long organizationId,
        @Valid @RequestBody CreateBlockRequest request
    ) {
        return toResponse(blockService.create(organizationId, request.name()));
    }

    @GetMapping("/{blockId}")
    @PreAuthorize("@accessGuard.isOrganizationMember(#organizationId, authentication)")
    @Operation(
        summary = "Get block"
    )
    public BlockResponse getById(
        @PathVariable Long organizationId,
        @PathVariable Long blockId
    ) {
        return toResponse(blockService.getInOrganization(organizationId, blockId));
    }

    @PatchMapping("/{blockId}")
    @PreAuthorize("@accessGuard.isOrganizationAdmin(#organizationId, authentication)")
    @Operation(
        summary = "Update block"
    )
    public BlockResponse update(
        @PathVariable Long organizationId,
        @PathVariable Long blockId,
        @Valid @RequestBody UpdateBlockRequest request
    ) {
        return toResponse(
            blockService.update(organizationId, blockId, request.name(), request.status())
        );
    }

    @PutMapping("/{blockId}/manager")
    @PreAuthorize("@accessGuard.isOrganizationAdmin(#organizationId, authentication)")
    @Operation(
        summary = "Assign block manager",
        description = "Only an active participant of the organization can lead its block"
    )
    public BlockRoleResponse assignManager(
        @PathVariable Long organizationId,
        @PathVariable Long blockId,
        @Valid @RequestBody AssignBlockRoleRequest request,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return blockMapper.toResponse(
            blockService.assignManager(
                organizationId,
                blockId,
                request.isuNumber(),
                JwtPrincipal.userId(jwt)
            )
        );
    }

    private BlockResponse toResponse(Block block) {
        String managerIsuNumber = blockService.managerOf(block.getId())
            .map(User::getIsuNumber)
            .orElse(null);

        return blockMapper.toResponse(block, managerIsuNumber);
    }
}
