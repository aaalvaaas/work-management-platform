package ru.itmo.wmp.platform.infrastructure.persistance;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.itmo.wmp.platform.domain.User;

public interface JpaUserRepository extends JpaRepository<User, Long> {
}
