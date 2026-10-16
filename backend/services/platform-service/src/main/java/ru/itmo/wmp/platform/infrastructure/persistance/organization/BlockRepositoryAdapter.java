package ru.itmo.wmp.platform.infrastructure.persistance.organization;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import ru.itmo.wmp.platform.application.organization.repository.BlockRepository;
import ru.itmo.wmp.platform.domain.organization.Block;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class BlockRepositoryAdapter implements BlockRepository {
    private final JpaBlockRepository blockRepository;

    @Override
    public List<Block> findByOrganizationId(Long organizationId) {
        return blockRepository.findByOrganizationId(organizationId);
    }

    @Override
    public Optional<Block> findById(Long id) {
        return blockRepository.findById(id);
    }

    @Override
    public boolean existsByOrganizationIdAndName(Long organizationId, String name) {
        return blockRepository.existsByOrganizationIdAndName(organizationId, name);
    }

    @Override
    public Block save(Block block) {
        return blockRepository.save(block);
    }
}
