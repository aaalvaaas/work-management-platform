package ru.itmo.wmp.platform.application.security;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import ru.itmo.wmp.platform.application.organization.repository.BlockRoleRepository;
import ru.itmo.wmp.platform.application.organization.repository.OrganizationParticipantRepository;
import ru.itmo.wmp.platform.application.organization.repository.OrganizationRoleRepository;
import ru.itmo.wmp.platform.domain.organization.BlockRole;
import ru.itmo.wmp.platform.domain.organization.BlockRoleType;
import ru.itmo.wmp.platform.domain.organization.OrganizationParticipant;
import ru.itmo.wmp.platform.domain.organization.OrganizationRole;
import ru.itmo.wmp.platform.domain.organization.OrganizationRoleType;
import ru.itmo.wmp.platform.domain.organization.ParticipantStatus;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserAuthorizationService {
    private final UserAuthorizationCache cache;
    private final OrganizationRoleRepository organizationRoleRepository;
    private final OrganizationParticipantRepository participantRepository;
    private final BlockRoleRepository blockRoleRepository;

    @Transactional(readOnly = true)
    public UserAuthorization of(Long userId) {
        return cache.find(userId)
            .orElseGet(() -> {
                UserAuthorization authorization = load(userId);

                cache.put(userId, authorization);

                return authorization;
            });
    }

    public void evict(Long userId) {
        if (userId == null) {
            return;
        }

        cache.evict(userId);

        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                cache.evict(userId);
            }
        });
    }

    private UserAuthorization load(Long userId) {
        Map<Long, Set<OrganizationRoleType>> organizationRoles = organizationRoleRepository
            .findByUserId(userId)
            .stream()
            .collect(Collectors.groupingBy(
                role -> role.getOrganization().getId(),
                Collectors.mapping(OrganizationRole::getRole, Collectors.toSet())
            ));

        Set<Long> activeOrganizations = participantRepository
            .findByUserIdAndStatus(userId, ParticipantStatus.ACTIVE)
            .stream()
            .map(participant -> participant.getOrganization().getId())
            .collect(Collectors.toSet());

        Map<Long, Set<BlockRoleType>> blockRoles = blockRoleRepository
            .findByUserId(userId)
            .stream()
            .collect(Collectors.groupingBy(
                role -> role.getBlock().getId(),
                Collectors.mapping(BlockRole::getRole, Collectors.toSet())
            ));

        return new UserAuthorization(organizationRoles, activeOrganizations, blockRoles);
    }
}
