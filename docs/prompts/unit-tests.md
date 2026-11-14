## Role
You are a senior Java test engineer, specialised in JUnit 5 and in parameterised boundary-value tests. You write tests from the test plan and the specification. You read the code only to find which unit to call, never to decide what the expected result is.

## Goal
Write and run unit tests for one business rule that is already implemented: **the validation of a task's title**, which corresponds to scenarios TP03 and TP04 in `docs/test-plan.md`. The result is:
- unit tests, written with the framework already in the project, that cover at least a valid case, an invalid case and a boundary value;
- assertions that check the requirements, not what the code happens to do;
- the tests run, and their results recorded.

Incoherent tests are corrected. The rule itself is never changed just to make the tests pass. If a test shows that the code doesn't meet the specification, that is a defect to report.

## Context
- **Project:** Matriz, a project and task management application. The back end uses Java 21 and Spring Boot 4, built with Maven.
- **Sources of the expected results:** only these.
  - `docs/test-plan.md`: scenarios TP03 (missing title) and TP04 (length limits, counted after trimming spaces);
  - `docs/user-stories.md`: US05 Rules, AC03 and AC08, and the decisions D01 (leading and trailing spaces are removed; a field with only spaces counts as empty) and D02 (limits count characters after D01).
  - What these fix:
    - **Title:** required, between 3 and 150 characters.
    - **Messages:** "Task title is required.", "Task title must be at least 3 characters." and "Task title must be at most 150 characters.".
- **Production code:** may be read only to find the unit that validates the title, meaning which class and method to call. It never decides an expected result.
- **Existing tests:** may be read only to follow the project's conventions (packages, assertion library, naming), never to take expected values from them.
- **Commands:** run a single test class with `./mvnw test -Dtest=<TestClass> -Dskip.npm -Dskip.installnodenpm`.

## Constraints
- **Unit level only:** each test calls the class that holds the rule directly, with no Spring context, database or HTTP. Integration tests come later and are out of scope here.
  - If the rule can't be called on its own without one of these, stop and ask.
- **Files:**
  - create only the new test class and update the record of the results;
  - never change production code, except for the temporary change in Validation check 4, which is reverted.
- **Test names:**
  - each name says clearly what the test checks, in terms any developer understands, e.g. `aTitleWithOnlySpacesIsRequired`;
  - no references to plan IDs such as TP03 or TP04.
- **Boundary values:** use parameterised tests (`@ParameterizedTest`) where several values check the same rule, e.g. 2, 3, 150 and 151 characters.
- **Ambiguity:** when the specification is silent or ambiguous, stop and ask the user.
- **Language:** code and names in English (C05).

## Task
1. **Find the unit.** Find, in the production code, the class that validates a task's title, and confirm it can be called on its own. If it can't, stop and ask.
2. **Write the tests.** Write one JUnit 5 test class, with every assertion derived from the specification:
   - **valid cases:** a title of 3 characters, a title of 150 characters, and a title with spaces around it, which is kept without them;
   - **invalid cases:** "" and "   " give "Task title is required.";
   - **boundary values:** 2 characters gives "Task title must be at least 3 characters." and 151 characters gives "Task title must be at most 150 characters."; 3 and 150 are accepted; "  ab  " gives the minimum-length message.
3. **Run the test class.**
4. **Review every failing test.**
   - If the test contradicts the specification, correct the test.
   - If the code doesn't meet the specification, record it as a defect and leave the rule unchanged.
5. **Record the results** in a new section of `docs/verification.md`, with:
   - the command used and the date;
   - the number of tests, and how many passed and failed;
   - each test that was corrected, and why;
   - each defect found, if any.

## Validation
Before reporting, run these checks and give each one as passed or failed:
1. **Tests run:** the test class runs. Every test passes, or each failing one is explained as a recorded defect.
2. **Full build:** `./mvnw verify` passes.
3. **Expected values:** each expected message and limit in the tests matches US05 and D01/D02 character for character.
4. **The tests check the rule:** make a temporary change to the rule (e.g. the maximum of 150 becomes 149) and confirm at least one test fails. Then revert it.
5. **No production change:** after the revert, `git diff` shows no change under `src/main`.
6. **Final report:** follow AGENTS.md §9 (changes, assumptions, checks, not checked), with the results of checks 1 to 5.
