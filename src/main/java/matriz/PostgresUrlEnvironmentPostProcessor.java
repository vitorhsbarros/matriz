package matriz;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.EnvironmentPostProcessor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.util.StringUtils;

/**
 * Lets {@code MATRIZ_DB_URL} also be a PostgreSQL connection string ({@code postgresql://user:password@host:port/database}),
 * the form that hosting platforms such as Render give, besides a JDBC URL. The string is turned into the JDBC URL the
 * application uses, without the credentials. The user and password in the string are used only when
 * {@code MATRIZ_DB_USER} and {@code MATRIZ_DB_PASSWORD} are not set.
 *
 * <p>Runs after the configuration, {@code .env} included, has been read. A JDBC URL is left as it is.
 */
public class PostgresUrlEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

    static final String URL = "spring.datasource.url";
    static final String USERNAME = "spring.datasource.username";
    static final String PASSWORD = "spring.datasource.password";
    static final String SOURCE = "matrizPostgresConnectionString";

    private static final int DEFAULT_PORT = 5432;

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String url = environment.getProperty(URL);
        if (url == null || !(url.startsWith("postgresql://") || url.startsWith("postgres://"))) {
            return;
        }
        Map<String, Object> properties = new HashMap<>();
        URI uri = parse(url);
        properties.put(URL, jdbcUrl(uri));
        String[] credentials = credentials(uri);
        if (!StringUtils.hasText(environment.getProperty(USERNAME)) && credentials.length > 0) {
            properties.put(USERNAME, credentials[0]);
        }
        if (!StringUtils.hasText(environment.getProperty(PASSWORD)) && credentials.length > 1) {
            properties.put(PASSWORD, credentials[1]);
        }
        environment.getPropertySources().addFirst(new MapPropertySource(SOURCE, properties));
    }

    @Override
    public int getOrder() {
        return ConfigDataEnvironmentPostProcessor.ORDER + 1;
    }

    /** The string may hold a password, so an error names the setting but never shows its value. */
    private static URI parse(String url) {
        try {
            URI uri = new URI(url);
            if (!StringUtils.hasText(uri.getHost()) || uri.getPath() == null || uri.getPath().length() < 2) {
                throw invalid();
            }
            return uri;
        } catch (URISyntaxException e) {
            throw invalid();
        }
    }

    private static String jdbcUrl(URI uri) {
        int port = uri.getPort() == -1 ? DEFAULT_PORT : uri.getPort();
        String query = uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery();
        return "jdbc:postgresql://" + uri.getHost() + ":" + port + uri.getRawPath() + query;
    }

    private static String[] credentials(URI uri) {
        String userInfo = uri.getRawUserInfo();
        if (!StringUtils.hasText(userInfo)) {
            return new String[0];
        }
        String[] parts = userInfo.split(":", 2);
        for (int i = 0; i < parts.length; i++) {
            parts[i] = URLDecoder.decode(parts[i], StandardCharsets.UTF_8);
        }
        return parts;
    }

    private static IllegalStateException invalid() {
        return new IllegalStateException("MATRIZ_DB_URL is not a valid JDBC URL or PostgreSQL connection string "
                + "(postgresql://user:password@host:port/database). Its value is not shown because it may contain a password.");
    }
}
