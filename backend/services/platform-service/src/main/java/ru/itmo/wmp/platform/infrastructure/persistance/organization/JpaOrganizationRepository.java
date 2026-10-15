package ru.itmo.wmp.platform.infrastructure.persistance.organization;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.itmo.wmp.platform.domain.organization.Organization;

public interface JpaOrganizationRepository extends JpaRepository<Organization, Long> {
}
