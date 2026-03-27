package com.pbm5.bugtracker.config;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.StringUtils;

/**
 * Accepts Neon / Heroku style {@code postgresql://} (or {@code postgres://}) URLs from
 * {@code SPRING_DATASOURCE_URL} or {@code DATABASE_URL} and exposes {@code spring.datasource.*}
 * so Hikari receives a proper {@code jdbc:postgresql://} URL.
 * <p>
 * Runs with {@link Ordered#LOWEST_PRECEDENCE} so config data (e.g. {@code application.properties}
 * resolving {@code ${SPRING_DATASOURCE_URL}}) is applied first; we then prepend the JDBC URL so it wins.
 */
public class PostgresJdbcUrlEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    private static final String PROPERTY_SOURCE_NAME = "postgresJdbcUrlTranslation";

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        // Prefer explicit env (PaaS DATABASE_URL) before resolved spring.datasource.url, which may
        // still be the local default when only DATABASE_URL is set on the host.
        String raw = firstNonBlank(
                environment.getProperty("SPRING_DATASOURCE_URL"),
                environment.getProperty("DATABASE_URL"),
                environment.getProperty("spring.datasource.url"));
        if (!StringUtils.hasText(raw)) {
            return;
        }
        raw = raw.trim();
        if (raw.startsWith("jdbc:")) {
            return;
        }
        if (!raw.startsWith("postgresql://") && !raw.startsWith("postgres://")) {
            return;
        }

        String normalized = raw.startsWith("postgres://")
                ? "postgresql://" + raw.substring("postgres://".length())
                : raw;

        URI uri;
        try {
            uri = URI.create(normalized);
        } catch (IllegalArgumentException ex) {
            return;
        }

        String host = uri.getHost();
        if (!StringUtils.hasText(host)) {
            return;
        }

        int port = uri.getPort();
        String path = uri.getPath();
        String db = StringUtils.hasText(path) && path.startsWith("/")
                ? path.substring(1)
                : path;
        if (!StringUtils.hasText(db)) {
            db = "postgres";
        }
        String query = uri.getRawQuery();

        StringBuilder jdbc = new StringBuilder("jdbc:postgresql://").append(host);
        if (port != -1) {
            jdbc.append(':').append(port);
        }
        jdbc.append('/').append(db);
        if (StringUtils.hasText(query)) {
            jdbc.append('?').append(query);
        }

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("spring.datasource.url", jdbc.toString());

        String userInfo = uri.getRawUserInfo();
        if (StringUtils.hasText(userInfo)) {
            int colon = userInfo.indexOf(':');
            String user = colon >= 0 ? userInfo.substring(0, colon) : userInfo;
            String pass = colon >= 0 && colon < userInfo.length() - 1
                    ? userInfo.substring(colon + 1)
                    : "";
            if (!StringUtils.hasText(environment.getProperty("SPRING_DATASOURCE_USERNAME"))) {
                map.put("spring.datasource.username", decode(user));
            }
            if (!StringUtils.hasText(environment.getProperty("SPRING_DATASOURCE_PASSWORD"))) {
                map.put("spring.datasource.password", decode(pass));
            }
        }

        environment.getPropertySources().addFirst(new MapPropertySource(PROPERTY_SOURCE_NAME, map));
    }

    private static String firstNonBlank(String... candidates) {
        if (candidates == null) {
            return null;
        }
        for (String s : candidates) {
            if (StringUtils.hasText(s)) {
                return s;
            }
        }
        return null;
    }

    private static String decode(String s) {
        if (!StringUtils.hasText(s)) {
            return s;
        }
        return URLDecoder.decode(s, StandardCharsets.UTF_8);
    }
}
