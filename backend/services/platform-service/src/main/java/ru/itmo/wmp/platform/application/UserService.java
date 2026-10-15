package ru.itmo.wmp.platform.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.wmp.platform.application.repository.UserGlobalRoleRepository;
import ru.itmo.wmp.platform.application.repository.UserRepository;
import ru.itmo.wmp.platform.domain.GlobalRole;
import ru.itmo.wmp.platform.domain.User;
import ru.itmo.wmp.platform.domain.UserGlobalRole;
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

    /**
     * Обновляет только те поля, которые пользователь может менять сам.
     * ФИО, email и номер ИСУ приходят из списка участников организации
     * и здесь не меняются.
     */
    @Transactional
    public User updateProfile(Long id, String patronymic, String position) {
        User user = getById(id);

        user.setPatronymic(patronymic);
        user.setPosition(position);

        return userRepository.save(user);
    }
}
