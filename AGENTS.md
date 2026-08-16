# ModelMatrix4J repository guidance

## Navigation

- `docs/PRODUCT_SPEC.md` defines the product boundary and MVP.
- `docs/ARCHITECTURE.md` defines module and dependency decisions.
- `docs/DOMAIN_MODEL.md` defines the minimum domain vocabulary.
- `docs/TEST_STRATEGY.md` defines test layers and provider isolation.
- `docs/ROADMAP.md` defines milestone scope and acceptance criteria.
- `docs/QUALITY_GATES.md` maps approved architectural rules to executable, manual, or deferred enforcement.
- `docs/adr/` contains durable architecture decisions.
- `docs/exec-plans/` contains temporary implementation plans; completed plans move to `completed/`.

## Engineering rules

1. Implementation proceeds only through the currently approved roadmap milestone; functionality from later milestones must not be implemented prematurely.
2. Keep `modelmatrix-core` framework-free and provider-neutral. It must not depend on Spring, Spring Boot, Spring AI, MCP, PostgreSQL, or provider SDKs.
3. JUnit Jupiter is an integration layer, not a dependency of core. Optional integrations must never be required for the default build.
4. Prefer a small number of stable value objects and ports over speculative interfaces. New public API requires a documented use case and tests.
5. Deterministic tests must not call real models. Real-model, database, MCP, and cloud tests belong in explicitly opt-in integration-test source sets/profiles.
6. Keep architectural decisions in `docs/` and record consequential reversals as ADRs. Keep this file concise and navigational.
7. Dependencies must have licenses suitable for the intended use of the project and must be reviewed before public distribution. Keep builds reproducible and use semantic versioning. Do not add infrastructure that the product specification does not justify.
