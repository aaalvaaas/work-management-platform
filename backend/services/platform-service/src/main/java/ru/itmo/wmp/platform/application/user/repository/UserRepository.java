package ru.itmo.wmp.platform.application.user.repository;

import ru.itmo.wmp.platform.domain.user.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository {
    List<User> findAll();

    Optional<User> findById(Long id);

    Optional<User> findByIsuNumber(String isuNumber);

    Optional<User> findByEmail(String email);

    boolean existsByIsuNumber(String isuNumber);

    boolean existsByEmail(String email);

    User save(User user);
}
