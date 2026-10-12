package com.byfood.api.config;

import com.byfood.api.model.AppUser;
import com.byfood.api.model.UserRole;
import com.byfood.api.repository.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminCredentialsInitializerTest {

    private final AppUserRepository repository = mock(AppUserRepository.class);
    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);

    private AdminCredentialsInitializer initializer(String password) {
        return new AdminCredentialsInitializer(repository, encoder, "admin", password);
    }

    private AppUser admin(String rawPassword) {
        return AppUser.builder().login("admin").name("Administrador").role(UserRole.STORE)
                .passwordHash(encoder.encode(rawPassword)).build();
    }

    @Test
    void shouldReplaceDefaultPassword() {
        AppUser admin = admin("admin123");
        when(repository.findByLogin("admin")).thenReturn(Optional.of(admin));

        initializer("S3nh@Forte!").run(null);

        verify(repository).save(admin);
        assertThat(encoder.matches("S3nh@Forte!", admin.getPasswordHash())).isTrue();
    }

    @Test
    void shouldCreateAdminWhenMissing() {
        when(repository.findByLogin("admin")).thenReturn(Optional.empty());

        initializer("S3nh@Forte!").run(null);

        verify(repository).save(any(AppUser.class));
    }

    @Test
    void shouldNotRehashWhenPasswordAlreadyMatches() {
        AppUser admin = admin("S3nh@Forte!");
        when(repository.findByLogin("admin")).thenReturn(Optional.of(admin));

        initializer("S3nh@Forte!").run(null);

        verify(repository, never()).save(any());
    }

    @Test
    void shouldDoNothingWithoutConfiguredPassword() {
        initializer("").run(null);

        verify(repository, never()).findByLogin(any());
    }

    @Test
    void shouldRejectShortPassword() {
        assertThatThrownBy(() -> initializer("123").run(null))
                .isInstanceOf(IllegalStateException.class);
    }
}
