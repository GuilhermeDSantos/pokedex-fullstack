# AI-Assisted Development — Case Study: a Task Management API

A worked example, separate from the Pokémon project (requirements AI-1…AI-5): a RESTful API for a
simple task management system, generated with an AI coding agent, then validated and corrected.

**Functional scope:** CRUD on tasks. A task has `title`, `description`, `status` and `due_date`, and
belongs to a user (a basic `User` model is assumed to exist).

The code is in [`genai-case-study/task-api/`](../genai-case-study/task-api/), a module isolated
from the Pokémon build.

---

## 1. Tool and approach

- **Tool:** Claude Code, the agent that also wrote most of the Pokémon project, working in this
  repository.
- **How I work with it:** the agent never starts from a bare prompt. It works under my rules: the
  repository's [`AGENTS.md`](../AGENTS.md) and [`docs/standards/backend.md`](standards/backend.md)
  (strict Clean Architecture, TDD one behaviour at a time, verify external facts, ask before adding
  a dependency, one commit per red/green/refactor step). The prompt then specifies the task itself.
- **Spec-first prompting:** constraints, the API contract and acceptance criteria go *in* the
  prompt, so the output can be checked against them instead of judged by eye.
- **A first attempt that I stopped.** The agent's first run took only the prompt, in an isolated
  context without my rules. It produced a plausible layout, but with conventions that weren't mine
  (flat packages, `*Command`/`*View` names) and no tests yet. I stopped it: on a real job I would
  never run an agent blind, so the case study should show how I actually work. The second run used
  the same prompt under the project's rules, test-first.

## 2. The prompt (AI-1)

Verbatim in [`genai-case-study/PROMPT.md`](../genai-case-study/PROMPT.md). Its sections:

- **Stack and versions:** Java 25, Spring Boot 4.1, PostgreSQL 17, Flyway, Testcontainers, ArchUnit;
  no Lombok.
- **Architecture:** four packages (`domain`, `application`, `infrastructure`, `interfaces`),
  framework-free use cases behind input ports, a `UnitOfWork` port, an ArchUnit test.
- **Model:** `Task` with a `version`; `TaskStatus` `TODO`/`IN_PROGRESS`/`DONE`, any transition
  allowed; users seeded by Flyway.
- **Naming on the wire:** JSON is camelCase (`dueDate`) like the Pokémon API; the column is
  `due_date`.
- **Authentication and authorization:** HTTP Basic; the owner always comes from the signed-in user;
  another user's task is a **404**, exactly like a missing one, so ids can't be probed.
- **The contract:** endpoints, statuses (400 malformed and invalid, 401, 404, 409 for a stale
  `version`), one error shape, bounded paging (`size` 1–100).
- **Validation:** title 1–200 trimmed, description ≤ 2000 (blank = none), a due date not in the past
  when given or changed, "today" from an injected `Clock` in UTC; the domain owns the rules, the
  edge checks shape only.
- **Tests and don'ts:** per layer, every error status, no entities in responses, no
  `ddl-auto` other than `validate`, no `Optional.get()`, no swallowed exceptions.
- **Output format:** file tree, every file, run instructions, assumptions.

## 3. Output (AI-2)

53 main classes and 16 test classes; the commit history shows each step (`test:` red, `feat:` green).

```
com.example.tasks
├── domain        model (Task, Title, Description, TaskId, UserId, TaskStatus) · repository (TaskRepository)
│                 exception (ValidationException, NotFoundException, ConflictException, ...) · pagination
├── application   usecase (5 × UseCase + Interactor) · dto · mapper (TaskMapper) · port (UnitOfWork)
├── infrastructure persistence (entity, Spring Data, adapter, mapper) · security (HTTP Basic, user lookup)
│                 transaction (SpringUnitOfWork) · config (UseCaseConfig: the composition root)
└── interfaces    rest (TaskController, requests/responses, GlobalExceptionHandler, JSON 401 entry point)
```

Representative files:

- [`Task.java`](../genai-case-study/task-api/src/main/java/com/example/tasks/domain/model/Task.java):
  the aggregate. `create` defaults the status and rejects a past due date; `update` refuses a stale
  version, requires a status, and checks a due date only when it changes.
- [`TaskRepository.java`](../genai-case-study/task-api/src/main/java/com/example/tasks/domain/repository/TaskRepository.java):
  every lookup takes an owner, so no use case can read a task without saying whose it is.
- [`UpdateTaskInteractor.java`](../genai-case-study/task-api/src/main/java/com/example/tasks/application/usecase/UpdateTaskInteractor.java):
  input mapped before the transaction; load by owner, update, save inside it.
- [`TaskController.java`](../genai-case-study/task-api/src/main/java/com/example/tasks/interfaces/rest/controller/TaskController.java):
  the edge, the only place that reads the clock, mints ids and knows who is signed in.
- [`TaskOwnershipIT.java`](../genai-case-study/task-api/src/test/java/com/example/tasks/TaskOwnershipIT.java):
  the whole application with real HTTP Basic and PostgreSQL: Bob gets 404 on Alice's task in
  every operation.

## 4. How the output was validated (AI-3)

| Check | Result | Evidence |
|---|---|---|
| Compiles and the tests pass, run, not assumed | ✅ | `./gradlew check`: 34 unit tests, 19 integration tests (Testcontainers), all green |
| Dependency Rule: no framework in domain or use cases | ✅ | the main project's 17 ArchUnit rules, copied unchanged, pass |
| Every endpoint returns the documented statuses | ✅ | `TaskControllerIT` (13 cases) and `curl` against the running app (below) |
| Ownership enforced on **every** operation | ✅ | `TaskOwnershipIT`: read, update, delete and list as Bob |
| Validation really runs (`@Valid` present; domain rules thrown) | ✅ | a 201-character title → `fieldErrors[title]`; a past due date → 400 from the domain |
| No entity or password hash in responses; nothing sensitive logged | ✅ | response records only; only unexpected errors are logged, without request bodies |
| Deterministic time; explicit time zone | ✅ / ⚠️ | an injected `Clock` (fixed in tests); "today" is UTC, see §6 |
| Bounded pagination | ✅ | `size=500` → 400 |
| No invented APIs for Boot 4 | ✅ | dependencies taken from the main project's build, already verified against Boot 4.1.1; the one new test class (`AutoConfigureMockMvc`) looked up in the resolved 4.1.1 jar, not guessed |

`curl` against the running application (`./gradlew bootRun`, PostgreSQL from `docker-compose.yml`):

| Request | Status |
|---|---|
| No credentials | 401, `UNAUTHENTICATED` |
| Alice creates `"  Buy milk "` with a blank description | 201 + `Location`; title trimmed, description `null` |
| Bob reads, updates or deletes it | 404 each time |
| Due date in the past · status `BLOCKED` · broken JSON | 400 each time |
| Update with `version: 0`, then again with `version: 0` | 200 (`version` becomes 1), then 409 |
| `size=500` · id `42` | 400 · 400 |
| Alice deletes it | 204 |

## 5. Corrections and improvements (AI-4)

Real defects, each caught before the code went green:

1. **The blind first run** (§1) didn't follow the project's architecture conventions. *Fix:* the
   developer redirected the approach; the second run worked under the project's rules.
2. **A mock that would have lied.** The interactor tests stubbed `findByIdAndOwner`, but the
   interactors call the port's *default* method `getByIdAndOwner`. Mockito doesn't run default
   methods on a mock, so it would have returned `null`.
   *Fix:* stub `getByIdAndOwner` itself, making it throw `TaskNotFoundException` for the "not yours"
   cases. The red step was redone before any green.
3. **Catching `NullPointerException`** to turn a missing id into a 400 hid the intent.
   *Before:* `catch (IllegalArgumentException | NullPointerException e)`.
   *After:* an explicit `if (raw == null)` check, then `catch (IllegalArgumentException e)` for
   `UUID.fromString`.
4. **`orElseThrow()` with no argument in a test helper**, the same thing as `Optional.get()`, which
   the rules forbid even in tests. *Fix:* use the port's `getByIdAndOwner`.
5. **Port clash at runtime:** `bootRun` failed because 8080 was taken by the main project. Found
   only by running it. *Fix:* the module runs on 8081 (and its PostgreSQL on 5434).

## 6. Edge cases, authentication, validation (AI-5)

- **Edge cases:**
  - an empty title after trimming is a 400;
  - a blank description becomes "none";
  - a due date of *today* is accepted, yesterday isn't;
  - a task that became overdue can still be edited, because only a **changed** due date is checked;
  - `PUT` replaces all four fields, so omitting `dueDate` clears it (seen with `curl`; the client
    must send what it wants to keep);
  - two edits from the same starting point: the second is a 409, because the version it sends is
    stale (tested). A race between load and save would also be a 409 through `@Version` in the
    adapter, but this module has no test for that path; the main project has one;
  - an id that isn't a UUID is a 400.
- **Time zone:** "today" is UTC, by design and stated in the prompt. At 22:00 in São Paulo it's
  already tomorrow in UTC, so a user there can't set "today" as a due date. The fix, if needed, is
  for the client to send its time zone; it's left as a documented limitation.
- **Authentication:** HTTP Basic against BCrypt hashes in `users`. The user lookup names the
  principal by the user's id, so the controller turns `Principal.getName()` into a `UserId` without
  knowing any security class. A missing or wrong password reads the same (401, the API's error
  shape).
- **Authorization:** every repository lookup is scoped to the owner, and "not yours" is the same 404
  as "doesn't exist", so ids can't be probed.
- **Validation layering:** the edge checks only shape (sizes, a required `version`, parseable JSON),
  with limits taken from the domain's constants; every rule lives in the domain (`Title`,
  `Description`, `Task`, `PageRequest`), which is the single authority.

## 7. Takeaways

- **Context beats prompt length.** The same prompt produced code that fits the project only once
  the agent worked under the project's rules. A context file is part of prompt engineering.
- **Spec-first makes review mechanical.** Statuses, limits and ownership written in the prompt
  became tests and `curl` checks with a yes/no answer.
- **Test doubles can lie.** The default-method trap passes review by eye; only running the red
  test showed it.
- **Run it.** The port clash and the "PUT clears the due date" behaviour only showed up against the
  running application.
- **Make the AI's limits explicit.** Asking it to verify Boot 4 facts instead of guessing, and to
  list its assumptions, turns silent errors into reviewable ones.

## Running it

```bash
cd genai-case-study/task-api
docker compose up -d          # PostgreSQL on localhost:5434
./gradlew bootRun             # API on http://localhost:8081
./gradlew check               # unit tests, ArchUnit, integration tests (needs Docker)

curl -u alice:alice-pass-1 -H 'Content-Type: application/json' \
     -d '{"title":"Buy milk","dueDate":"2099-01-01"}' localhost:8081/api/v1/tasks
```

Seeded users: `alice` / `alice-pass-1` and `bob` / `bob-pass-1`.
