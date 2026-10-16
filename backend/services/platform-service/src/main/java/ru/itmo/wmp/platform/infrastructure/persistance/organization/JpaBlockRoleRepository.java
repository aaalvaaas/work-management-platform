package ru.itmo.wmp.platform.infrastructure.persistance.organization;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.itmo.wmp.platform.domain.organization.BlockRole;
import ru.itmo.wmp.platform.domain.organization.BlockRoleType;

import java.util.List;
import java.util.Optional;

public interface JpaBlockRoleRepository extends JpaRepository<BlockRole, Long> {
    List<BlockRole> findByBlockId(Long blockId);

    Optional<BlockRole> findByBlockIdAndRole(Long blockId, BlockRoleType role);

    boolean existsByBlockIdAndUserIdAndRole(Long blockId, Long userId, BlockRoleType role);

    List<BlockRole> findByUserId(Long userId);
}
