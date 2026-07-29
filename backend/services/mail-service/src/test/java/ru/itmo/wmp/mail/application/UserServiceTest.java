package ru.itmo.wmp.mail.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.itmo.wmp.mail.exception.UserNotFoundException;
import ru.itmo.wmp.mail.infrastructure.persistance.UserRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void shouldReturnUserWhenExists() {
        User user = new User();

        user.setId(1L);
        user.setUsername("test");
        user.setEmail("test@test.ru");

        when(userRepository.findById(1L))
            .thenReturn(Optional.of(user));

        User result = userService.getById(1L);

        assertEquals(1L, result.getId());
        assertEquals("test", result.getUsername());
        assertEquals("test@test.ru", result.getEmail());

        verify(userRepository)
            .findById(1L);
    }

    @Test
    void shouldThrowExceptionWhenUserNotFound() {
        when(userRepository.findById(999L))
            .thenReturn(Optional.empty());

        assertThrows(
            UserNotFoundException.class,
            () -> userService.getById(999L)
        );

        verify(userRepository)
            .findById(999L);
    }

    @Test
    void shouldReturnAllUsers() {
        User user1 = new User();

        user1.setId(1L);
        user1.setUsername("user1");
        user1.setEmail("user1@test.ru");

        User user2 = new User();

        user2.setId(2L);
        user2.setUsername("user2");
        user2.setEmail("user2@test.ru");

        when(userRepository.findAll())
            .thenReturn(List.of(user1, user2));

        List<User> result = userService.findAll();

        assertEquals(2, result.size());
        assertEquals("user1", result.get(0).getUsername());

        verify(userRepository)
            .findAll();
    }

    @Test
    void shouldCreateUser() {
        User user = new User();

        user.setUsername("newuser");
        user.setEmail("new@test.ru");

        User savedUser = new User();

        savedUser.setId(1L);
        savedUser.setUsername("newuser");
        savedUser.setEmail("new@test.ru");

        when(userRepository.save(user))
            .thenReturn(savedUser);

        User result = userService.save(user);

        assertNotNull(result.getId());
        assertEquals("newuser", result.getUsername());

        verify(userRepository)
            .save(user);
    }

    @Test
    void shouldUpdateUser() {
        User existing = new User();

        existing.setId(1L);
        existing.setUsername("old");
        existing.setEmail("old@test.ru");

        User updated = new User();

        updated.setUsername("new");
        updated.setEmail("new@test.ru");

        when(userRepository.findById(1L))
            .thenReturn(Optional.of(existing));

        when(userRepository.save(existing))
            .thenReturn(existing);

        User result = userService.update(1L, updated);

        assertEquals("new", result.getUsername());
        assertEquals("new@test.ru", result.getEmail());

        verify(userRepository)
            .save(existing);
    }

    @Test
    void shouldDeleteUser() {
        User user = new User();

        user.setId(1L);

        when(userRepository.findById(1L))
            .thenReturn(Optional.of(user));

        userService.delete(1L);

        verify(userRepository)
            .delete(user);
    }

    @Test
    void shouldThrowExceptionWhenDeletingUnknownUser() {
        when(userRepository.findById(999L))
            .thenReturn(Optional.empty());

        assertThrows(
            UserNotFoundException.class,
            () -> userService.delete(999L)
        );

        verify(userRepository, never())
            .delete(any(User.class));
    }
}
