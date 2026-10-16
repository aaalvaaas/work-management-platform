package ru.itmo.wmp.platform.application.organization;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.wmp.platform.application.organization.repository.BlockRepository;
import ru.itmo.wmp.platform.application.organization.repository.BlockRoleRepository;
import ru.itmo.wmp.platform.application.organization.repository.OrganizationParticipantRepository;
import ru.itmo.wmp.platform.application.user.UserService;
import ru.itmo.wmp.platform.domain.organization.*;
import ru.itmo.wmp.platform.domain.user.User;
import ru.itmo.wmp.platform.exception.organization.BlockNameAlreadyUsedException;
import ru.itmo.wmp.platform.exception.organization.BlockNotFoundException;
import ru.itmo.wmp.platform.exception.organization.NotOrganizationParticipantException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BlockService {
    private final BlockRepository blockRepository;
    private final BlockRoleRepository blockRoleRepository;
    private final OrganizationService organizationService;
    private final OrganizationParticipantRepository participantRepository;
    private final UserService userService;

    public List<Block> findByOrganization(Long organizationId) {
        organizationService.getById(organizationId);

        return blockRepository.findByOrganizationId(organizationId);
    }

    public Block getById(Long blockId) {
        return blockRepository.findById(blockId)
            .orElseThrow(() -> new BlockNotFoundException(blockId));
    }

    public Block getInOrganization(Long organizationId, Long blockId) {
        Block block = getById(blockId);

        if (!Objects.equals(block.getOrganization().getId(), organizationId)) {
            throw new BlockNotFoundException(organizationId, blockId);
        }

        return block;
    }

    public Optional<User> managerOf(Long blockId) {
        return blockRoleRepository
            .findByBlockIdAndRole(blockId, BlockRoleType.MANAGER)
            .map(BlockRole::getUser);
    }

    @Transactional
    public Block create(Long organizationId, String name) {
        Organization organization = organizationService.getById(organizationId);

        if (blockRepository.existsByOrganizationIdAndName(organizationId, name)) {
            throw new BlockNameAlreadyUsedException(organizationId, name);
        }

        Block block = new Block();

        block.setOrganization(organization);
        block.setName(name);
        block.setStatus(BlockStatus.ACTIVE);

        return blockRepository.save(block);
    }

    @Transactional
    public Block update(Long organizationId, Long blockId, String name, BlockStatus status) {
        Block block = getInOrganization(organizationId, blockId);

        if (name != null && !name.equals(block.getName())) {
            if (blockRepository.existsByOrganizationIdAndName(organizationId, name)) {
                throw new BlockNameAlreadyUsedException(organizationId, name);
            }

            block.setName(name);
        }

        if (status != null) {
            block.setStatus(status);
        }

        return blockRepository.save(block);
    }

    @Transactional
    public BlockRole assignManager(
        Long organizationId,
        Long blockId,
        String isuNumber,
        Long actorUserId
    ) {
        Block block = getInOrganization(organizationId, blockId);
        User user = userService.getByIsuNumber(isuNumber);

        boolean participant = participantRepository.existsByOrganizationIdAndUserIdAndStatus(
            organizationId,
            user.getId(),
            ParticipantStatus.ACTIVE
        );

        if (!participant) {
            throw new NotOrganizationParticipantException(organizationId, isuNumber);
        }

        BlockRole managerRole = blockRoleRepository
            .findByBlockIdAndRole(blockId, BlockRoleType.MANAGER)
            .orElseGet(() -> {
                BlockRole created = new BlockRole();

                created.setBlock(block);
                created.setRole(BlockRoleType.MANAGER);

                return created;
            });

        managerRole.setUser(user);
        managerRole.setGrantedBy(userService.getById(actorUserId));
        managerRole.setGrantedAt(LocalDateTime.now());

        return blockRoleRepository.save(managerRole);
    }
}
