package ru.itmo.wmp.platform.application.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.wmp.platform.application.user.repository.UserGlobalRoleRepository;
import ru.itmo.wmp.platform.application.user.repository.UserRepository;
import ru.itmo.wmp.platform.config.properties.BootstrapAdminProperties;
import ru.itmo.wmp.platform.domain.user.GlobalRole;
import ru.itmo.wmp.platform.domain.user.User;
import ru.itmo.wmp.platform.domain.user.UserGlobalRole;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlatformAdminBootstrap implements ApplicationRunner {
    private final UserRepository userRepository;
    private final UserGlobalRoleRepository userGlobalRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final BootstrapAdminProperties bootstrapAdminProperties;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userGlobalRoleRepository.existsByRole(GlobalRole.PLATFORM_ADMIN)) {
            return;
        }

        if (!bootstrapAdminProperties.isComplete()) {
            log.warn("Platform administrator is not configured, bootstrap skipped");

            return;
        }

        User admin = userRepository
            .findByIsuNumber(bootstrapAdminProperties.isuNumber())
            .orElseGet(this::createAdminAccount);

        UserGlobalRole adminRole = new UserGlobalRole();

        adminRole.setUser(admin);
        adminRole.setRole(GlobalRole.PLATFORM_ADMIN);

        userGlobalRoleRepository.save(adminRole);

        log.info(
            "Platform administrator bootstrapped for isuNumber={}",
            admin.getIsuNumber()
        );
    }

    private User createAdminAccount() {
        User admin = new User();

        admin.setIsuNumber(bootstrapAdminProperties.isuNumber());
        admin.setEmail(bootstrapAdminProperties.email());
        admin.setLastName(bootstrapAdminProperties.lastName());
        admin.setFirstName(bootstrapAdminProperties.firstName());
        admin.setPasswordHash(
            passwordEncoder.encode(bootstrapAdminProperties.password())
        );

        return userRepository.save(admin);
    }
}
