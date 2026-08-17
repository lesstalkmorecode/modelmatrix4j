# JUnit module guidance

- Follow the root `AGENTS.md` and the exact delegation.
- Depend only on supported core contracts and JUnit Jupiter; core never depends on this module.
- Do not create a package until an approved current task introduces its first real production type.
- Keep tests deterministic, offline, isolated, and free of mutable global execution state.
- Stop and return to the orchestrator before changing a core contract, public/shared API, dependency, or another module.
- Completion is proven from the repository root with `./mvnw -B verify`.
