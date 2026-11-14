## Role
You are a senior Java test engineer, specialised in integration testing with Spring Boot Test and Testcontainers. You test the running application through real HTTP requests, with a real session and CSRF token, and you check what is stored in the database. You take expected results from the specification, and you read the code only to learn how to call the application.

## Goal
Validate the API and the real persistence in a controlled environment, with integration tests that cover at least:
- **one successful flow:** a project member creates a valid task through the API, then reads it back through the API, following the API contract and the permissions that apply;
- **one negative scenario:** an invalid title, or a resource that doesn't exist.

For each one, check the HTTP status code, the fields returned and what is stored in the database. The tests run against a test database with isolated data, and the state is cleaned or reset between runs. The tests are run and their results recorded.

## Context
- **Project:** Matriz, a project and task management application. The back end uses Java 21, Spring Boot 4, PostgreSQL and Flyway, built with Maven.
- **Sources of the expected results:**
  - **API contract:** `openapi.yaml` and `docs/api-contract.md`, for status codes, fields and paths;
  - **User stories:** `docs/user-stories.md` US05 (create a task), US20 (view a task's details), DoD03 (non-member) and DoD05 (task that doesn't exist), for messages and rules;
  - **Test plan:** `docs/test-plan.md` TP01, TP06, TP11 and TP19, for the scenarios.
  - **Conflicts:** if the contract and the user stories disagree, the user stories win, and the disagreement is reported.
- **Existing test infrastructure (reuse it):**
  - `ApiClient`: real HTTP requests, with the session cookie and CSRF token;
  - `TestUsers` and `ProjectFixtures`: test users and projects;
  - `TestcontainersConfiguration`: PostgreSQL in a container.

  Existing tests such as `TasksApiTest` may be read only to learn how to use this infrastructure, never to copy expected values or assertions.
- **Production code:** may be read only to learn how to call the application, never to decide an expected result.
- **Commands:** `./mvnw test -Dtest=<TestClass> -Dskip.npm -Dskip.installnodenpm` runs one class (Docker must be running); `./mvnw verify` runs the full build.

## Constraints
- **Isolated data:** both of these.
  - Each test creates its own users and project, with unique names, and depends neither on other tests' data nor on the order the tests run in.
  - Each test deletes the data it created when it ends, so the database is back to its previous state.
- **Files:**
  - create only one new test class, add the record of the results to `docs/verification.md`, and add one sentence to section 2 of `docs/test-plan.md` (Task step 7);
  - never change production code or the existing test infrastructure, except for the temporary change in Validation check 5, which is reverted.
- **Names and comments:** each test name says plainly what it checks. No specification or test-plan IDs (such as US05, AC08, D01 or TP04) appear in test names or comments.
- **Ambiguity and conflicts:** when the contract disagrees with the user stories, or something a test needs isn't specified, stop and ask the user.
- **Language:** code and names in English (C05).

## Task
1. **Read the sources.**
   - In the contract: the requests for creating a task and reading its details (path, status code, fields, errors).
   - In the user stories: US05, US20, DoD03 and DoD05.
2. **Write the successful flow.** A project member creates a valid task with every field. Check:
   - the status code and the fields returned;
   - the same task read back through the API;
   - the row stored in the database.
3. **Write the permission case.** A user who isn't a member of the project tries to read the task that was created. Check the status code, the message "Project not found." and that no task data is returned.
4. **Write the negative scenarios.**
   - **Invalid title:** check the status code, the message "Task title must be at least 3 characters." and that no task was stored.
   - **Task that doesn't exist**, both with the identifier of a deleted task and with a randomly generated identifier: check the status code, the message "Task not found." and that nothing changed.
5. **Run the test class and review every failure.**
   - If the test contradicts the sources, correct the test.
   - If the code doesn't meet them, record a defect and leave the code unchanged.
6. **Record the results** in a new section of `docs/verification.md`, with:
   - the command used and the date;
   - the tests, and how many passed and failed;
   - each test that was corrected, and why;
   - each defect found, if any.
7. **Say where HTTP details come from.** In `docs/test-plan.md` section 2, add: "Status codes, paths and field names are left to the integration tests, which take them from the API contract."

## Validation
Before reporting, run these checks and give each one as passed or failed:
1. **Tests run:** the test class runs. Every test passes, or each failing one is explained as a recorded defect.
2. **Full build:** `./mvnw verify` passes.
3. **Sources:** every expected status code and field is in the contract, and every expected message is in the user stories, character for character.
4. **Isolation:**
   - the number of rows in the `project` and `task` tables is the same before and after the test class runs;
   - the class passes when run twice in a row.
5. **The tests check the rule:** make a temporary change to the rule (e.g. the title's minimum of 3 becomes 2) and confirm at least one test fails. Then revert it.
6. **No other change:** `git diff` shows no change under `src/main` or in the existing test infrastructure.
7. **Final report:** follow AGENTS.md §9 (changes, assumptions, checks, not checked), with the results of checks 1 to 6.
