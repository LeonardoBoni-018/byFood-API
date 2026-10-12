package com.byfood.api.config;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseUrlEnvironmentPostProcessorTest {

    @Test
    void convertsNeonConnectionString() {
        Map<String, Object> result = DatabaseUrlEnvironmentPostProcessor.convert(
                "postgresql://neonuser:p%40ss@ep-xxx.us-east-1.aws.neon.tech/neondb?sslmode=require", null, null);

        assertThat(result.get("spring.datasource.url"))
                .isEqualTo("jdbc:postgresql://ep-xxx.us-east-1.aws.neon.tech/neondb?sslmode=require");
        assertThat(result.get("spring.datasource.username")).isEqualTo("neonuser");
        assertThat(result.get("spring.datasource.password")).isEqualTo("p@ss");
    }

    @Test
    void keepsExplicitCredentials() {
        Map<String, Object> result = DatabaseUrlEnvironmentPostProcessor.convert(
                "postgres://u:p@host:5433/db", "other", "secret");

        assertThat(result.get("spring.datasource.url")).isEqualTo("jdbc:postgresql://host:5433/db");
        assertThat(result).doesNotContainKeys("spring.datasource.username", "spring.datasource.password");
    }

    @Test
    void leavesJdbcUrlUntouched() {
        assertThat(DatabaseUrlEnvironmentPostProcessor.convert("jdbc:postgresql://h/db", null, null)).isEmpty();
        assertThat(DatabaseUrlEnvironmentPostProcessor.convert(null, null, null)).isEmpty();
    }
}
