package ru.itmo.wmp.platform.application.organization.repository;

import ru.itmo.wmp.platform.domain.organization.Organization;

import java.util.List;
import java.util.Optional;

public interface OrganizationRepository {
    List<Organization> findAll();

    Optional<Organization> findById(Long id);

    Optional<Organization> findByCode(String code);

    boolean existsByCode(String code);

    List<Organization> findByIdIn(List<Long> ids);

    Organization save(Organization organization);
}
