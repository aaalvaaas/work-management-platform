package ru.itmo.wmp.platform.infrastructure.persistance.registration;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import ru.itmo.wmp.platform.application.registration.repository.InvitationRepository;
import ru.itmo.wmp.platform.domain.registration.Invitation;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class InvitationRepositoryAdapter implements InvitationRepository {
    private final JpaInvitationRepository invitationRepository;

    @Override
    public Optional<Invitation> findByTokenHash(String tokenHash) {
        return invitationRepository.findByTokenHash(tokenHash);
    }

    @Override
    public List<Invitation> findByIsuNumberAndUsedAtIsNull(String isuNumber) {
        return invitationRepository.findByIsuNumberAndUsedAtIsNull(isuNumber);
    }

    @Override
    public Invitation save(Invitation invitation) {
        return invitationRepository.save(invitation);
    }

    @Override
    public List<Invitation> saveAll(List<Invitation> invitations) {
        return invitationRepository.saveAll(invitations);
    }
}
