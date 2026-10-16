package ru.itmo.wmp.platform.infrastructure.persistance.organization;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.itmo.wmp.platform.domain.organization.Organization;

import java.util.List;
import java.util.Optional;

public interface JpaOrganizationRepository extends JpaRepository<Organization, Long> {
    Optional<Organization> findByCode(String code);

    boolean existsByCode(String code);

    List<Organization> findByIdIn(List<Long> ids);
}
