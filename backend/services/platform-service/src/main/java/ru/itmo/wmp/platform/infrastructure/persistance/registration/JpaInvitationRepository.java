package ru.itmo.wmp.platform.infrastructure.persistance.registration;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.itmo.wmp.platform.domain.registration.Invitation;

import java.util.List;
import java.util.Optional;

public interface JpaInvitationRepository extends JpaRepository<Invitation, Long> {
    Optional<Invitation> findByTokenHash(String tokenHash);

    List<Invitation> findByIsuNumberAndUsedAtIsNull(String isuNumber);
}
