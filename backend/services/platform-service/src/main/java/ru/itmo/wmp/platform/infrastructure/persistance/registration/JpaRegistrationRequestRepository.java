package ru.itmo.wmp.platform.infrastructure.persistance.registration;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.itmo.wmp.platform.domain.registration.RegistrationRequest;
import ru.itmo.wmp.platform.domain.registration.RegistrationRequestStatus;

import java.util.List;

public interface JpaRegistrationRequestRepository extends JpaRepository<RegistrationRequest, Long> {
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
}
