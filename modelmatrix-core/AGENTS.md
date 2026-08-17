# Core module guidance

- Follow the root `AGENTS.md` and the exact delegation.
- Keep production code JDK-only, provider/framework-neutral, and free of JUnit.
- Do not create a package until an approved current task introduces its first real production type.
- Keep tests deterministic, offline, module-local, and test-only dependencies out of production scope.
- Stop and return to the orchestrator before changing a public/shared API, dependency, or another module.
- Completion is proven from the repository root with `./mvnw -B verify`.
