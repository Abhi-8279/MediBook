package com.medibook.auth.config;

import com.medibook.auth.entity.User;
import com.medibook.auth.enums.AuthProvider;
import com.medibook.auth.enums.Role;
import com.medibook.auth.repository.UserRepository;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class AdminBootstrapConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(AdminBootstrapConfig.class);

    @Bean
    CommandLineRunner adminBootstrapper(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AppProperties appProperties) {
        return args -> {
            if (userRepository.existsByEmail(appProperties.getAdmin().getEmail())) {
                return;
            }

            User admin = new User();
            admin.setUserId(UUID.randomUUID().toString());
            admin.setFullName(appProperties.getAdmin().getFullName());
            admin.setEmail(appProperties.getAdmin().getEmail().toLowerCase());
            admin.setPasswordHash(passwordEncoder.encode(appProperties.getAdmin().getPassword()));
            admin.setRole(Role.ADMIN);
            admin.setAuthProvider(AuthProvider.LOCAL);
            admin.setActive(true);
            userRepository.save(admin);

            LOGGER.info("Seeded default MediBook admin account: {}", admin.getEmail());
        };
    }
}

