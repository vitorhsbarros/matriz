package matriz.projects.internal;

import java.util.UUID;
import matriz.accounts.AccountDirectory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates the demo projects and memberships when {@code MATRIZ_DEMO_SEED=true}, after the demo
 * accounts. They let different access rules be tested:
 *
 * <ul>
 *   <li><b>Website:</b> Ana is the manager, Rui is a member, Sara is not a member;</li>
 *   <li><b>App:</b> Rui is the manager, Ana is a member, Sara is not a member.</li>
 * </ul>
 *
 * <p>The projects have fixed identifiers, so the seed is idempotent: existing rows are left as
 * they are, and nothing is deleted. Users are found through the Accounts public interface.
 */
@Component
@Order(2)
class DemoProjectsSeeder implements ApplicationRunner {

    static final UUID WEBSITE = UUID.fromString("5b0c6f0e-2f4a-4c47-9f39-1b2f7f6f9a10");
    static final UUID APP = UUID.fromString("9a1b2c3d-4e5f-4a6b-8c7d-9e0f1a2b3c4d");

    private final boolean seed;
    private final AccountDirectory accounts;
    private final JdbcClient jdbc;

    DemoProjectsSeeder(@Value("${matriz.demo.seed:false}") boolean seed, AccountDirectory accounts, JdbcClient jdbc) {
        this.seed = seed;
        this.accounts = accounts;
        this.jdbc = jdbc;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!seed) {
            return;
        }
        UUID ana = user("ana@example.com");
        UUID rui = user("rui@example.com");

        project(WEBSITE, "Website", "New company website", ana);
        member(WEBSITE, ana);
        member(WEBSITE, rui);

        project(APP, "App", "Mobile app for customers", rui);
        member(APP, rui);
        member(APP, ana);
    }

    private UUID user(String email) {
        return accounts.findUserIdByEmail(email)
                .orElseThrow(() -> new IllegalStateException("Demo account missing: " + email));
    }

    private void project(UUID id, String name, String description, UUID manager) {
        jdbc.sql("""
                insert into project (id, name, description, manager_id)
                values (?, ?, ?, ?)
                on conflict (id) do nothing
                """)
                .params(id, name, description, manager)
                .update();
    }

    private void member(UUID project, UUID user) {
        jdbc.sql("""
                insert into project_member (project_id, user_id)
                values (?, ?)
                on conflict do nothing
                """)
                .params(project, user)
                .update();
    }
}
