package ru.itmo.wmp.platform.infrastructure.persistance.registration;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import ru.itmo.wmp.platform.application.registration.repository.RegistrationRequestRepository;
import ru.itmo.wmp.platform.domain.registration.RegistrationRequest;
import ru.itmo.wmp.platform.domain.registration.RegistrationRequestStatus;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class RegistrationRequestRepositoryAdapter implements RegistrationRequestRepository {
    private final JpaRegistrationRequestRepository requestRepository;

    @Override
    public Optional<RegistrationRequest> findById(Long id) {
        return requestRepository.findById(id);
    }

    @Override
    public List<RegistrationRequest> findByOrganizationIdOrderByIdDesc(Long organizationId) {
        return requestRepository.findByOrganizationIdOrderByIdDesc(organizationId);
    }

    @Override
    public List<RegistrationRequest> findByOrganizationIdAndStatusOrderByIdDesc(
        Long organizationId,
        RegistrationRequestStatus status
    ) {
        return requestRepository.findByOrganizationIdAndStatusOrderByIdDesc(organizationId, status);
    }

    @Override
    public boolean existsByOrganizationIdAndIsuNumberAndStatus(
        Long organizationId,
        String isuNumber,
        RegistrationRequestStatus status
    ) {
        return requestRepository.existsByOrganizationIdAndIsuNumberAndStatus(
            organizationId,
            isuNumber,
            status
        );
    }

    @Override
    public List<RegistrationRequest> findByIsuNumberAndStatus(
        String isuNumber,
        RegistrationRequestStatus status
    ) {
        return requestRepository.findByIsuNumberAndStatus(isuNumber, status);
    }

    @Override
    public RegistrationRequest save(RegistrationRequest request) {
        return requestRepository.save(request);
    }
}
