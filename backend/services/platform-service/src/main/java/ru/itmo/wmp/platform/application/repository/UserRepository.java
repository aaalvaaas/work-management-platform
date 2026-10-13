package ru.itmo.wmp.platform.application.repository;

import ru.itmo.wmp.platform.domain.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository {
    List<User> findAll();

    Optional<User> findById(Long id);

    User save(User user);

    void delete(User user);
}
