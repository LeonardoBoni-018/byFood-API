package com.byfood.api.config;

import com.byfood.api.model.AdminUser;
import com.byfood.api.repository.AdminUserRepository;
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

    private final AdminUserRepository repository = mock(AdminUserRepository.class);
    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);

    private AdminCredentialsInitializer initializer(String password) {
        return new AdminCredentialsInitializer(repository, encoder, "admin", password);
    }

    @Test
    void shouldReplaceDefaultPassword() {
        AdminUser admin = AdminUser.builder().username("admin").passwordHash(encoder.encode("admin123")).build();
        when(repository.findByUsername("admin")).thenReturn(Optional.of(admin));

        initializer("S3nh@Forte!").run(null);

        verify(repository).save(admin);
        assertThat(encoder.matches("S3nh@Forte!", admin.getPasswordHash())).isTrue();
    }

    @Test
    void shouldCreateAdminWhenMissing() {
        when(repository.findByUsername("admin")).thenReturn(Optional.empty());

        initializer("S3nh@Forte!").run(null);

        verify(repository).save(any(AdminUser.class));
    }

    @Test
    void shouldNotRehashWhenPasswordAlreadyMatches() {
        AdminUser admin = AdminUser.builder().username("admin").passwordHash(encoder.encode("S3nh@Forte!")).build();
        when(repository.findByUsername("admin")).thenReturn(Optional.of(admin));

        initializer("S3nh@Forte!").run(null);

        verify(repository, never()).save(any());
    }

    @Test
    void shouldDoNothingWithoutConfiguredPassword() {
        initializer("").run(null);

        verify(repository, never()).findByUsername(any());
    }

    @Test
    void shouldRejectShortPassword() {
        assertThatThrownBy(() -> initializer("123").run(null))
                .isInstanceOf(IllegalStateException.class);
    }
}
