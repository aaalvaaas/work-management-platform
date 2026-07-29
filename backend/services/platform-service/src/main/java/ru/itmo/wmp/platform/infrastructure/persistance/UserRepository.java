package ru.itmo.wmp.platform.infrastructure.persistance;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.itmo.wmp.platform.domain.User;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
}
