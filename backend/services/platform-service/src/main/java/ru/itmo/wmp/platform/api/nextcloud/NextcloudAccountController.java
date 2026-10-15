package ru.itmo.wmp.platform.api.nextcloud;

import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.itmo.wmp.platform.api.nextcloud.mapper.NextcloudAccountMapper;
import ru.itmo.wmp.platform.application.nextcloud.NextcloudAccountService;
import ru.itmo.wmp.platform.application.user.UserService;
import ru.itmo.wmp.platform.dto.nextcloud.NextcloudAccountResponse;
import ru.itmo.wmp.platform.exception.nextcloud.NextcloudAccountNotFoundException;

@RestController
@RequestMapping("/api/v1/users/{id}/nextcloud")
@RequiredArgsConstructor
public class NextcloudAccountController {
    private final NextcloudAccountService nextcloudAccountService;
    private final UserService userService;
    private final NextcloudAccountMapper nextcloudAccountMapper;

    @GetMapping
    @PreAuthorize("@accessGuard.isSelfOrPlatformAdmin(#id, authentication)")
    @Operation(
        summary = "Get nextcloud account state"
    )
    public NextcloudAccountResponse get(
        @PathVariable Long id
    ) {
        return nextcloudAccountService.findByUserId(id)
            .map(nextcloudAccountMapper::toResponse)
            .orElseThrow(() -> new NextcloudAccountNotFoundException(id));
    }

    @PostMapping("/sync")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    @Operation(
        summary = "Sync account with nextcloud",
        description = "Creates the nextcloud user or updates its profile from platform data"
    )
    public NextcloudAccountResponse sync(
        @PathVariable Long id
    ) {
        return nextcloudAccountMapper.toResponse(
            nextcloudAccountService.syncProfile(userService.getById(id))
        );
    }
}
