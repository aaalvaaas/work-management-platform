package ru.itmo.wmp.platform.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.itmo.wmp.platform.domain.User;
import ru.itmo.wmp.platform.exception.UserNotFoundException;
import ru.itmo.wmp.platform.infrastructure.persistance.UserRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public User save(User user) {
        return userRepository.save(user);
    }

    public User getById(Long id) {
        return userRepository.findById(id)
            .orElseThrow(
                () -> new UserNotFoundException(id)
            );
    }

    public User update(Long id, User updatedUser) {
        User user = getById(id);

        user.setUsername(updatedUser.getUsername());
        user.setEmail(updatedUser.getEmail());

        return userRepository.save(user);
    }

    public void delete(Long id) {
        User user = getById(id);

        userRepository.delete(user);
    }
}
