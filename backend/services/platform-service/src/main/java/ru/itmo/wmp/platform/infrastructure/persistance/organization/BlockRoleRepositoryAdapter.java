package ru.itmo.wmp.platform.infrastructure.persistance.organization;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import ru.itmo.wmp.platform.application.organization.repository.BlockRoleRepository;
import ru.itmo.wmp.platform.domain.organization.BlockRole;
import ru.itmo.wmp.platform.domain.organization.BlockRoleType;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class BlockRoleRepositoryAdapter implements BlockRoleRepository {
    private final JpaBlockRoleRepository blockRoleRepository;

    @Override
    public List<BlockRole> findByBlockId(Long blockId) {
        return blockRoleRepository.findByBlockId(blockId);
    }

    @Override
    public Optional<BlockRole> findByBlockIdAndRole(Long blockId, BlockRoleType role) {
        return blockRoleRepository.findByBlockIdAndRole(blockId, role);
    }

    @Override
    public boolean existsByBlockIdAndUserIdAndRole(
        Long blockId,
        Long userId,
        BlockRoleType role
    ) {
        return blockRoleRepository.existsByBlockIdAndUserIdAndRole(blockId, userId, role);
    }

    @Override
    public List<BlockRole> findByUserId(Long userId) {
        return blockRoleRepository.findByUserId(userId);
    }

    @Override
    public BlockRole save(BlockRole blockRole) {
        return blockRoleRepository.save(blockRole);
    }

    @Override
    public void delete(BlockRole blockRole) {
        blockRoleRepository.delete(blockRole);
    }
}
