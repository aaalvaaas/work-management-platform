package ru.itmo.wmp.platform.application.registration.repository;

import ru.itmo.wmp.platform.domain.registration.Invitation;

import java.util.List;
import java.util.Optional;

public interface InvitationRepository {
    Optional<Invitation> findByTokenHash(String tokenHash);

    List<Invitation> findByIsuNumberAndUsedAtIsNull(String isuNumber);

    Invitation save(Invitation invitation);

    List<Invitation> saveAll(List<Invitation> invitations);
}
