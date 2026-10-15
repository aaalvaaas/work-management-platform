package ru.itmo.wmp.platform.application.repository;

import ru.itmo.wmp.platform.domain.Organization;

import java.util.List;
import java.util.Optional;

public interface OrganizationRepository {
    List<Organization> findAll();

    Optional<Organization> findById(Long id);

    Organization save(Organization organization);

    void delete(Organization organization);
}
