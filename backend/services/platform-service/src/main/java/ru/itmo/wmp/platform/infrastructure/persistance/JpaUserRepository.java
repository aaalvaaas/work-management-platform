package ru.itmo.wmp.platform.infrastructure.persistance;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.itmo.wmp.platform.domain.User;

import java.util.Optional;

public interface JpaUserRepository extends JpaRepository<User, Long> {
    Optional<User> findByIsuNumber(String isuNumber);

    Optional<User> findByEmail(String email);

    boolean existsByIsuNumber(String isuNumber);

    boolean existsByEmail(String email);
}
