package ru.itmo.wmp.platform.application.organization.repository;

import ru.itmo.wmp.platform.domain.organization.BlockRole;
import ru.itmo.wmp.platform.domain.organization.BlockRoleType;

import java.util.List;
import java.util.Optional;

public interface BlockRoleRepository {
    List<BlockRole> findByBlockId(Long blockId);

    Optional<BlockRole> findByBlockIdAndRole(Long blockId, BlockRoleType role);

    boolean existsByBlockIdAndUserIdAndRole(Long blockId, Long userId, BlockRoleType role);

    List<BlockRole> findByUserId(Long userId);

    BlockRole save(BlockRole blockRole);

    void delete(BlockRole blockRole);
}
