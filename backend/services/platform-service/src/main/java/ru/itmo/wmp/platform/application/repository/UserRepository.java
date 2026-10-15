package ru.itmo.wmp.platform.application.repository;

import ru.itmo.wmp.platform.domain.User;

import java.util.List;
import java.util.Optional;

/**
 * Аккаунты платформы не удаляются: исключение из организации и блокировка
 * аккаунта меняют статусы, но запись пользователя сохраняется.
 */
public interface UserRepository {
    List<User> findAll();

    Optional<User> findById(Long id);

    Optional<User> findByIsuNumber(String isuNumber);

    Optional<User> findByEmail(String email);

    boolean existsByIsuNumber(String isuNumber);

    boolean existsByEmail(String email);

    User save(User user);
}
