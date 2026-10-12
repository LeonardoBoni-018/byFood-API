package com.byfood.api.config;

import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Aceita em DB_URL a connection string copiada do Neon/Render ({@code postgresql://user:senha@host/db?sslmode=require})
 * e a converte para o formato JDBC, preenchendo usuário/senha quando não informados à parte.
 * Sem isso o driver rejeita a URL e a API nem sobe (o front só vê timeout).
 */
public class DatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor {

    static final String URL_KEY = "spring.datasource.url";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String raw = environment.getProperty(URL_KEY);
        Map<String, Object> overrides = convert(raw, environment.getProperty("spring.datasource.username"),
                environment.getProperty("spring.datasource.password"));
        if (!overrides.isEmpty()) {
            environment.getPropertySources().addFirst(new MapPropertySource("databaseUrlOverrides", overrides));
        }
    }

    static Map<String, Object> convert(String raw, String currentUser, String currentPassword) {
        Map<String, Object> result = new HashMap<>();
        if (raw == null) {
            return result;
        }
        String url = raw.trim();
        if (!url.startsWith("postgres://") && !url.startsWith("postgresql://")) {
            return result;
        }
        URI uri = URI.create(url);
        int port = uri.getPort();
        StringBuilder jdbc = new StringBuilder("jdbc:postgresql://").append(uri.getHost());
        if (port > 0) {
            jdbc.append(':').append(port);
        }
        jdbc.append(uri.getRawPath() == null ? "" : uri.getRawPath());
        if (uri.getRawQuery() != null) {
            jdbc.append('?').append(uri.getRawQuery());
        }
        result.put(URL_KEY, jdbc.toString());

        String userInfo = uri.getRawUserInfo();
        if (userInfo != null) {
            int sep = userInfo.indexOf(':');
            String user = sep < 0 ? userInfo : userInfo.substring(0, sep);
            String password = sep < 0 ? null : userInfo.substring(sep + 1);
            if (isBlank(currentUser)) {
                result.put("spring.datasource.username", decode(user));
            }
            if (isBlank(currentPassword) && password != null) {
                result.put("spring.datasource.password", decode(password));
            }
        }
        return result;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static String decode(String value) {
        return URLDecoder.decode(value, StandardCharsets.UTF_8);
    }
}
