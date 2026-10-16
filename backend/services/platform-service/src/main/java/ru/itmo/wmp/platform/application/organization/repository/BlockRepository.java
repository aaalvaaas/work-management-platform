package ru.itmo.wmp.platform.application.organization.repository;

import ru.itmo.wmp.platform.domain.organization.Block;

import java.util.List;
import java.util.Optional;

public interface BlockRepository {
    List<Block> findByOrganizationId(Long organizationId);

    Optional<Block> findById(Long id);

    boolean existsByOrganizationIdAndName(Long organizationId, String name);

    Block save(Block block);
}
