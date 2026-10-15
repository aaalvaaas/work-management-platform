package ru.itmo.wmp.platform.api.registration.mapper;

import org.springframework.stereotype.Component;
import ru.itmo.wmp.platform.application.registration.RegistrationDetails;
import ru.itmo.wmp.platform.application.registration.RegistrationStart;
import ru.itmo.wmp.platform.dto.registration.RegistrationDetailsResponse;
import ru.itmo.wmp.platform.dto.registration.RegistrationStartResponse;

@Component
public class RegistrationMapper {
    public RegistrationStartResponse toResponse(RegistrationStart start) {
        return new RegistrationStartResponse(
            EmailMask.apply(start.email()),
            start.expiresAt()
        );
    }

    public RegistrationDetailsResponse toResponse(RegistrationDetails details) {
        return new RegistrationDetailsResponse(
            details.isuNumber(),
            details.lastName(),
            details.firstName(),
            details.patronymic(),
            EmailMask.apply(details.email()),
            details.organizations()
        );
    }
}
