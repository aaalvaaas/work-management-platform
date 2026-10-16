package ru.itmo.wmp.platform.api.registration.mapper;

import org.springframework.stereotype.Component;
import ru.itmo.wmp.platform.domain.registration.RegistrationRequest;
import ru.itmo.wmp.platform.dto.registration.RegistrationRequestResponse;

import java.util.List;

@Component
public class RegistrationRequestMapper {
    public RegistrationRequestResponse toResponse(RegistrationRequest request) {
        return new RegistrationRequestResponse(
            request.getId(),
            request.getOrganization().getId(),
            request.getOrganization().getName(),
            request.getIsuNumber(),
            request.getLastName(),
            request.getFirstName(),
            request.getPatronymic(),
            request.getEmail(),
            request.getPosition(),
            request.getStatus(),
            request.getDecisionComment(),
            request.getCreatedAt(),
            request.getDecidedAt()
        );
    }

    public List<RegistrationRequestResponse> toResponses(List<RegistrationRequest> requests) {
        return requests.stream()
            .map(this::toResponse)
            .toList();
    }
}
