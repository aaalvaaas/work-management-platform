package ru.itmo.wmp.platform.infrastructure.persistance;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.itmo.wmp.platform.domain.Organization;

public interface JpaOrganizationRepository extends JpaRepository<Organization, Long> {
}
