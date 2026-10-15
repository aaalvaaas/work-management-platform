package ru.itmo.wmp.platform.application.user;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.itmo.wmp.platform.application.user.repository.UserGlobalRoleRepository;
import ru.itmo.wmp.platform.application.user.repository.UserRepository;
import ru.itmo.wmp.platform.domain.user.GlobalRole;
import ru.itmo.wmp.platform.domain.user.User;
import ru.itmo.wmp.platform.domain.user.UserGlobalRole;
import ru.itmo.wmp.platform.exception.UserNotFoundException;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {
    @Mock
    private UserRepository userRepository;

    @Mock
    private UserGlobalRoleRepository userGlobalRoleRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void shouldReturnUserWhenExists() {
        User user = user(1L, "465824");

        when(userRepository.findById(1L))
            .thenReturn(Optional.of(user));

        User result = userService.getById(1L);

        assertEquals(1L, result.getId());
        assertEquals("465824", result.getIsuNumber());

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
    }

    @Test
    void shouldReturnUserByIsuNumber() {
        User user = user(1L, "465824");

        when(userRepository.findByIsuNumber("465824"))
            .thenReturn(Optional.of(user));

        assertEquals(1L, userService.getByIsuNumber("465824").getId());
    }

    @Test
    void shouldThrowExceptionWhenIsuNumberIsUnknown() {
        when(userRepository.findByIsuNumber("465824"))
            .thenReturn(Optional.empty());

        assertThrows(
            UserNotFoundException.class,
            () -> userService.getByIsuNumber("465824")
        );
    }

    @Test
    void shouldReturnAllUsers() {
        when(userRepository.findAll())
            .thenReturn(List.of(user(1L, "465824"), user(2L, "465825")));

        List<User> result = userService.findAll();

        assertEquals(2, result.size());
    }

    @Test
    void shouldReturnGlobalRolesOfUser() {
        UserGlobalRole role = new UserGlobalRole();

        role.setRole(GlobalRole.PLATFORM_ADMIN);

        when(userGlobalRoleRepository.findByUserId(1L))
            .thenReturn(List.of(role));

        assertEquals(Set.of(GlobalRole.PLATFORM_ADMIN), userService.rolesOf(1L));
    }

    @Test
    void shouldReturnEmptyRolesForRegularUser() {
        when(userGlobalRoleRepository.findByUserId(1L))
            .thenReturn(List.of());

        assertTrue(userService.rolesOf(1L).isEmpty());
    }

    @Test
    void shouldUpdateOnlyAllowedProfileFields() {
        User existing = user(1L, "465824");

        existing.setLastName("Дядев");
        existing.setFirstName("Владислав");

        when(userRepository.findById(1L))
            .thenReturn(Optional.of(existing));

        when(userRepository.save(existing))
            .thenReturn(existing);

        User result = userService.updateProfile(1L, "Сергеевич", "Аналитик");

        assertEquals("Сергеевич", result.getPatronymic());
        assertEquals("Аналитик", result.getPosition());
        assertEquals("Дядев", result.getLastName());
        assertEquals("Владислав", result.getFirstName());
        assertEquals("465824", result.getIsuNumber());
    }

    private User user(Long id, String isuNumber) {
        User user = new User();

        user.setId(id);
        user.setIsuNumber(isuNumber);
        user.setEmail(isuNumber + "@niuitmo.ru");
        user.setPasswordHash("hash");
        user.setLastName("Иванов");
        user.setFirstName("Иван");

        return user;
    }
}
