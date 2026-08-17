# ModelMatrix4J repository guidance

## Scope

- `docs/ROADMAP.md` authorizes product milestones. A user's explicit task may authorize repository maintenance, but it does not authorize product behavior.
- Before editing a module, explicitly read that module's nearest AGENTS.md.
- Change only the exact files or bounded paths in the delegation. Do not cross module boundaries, add dependencies, or change public/shared APIs without returning to the orchestrator for a revised task.
- Do not implement a later milestone, invent future packages, or create empty packages and placeholders. Create a package only with its first required production type.

## Dependencies and review

For authorized delegated work, follow `.agents/skills/modelmatrix-milestone/SKILL.md`, the single workflow source of truth.

A task starts only after its named prerequisites are finished and reconciled. Writers have exclusive scope. Reviewers are read-only, use different actual agents from the implementer, and review the integrated diff.

## Boundaries

- `modelmatrix-core` is JDK-only, provider/framework-neutral, and has no production JUnit dependency.
- `modelmatrix-junit` depends inward on supported core contracts and JUnit Jupiter; core never depends on it.
- Default tests are deterministic and offline. External services, credentials, real models, databases, MCP, and cloud providers are opt-in only when their milestone authorizes them.
- `docs/PRODUCT_SPEC.md`, `docs/ARCHITECTURE.md`, and `docs/ROADMAP.md` define product scope and module direction.

## Completion

The one canonical verification command, run from the repository root, is:

```text
./mvnw -B verify
```

Before handoff, also inspect the full diff and run `git diff --check`. Do not commit or push unless the user explicitly asks.
