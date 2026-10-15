package ru.itmo.wmp.platform.application.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.wmp.platform.application.user.repository.UserGlobalRoleRepository;
import ru.itmo.wmp.platform.application.user.repository.UserRepository;
import ru.itmo.wmp.platform.domain.user.GlobalRole;
import ru.itmo.wmp.platform.domain.user.User;
import ru.itmo.wmp.platform.domain.user.UserGlobalRole;
import ru.itmo.wmp.platform.exception.UserNotFoundException;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository userRepository;
    private final UserGlobalRoleRepository userGlobalRoleRepository;

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public User getById(Long id) {
        return userRepository.findById(id)
            .orElseThrow(
                () -> new UserNotFoundException(id)
            );
    }

    public User getByIsuNumber(String isuNumber) {
        return userRepository.findByIsuNumber(isuNumber)
            .orElseThrow(
                () -> new UserNotFoundException(isuNumber)
            );
    }

    public Set<GlobalRole> rolesOf(Long userId) {
        Set<GlobalRole> roles = EnumSet.noneOf(GlobalRole.class);

        userGlobalRoleRepository.findByUserId(userId)
            .stream()
            .map(UserGlobalRole::getRole)
            .forEach(roles::add);

        return roles;
    }

    @Transactional
    public User updateProfile(Long id, String patronymic, String position) {
        User user = getById(id);

        user.setPatronymic(patronymic);
        user.setPosition(position);

        return userRepository.save(user);
    }
}
