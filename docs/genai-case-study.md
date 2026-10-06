# AI-Assisted Development — Case Study: a Task Management API

A worked example, separate from the Pokémon project (requirements AI-1…AI-5). It covers the prompt
used to generate a RESTful API for a simple task management system with an AI coding tool, a
representative sample of the output, and how that output was validated, corrected and improved,
including edge cases, authentication and validation.

**Functional scope:** CRUD on tasks. A task has `title`, `description`, `status`
and `due_date`. Tasks belong to a user (assume a basic `User` model exists).

> Status: ⬜ to be produced in plan Phase 7. The sections below are the structure. Fill them with
> real output, never with invented results. If generated code is kept in the repo, it goes in
> `genai-case-study/` (an isolated module) so it can't be confused with the Pokémon project's code.

---

## 1. Tool and approach

- **Tool:** Claude Code (agentic, in the repository), the same tool used for the Pokémon project. Any
  context file it was given (an `AGENTS.md`-style spec) is shown verbatim.
- **Approach:** Spec-first prompting. Constraints, contracts and acceptance criteria go *in* the
  prompt, so the output can be checked against them instead of judged by eye. Generation is
  iterative: scaffold → review → targeted follow-up prompts.

## 2. The prompt (AI-1)

> Write the final prompt here **verbatim**. It should include:
> - Stack and versions (Java 25, Spring Boot 4, PostgreSQL, Flyway).
> - Architecture constraints (Clean Architecture, framework-free use cases, ports & adapters).
> - The data model: `Task(id, title, description, status, dueDate, ownerId)`, a `TaskStatus` enum
>   (`TODO`, `IN_PROGRESS`, `DONE`), and `User` assumed to exist.
> - The JSON field naming: the scope says `due_date`, while Java fields are `dueDate`. The prompt
>   states which name goes on the wire and why (for example camelCase to match the Pokémon API,
>   or `due_date` to match the scope literally), so the output can be checked against it.
> - The API contract: endpoints, verbs, status codes, error shape, pagination/filtering by status.
> - Authorization rules: a user only sees and changes **their own** tasks. Another user's task is a
>   404, not a 403, so ids can't be enumerated.
> - Validation rules: title required and ≤ 200 chars, description ≤ 2000, a valid status, `dueDate`
>   not in the past on create (the clock is injected), and status transitions if any.
> - Test expectations (TDD; unit tests per layer + controller tests) and what *not* to do (no
>   `@Autowired` fields, no entity in responses, no `ddl-auto: update`).
> - The output format: file tree first, then files.

## 3. Output (AI-2)

> A representative sample: the file tree, plus 2–3 key files (use case, controller, a test). Paste
> unedited, and mark it **"raw AI output"**.

## 4. How the output was validated (AI-3)

Checklist applied to the raw output (✅ / ❌ with notes):

- [ ] Compiles and the tests pass (`./gradlew check`), run, not assumed.
- [ ] Dependency Rule: no framework imports in domain/use cases (ArchUnit run on it).
- [ ] Every endpoint returns the documented statuses (400/401/404/409), checked with tests or `curl`.
- [ ] Ownership enforced on **every** operation, including update and delete, not just reads.
- [ ] Validation exists server-side, not only as annotations that are never triggered (`@Valid`
      actually present on the parameter).
- [ ] No entity or password hash leaked in responses. No sensitive data in logs.
- [ ] Time handling is deterministic (an injected `Clock`), and time zones are explicit for `due_date`.
- [ ] Pagination is bounded (a max page size).
- [ ] No invented APIs: every library/annotation used exists in the declared versions (checked
      against docs or the dependency tree).

## 5. Corrections and improvements (AI-4)

> For each defect found: what was wrong → why it matters → the fix (before/after snippet) → how
> the fix was verified. Real examples only.

## 6. Edge cases, authentication, validation (AI-5)

> - Edge cases: a past due date, unknown status values, an empty title after trimming, concurrent
>   updates (`@Version` → 409), a large description, deleting a missing task.
> - Authentication: how the user identity reaches the use case (from the token at the edge, never
>   from the request body).
> - Authorization: ownership checks and the 404-vs-403 choice.
> - Validation layering: syntactic (Bean Validation at the edge) vs semantic (domain invariants).

## 7. Takeaways

> 3–5 bullets on prompt engineering lessons: what context improved the output most, where the AI
> was confidently wrong, and how you'd structure the prompt differently next time.
