package ru.itmo.wmp.platform.application.organization;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.wmp.platform.application.organization.repository.OrganizationParticipantRepository;
import ru.itmo.wmp.platform.application.organization.repository.OrganizationRepository;
import ru.itmo.wmp.platform.application.organization.repository.OrganizationRoleRepository;
import ru.itmo.wmp.platform.application.security.UserAuthorizationService;
import ru.itmo.wmp.platform.application.user.UserService;
import ru.itmo.wmp.platform.domain.organization.*;
import ru.itmo.wmp.platform.domain.user.User;
import ru.itmo.wmp.platform.exception.organization.NotOrganizationParticipantException;
import ru.itmo.wmp.platform.exception.organization.OrganizationCodeAlreadyUsedException;
import ru.itmo.wmp.platform.exception.organization.OrganizationNotFoundException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrganizationService {
    private final OrganizationRepository organizationRepository;
    private final OrganizationRoleRepository organizationRoleRepository;
    private final OrganizationParticipantRepository participantRepository;
    private final UserService userService;
    private final UserAuthorizationService userAuthorizationService;

    public List<Organization> findAll() {
        return organizationRepository.findAll();
    }

    public List<Organization> findForUser(Long userId) {
        List<Long> organizationIds = participantRepository
            .findByUserIdAndStatus(userId, ParticipantStatus.ACTIVE)
            .stream()
            .map(participant -> participant.getOrganization().getId())
            .distinct()
            .toList();

        return organizationIds.isEmpty()
            ? List.of()
            : organizationRepository.findByIdIn(organizationIds);
    }

    public Organization getById(Long id) {
        return organizationRepository.findById(id)
            .orElseThrow(() -> new OrganizationNotFoundException(id));
    }

    public Organization getByCode(String code) {
        return organizationRepository.findByCode(code)
            .orElseThrow(() -> new OrganizationNotFoundException(code));
    }

    public Optional<User> managerOf(Long organizationId) {
        return organizationRoleRepository
            .findByOrganizationIdAndRole(organizationId, OrganizationRoleType.MANAGER)
            .map(OrganizationRole::getUser);
    }

    public List<OrganizationRole> rolesOf(Long organizationId) {
        return organizationRoleRepository.findByOrganizationId(organizationId);
    }

    public List<OrganizationParticipant> participantsOf(Long organizationId) {
        getById(organizationId);

        return participantRepository.findByOrganizationId(organizationId);
    }

    @Transactional
    public Organization create(String code, String name) {
        if (organizationRepository.existsByCode(code)) {
            throw new OrganizationCodeAlreadyUsedException(code);
        }

        Organization organization = new Organization();

        organization.setCode(code);
        organization.setName(name);
        organization.setStatus(OrganizationStatus.ACTIVE);

        return organizationRepository.save(organization);
    }

    @Transactional
    public Organization update(Long id, String name, OrganizationStatus status) {
        Organization organization = getById(id);

        if (name != null) {
            organization.setName(name);
        }

        if (status != null) {
            organization.setStatus(status);
        }

        return organizationRepository.save(organization);
    }

    @Transactional
    public OrganizationRole assignManager(Long organizationId, String isuNumber, Long actorUserId) {
        Organization organization = getById(organizationId);
        User user = userService.getByIsuNumber(isuNumber);

        OrganizationRole managerRole = organizationRoleRepository
            .findByOrganizationIdAndRole(organizationId, OrganizationRoleType.MANAGER)
            .orElseGet(() -> role(organization, user, OrganizationRoleType.MANAGER, actorUserId));

        if (managerRole.getUser() != null) {
            userAuthorizationService.evict(managerRole.getUser().getId());
        }

        managerRole.setUser(user);
        managerRole.setGrantedBy(userService.getById(actorUserId));
        managerRole.setGrantedAt(LocalDateTime.now());

        userAuthorizationService.evict(user.getId());

        return organizationRoleRepository.save(managerRole);
    }

    @Transactional
    public OrganizationRole grantAdmin(Long organizationId, String isuNumber, Long actorUserId) {
        Organization organization = getById(organizationId);
        User user = userService.getByIsuNumber(isuNumber);

        requireActiveParticipant(organizationId, user.getId(), isuNumber);

        userAuthorizationService.evict(user.getId());

        return organizationRoleRepository
            .findByOrganizationIdAndUserIdAndRole(
                organizationId,
                user.getId(),
                OrganizationRoleType.ADMIN
            )
            .orElseGet(() -> organizationRoleRepository.save(
                role(organization, user, OrganizationRoleType.ADMIN, actorUserId)
            ));
    }

    @Transactional
    public void revokeAdmin(Long organizationId, String isuNumber) {
        getById(organizationId);

        User user = userService.getByIsuNumber(isuNumber);

        organizationRoleRepository
            .findByOrganizationIdAndUserIdAndRole(
                organizationId,
                user.getId(),
                OrganizationRoleType.ADMIN
            )
            .ifPresent(organizationRoleRepository::delete);

        userAuthorizationService.evict(user.getId());
    }

    private void requireActiveParticipant(Long organizationId, Long userId, String isuNumber) {
        boolean participant = participantRepository.existsByOrganizationIdAndUserIdAndStatus(
            organizationId,
            userId,
            ParticipantStatus.ACTIVE
        );

        if (!participant) {
            throw new NotOrganizationParticipantException(organizationId, isuNumber);
        }
    }

    private OrganizationRole role(
        Organization organization,
        User user,
        OrganizationRoleType type,
        Long actorUserId
    ) {
        OrganizationRole organizationRole = new OrganizationRole();

        organizationRole.setOrganization(organization);
        organizationRole.setUser(user);
        organizationRole.setRole(type);
        organizationRole.setGrantedBy(userService.getById(actorUserId));

        return organizationRole;
    }
}
