package matriz;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.dao.DataAccessException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Checks for increment I0: the migrations create the schema of docs/database.md on an empty
 * database, and the demo seed is idempotent and allows different access rules to be tested.
 * The passwords below are fictitious and exist only in this test.
 */
@SpringBootTest(properties = {
        "matriz.demo.seed=true",
        "matriz.demo.password-ana=test-password-a",
        "matriz.demo.password-rui=test-password-r",
        "matriz.demo.password-sara=test-password-s"})
@Import(TestcontainersConfiguration.class)
class PersistenceSetupTest {

    private static final List<String> COUNTED_TABLES =
            List.of("users", "project", "project_member", "team", "team_member", "task",
                    "task_assignee_user", "task_assignee_team", "task_comment");

    @Autowired
    JdbcClient jdbc;

    @Autowired
    List<ApplicationRunner> runners;

    @Test
    void migrationsCreateTheTablesOfTheDataModel() {
        List<String> tables = jdbc.sql("""
                select table_name from information_schema.tables
                where table_schema = 'public' and table_type = 'BASE TABLE'
                """).query(String.class).list();

        assertThat(tables).contains(
                "users", "spring_session", "spring_session_attributes",   // Accounts
                "project", "project_member",                              // Projects
                "team", "team_member",                                    // Teams
                "task", "task_assignee_user", "task_assignee_team", "task_comment"); // Tasks
    }

    @Test
    void seedRunTwiceDoesNotChangeTheData() throws Exception {
        Map<String, Integer> before = rowCounts();

        ApplicationArguments args = new DefaultApplicationArguments();
        for (ApplicationRunner runner : runners) {
            runner.run(args);
        }

        assertThat(rowCounts()).isEqualTo(before);
        assertThat(before.get("users")).isEqualTo(3);
    }

    @Test
    void demoDataHasAManagerAMemberAndANonMemberOfTheSameProject() {
        String manager = jdbc.sql("""
                select u.name from project p join users u on u.id = p.manager_id
                where p.name = 'Website'
                """).query(String.class).single();
        List<String> members = jdbc.sql("""
                select u.name from project_member m
                join project p on p.id = m.project_id
                join users u on u.id = m.user_id
                where p.name = 'Website'
                """).query(String.class).list();

        assertThat(manager).isEqualTo("Ana Silva");
        assertThat(members).containsExactlyInAnyOrder("Ana Silva", "Rui Costa");
        assertThat(members).doesNotContain("Sara Lopes");
    }

    @Test
    void demoPasswordsAreNotStoredInReadableForm() {
        List<String> hashes = jdbc.sql("select password_hash from users").query(String.class).list();

        assertThat(hashes).allSatisfy(hash -> {
            assertThat(hash).startsWith("{bcrypt}");
            assertThat(hash).doesNotContain("test-password");
        });
    }

    @Test
    void theTaskTableEnforcesTheTaskLimits() {
        // Task limits decided on 2026-11-11 (US05, V4_1): title 3–150, description up to 2000, and a new task
        // starts in the status value "TODO" ("To do" in the interface).
        UUID website = UUID.fromString("5b0c6f0e-2f4a-4c47-9f39-1b2f7f6f9a10");
        UUID valid = UUID.randomUUID();
        jdbc.sql("insert into task (id, project_id, title, description) values (?, ?, ?, ?)")
                .params(valid, website, "W".repeat(150), "d".repeat(2000))
                .update();

        assertThat(jdbc.sql("select status from task where id = ?").param(valid).query(String.class).single())
                .isEqualTo("TODO");
        for (List<String> invalid : List.of(
                List.of("Wd", "description"),
                List.of("W".repeat(151), "description"),
                List.of("Valid title", "d".repeat(2001)))) {
            assertThatThrownBy(() -> jdbc.sql("insert into task (id, project_id, title, description) values (?, ?, ?, ?)")
                    .params(UUID.randomUUID(), website, invalid.get(0), invalid.get(1))
                    .update())
                    .isInstanceOf(DataAccessException.class);
        }
        assertThatThrownBy(() -> jdbc.sql("update task set status = 'TO_DO' where id = ?").param(valid).update())
                .isInstanceOf(DataAccessException.class);

        jdbc.sql("delete from task where id = ?").param(valid).update();
    }

    private Map<String, Integer> rowCounts() {
        return COUNTED_TABLES.stream().collect(java.util.stream.Collectors.toMap(
                table -> table,
                table -> jdbc.sql("select count(*) from " + table).query(Integer.class).single()));
    }
}
