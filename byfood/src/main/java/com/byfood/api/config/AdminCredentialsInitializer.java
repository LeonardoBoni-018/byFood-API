package com.byfood.api.config;

import com.byfood.api.model.AppUser;
import com.byfood.api.model.UserRole;
import com.byfood.api.repository.AppUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Garante que o usuário legado ("admin", dono da loja semeada) use a senha definida em ADMIN_PASSWORD,
 * substituindo a senha padrão das migrations. Sem a variável (ambiente local), nada é alterado.
 */
@Component
public class AdminCredentialsInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminCredentialsInitializer.class);
    static final int MIN_PASSWORD_LENGTH = 8;

    private final AppUserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final String username;
    private final String password;

    public AdminCredentialsInitializer(AppUserRepository repository, PasswordEncoder passwordEncoder,
                                       @Value("${app.admin.username}") String username,
                                       @Value("${app.admin.password:}") String password) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.username = username;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (password == null || password.isBlank()) {
            log.warn("ADMIN_PASSWORD não definido: o usuário '{}' mantém a senha padrão (apenas para desenvolvimento).", username);
            return;
        }
        if (password.length() < MIN_PASSWORD_LENGTH) {
            throw new IllegalStateException("ADMIN_PASSWORD deve ter pelo menos " + MIN_PASSWORD_LENGTH + " caracteres");
        }

        AppUser admin = repository.findByLogin(username)
                .orElseGet(() -> AppUser.builder()
                        .login(username)
                        .name("Administrador")
                        .role(UserRole.STORE)
                        .build());

        if (admin.getPasswordHash() == null || !passwordEncoder.matches(password, admin.getPasswordHash())) {
            admin.setPasswordHash(passwordEncoder.encode(password));
            repository.save(admin);
            log.info("Credenciais do usuário '{}' atualizadas a partir de ADMIN_PASSWORD.", username);
        }
    }
}
