package matriz.accounts.internal;

import java.util.List;
import java.util.UUID;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates the three demo accounts when {@code MATRIZ_DEMO_SEED=true} (FR21, US19).
 *
 * <p>Idempotent: an account whose email already exists is left as it is, so later start-ups
 * neither duplicate nor change data, and nothing is ever deleted. All data is made up (PRI05).
 */
@Component
@Order(1)
class DemoAccountsSeeder implements ApplicationRunner {

    private record DemoAccount(String name, String email, String passwordVariable, String password) {
    }

    private final DemoAccountsProperties properties;
    private final PasswordEncoder passwordEncoder;
    private final JdbcClient jdbc;

    DemoAccountsSeeder(DemoAccountsProperties properties, PasswordEncoder passwordEncoder, JdbcClient jdbc) {
        this.properties = properties;
        this.passwordEncoder = passwordEncoder;
        this.jdbc = jdbc;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!properties.seed()) {
            return;
        }
        List<DemoAccount> accounts = List.of(
                new DemoAccount("Ana Silva", "ana@example.com", "MATRIZ_DEMO_PASSWORD_ANA", properties.passwordAna()),
                new DemoAccount("Rui Costa", "rui@example.com", "MATRIZ_DEMO_PASSWORD_RUI", properties.passwordRui()),
                new DemoAccount("Sara Lopes", "sara@example.com", "MATRIZ_DEMO_PASSWORD_SARA", properties.passwordSara()));
        for (DemoAccount account : accounts) {
            insertIfMissing(account);
        }
    }

    private void insertIfMissing(DemoAccount account) {
        String password = account.password();
        // Only the variable's name is reported, never its value (SEC10).
        if (password == null || password.length() < 8 || password.length() > 128) {
            throw new IllegalStateException("The demo seed is enabled, but " + account.passwordVariable()
                    + " is not set or is not 8 to 128 characters long (US01 password rules).");
        }
        jdbc.sql("""
                insert into users (id, name, email, password_hash)
                values (?, ?, ?, ?)
                on conflict (email) do nothing
                """)
                .params(UUID.randomUUID(), account.name(), account.email(), passwordEncoder.encode(password))
                .update();
    }
}
