package ru.itmo.wmp.platform.api.registration;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import ru.itmo.wmp.platform.api.registration.mapper.RegistrationMapper;
import ru.itmo.wmp.platform.api.user.mapper.UserMapper;
import ru.itmo.wmp.platform.application.registration.RegistrationRequestService;
import ru.itmo.wmp.platform.application.registration.RegistrationService;
import ru.itmo.wmp.platform.application.registration.RegistrationSubmission;
import ru.itmo.wmp.platform.api.registration.mapper.RegistrationRequestMapper;
import ru.itmo.wmp.platform.domain.user.User;
import ru.itmo.wmp.platform.dto.registration.CompleteRegistrationRequest;
import ru.itmo.wmp.platform.dto.registration.RegistrationDetailsResponse;
import ru.itmo.wmp.platform.dto.registration.RegistrationStartResponse;
import ru.itmo.wmp.platform.dto.registration.RegistrationRequestResponse;
import ru.itmo.wmp.platform.dto.registration.StartRegistrationRequest;
import ru.itmo.wmp.platform.dto.registration.SubmitRegistrationRequest;
import ru.itmo.wmp.platform.dto.user.UserResponse;

import java.util.Set;

@RestController
@RequestMapping("/api/v1/registration")
@RequiredArgsConstructor
public class RegistrationController {
    private final RegistrationService registrationService;
    private final RegistrationMapper registrationMapper;
    private final RegistrationRequestService registrationRequestService;
    private final RegistrationRequestMapper registrationRequestMapper;
    private final UserMapper userMapper;

    @PostMapping("/requests")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "Submit a registration request",
        description = "Asks the organization with the given code to accept a new member; "
            + "the account is created when an organization administrator approves the request"
    )
    public RegistrationRequestResponse submit(
        @Valid @RequestBody SubmitRegistrationRequest request
    ) {
        return registrationRequestMapper.toResponse(
            registrationRequestService.submit(new RegistrationSubmission(
                request.organizationCode(),
                request.isuNumber(),
                request.lastName(),
                request.firstName(),
                request.patronymic(),
                request.email(),
                request.position(),
                request.password()
            ))
        );
    }

    @PostMapping("/start")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(
        summary = "Start registration",
        description = "Checks the ISU number against organization participant lists and sends a confirmation link"
    )
    public RegistrationStartResponse start(
        @Valid @RequestBody StartRegistrationRequest request
    ) {
        return registrationMapper.toResponse(
            registrationService.start(request.isuNumber())
        );
    }

    @GetMapping("/{token}")
    @Operation(
        summary = "Get registration details",
        description = "Returns the data pre-entered by an organization for the invited participant"
    )
    public RegistrationDetailsResponse details(
        @PathVariable String token
    ) {
        return registrationMapper.toResponse(
            registrationService.details(token)
        );
    }

    @PostMapping("/complete")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
        summary = "Complete registration",
        description = "Creates the account and links it to all participant records with the same ISU number"
    )
    public UserResponse complete(
        @Valid @RequestBody CompleteRegistrationRequest request
    ) {
        User user = registrationService.complete(
            request.token(),
            request.password()
        );

        return userMapper.toResponse(user, Set.of());
    }
}
