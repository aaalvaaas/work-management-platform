package ru.itmo.wmp.platform.application.registration.repository;

import ru.itmo.wmp.platform.domain.registration.RegistrationRequest;
import ru.itmo.wmp.platform.domain.registration.RegistrationRequestStatus;

import java.util.List;
import java.util.Optional;

public interface RegistrationRequestRepository {
    Optional<RegistrationRequest> findById(Long id);

    List<RegistrationRequest> findByOrganizationIdOrderByIdDesc(Long organizationId);

    List<RegistrationRequest> findByOrganizationIdAndStatusOrderByIdDesc(
        Long organizationId,
        RegistrationRequestStatus status
    );

    boolean existsByOrganizationIdAndIsuNumberAndStatus(
        Long organizationId,
        String isuNumber,
        RegistrationRequestStatus status
    );

    List<RegistrationRequest> findByIsuNumberAndStatus(
        String isuNumber,
        RegistrationRequestStatus status
    );

    RegistrationRequest save(RegistrationRequest request);
}
