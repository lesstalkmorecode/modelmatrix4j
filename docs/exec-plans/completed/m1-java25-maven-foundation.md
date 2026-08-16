# M1 — Java 25 / Maven foundation

Status: completed after executable quality-gate correction.

## Scope

Establish a reproducible Java 25 Maven reactor with only `modelmatrix-core` and `modelmatrix-junit`. M1 does not implement execution contracts, providers, Spring AI, RAG, MCP, reporting, or external-service infrastructure.

## Corrective review decisions

- The parent POM enforces Java 25 and dependency convergence only.
- The core POM owns the prohibited-dependency rule for Spring, Spring AI, MCP, PostgreSQL, and provider SDKs. Optional adapter modules will inherit the repository-wide rules without inheriting the core-only ban.
- Stable GA versions are pinned to JUnit 6.1.2, maven-compiler-plugin 3.15.0, maven-enforcer-plugin 3.6.3, and maven-surefire-plugin 3.5.5. Maven Wrapper scripts remain version 3.3.4 and the configured Maven distribution is 3.9.16.
- Checkstyle and its repository-specific configuration were removed because M1 has insufficient production code to justify that quality gate. No replacement formatter or static-analysis tool was added.
- The unused Antrun plugin and its version property were removed.
- Placeholder production classes were removed. Core retains only a test-scoped JUnit smoke test proving Java 25 test execution; the JUnit module retains one smoke test proving that JUnit Jupiter execution is wired into the Maven lifecycle. Core boundary protection is verified by its Enforcer rule and dependency inspection.
- The parent description and repository URL match the project specification and the configured Git origin.
- The default GitHub Actions workflow runs the same Wrapper verification on push and pull request with Java 25 on Ubuntu.

## Executable Quality Gates

- Java 25 compiler baseline → GATE-M1-01.
- Dependency convergence → GATE-M1-02.
- Framework/provider dependency boundary for core → GATE-M1-03.
- Inward module dependency direction → GATE-M1-04.
- Default verification isolation → GATE-M1-05.
- Core and JUnit test lifecycle execution → GATE-M1-06.
- CI/local verification parity → GATE-M1-07.
- M1 reactor module scope → GATE-M1-08.

The core boundary was verified structurally and by the passing core Enforcer execution: `bannedDependencies` appears in `modelmatrix-core/pom.xml` only, while the parent POM contains only the repository-wide Java and convergence rules. A prohibited dependency was not left in the repository; the final core dependency tree contains only test-scoped JUnit dependencies.

`AUTOMATED` is used only where the current Maven or workflow configuration executes a failing check; default-runtime isolation and exact M1 module enumeration remain explicitly manual gates.

## Verification evidence

- `.\mvnw.cmd -B verify`: passed on 2026-08-16 with Maven 3.9.16 and Temurin Java 25. The Windows wrapper launcher now handles a normal non-symlink `.m2` directory while retaining wrapper version 3.3.4.
- Reactor modules: `modelmatrix-core` and `modelmatrix-junit` only.
- Java compiler log: `javac [debug deprecation release 25]`.
- Core JUnit result: 1 test run, 0 failures, 0 errors.
- JUnit module result: 1 test run, 0 failures, 0 errors.
- `.\mvnw.cmd -B -pl modelmatrix-core dependency:tree`: passed; core has no production dependencies and only test-scoped JUnit API/engine dependencies.
- `.\mvnw.cmd -B -pl modelmatrix-junit dependency:tree`: passed; JUnit depends inward on core and JUnit Jupiter, with no reverse dependency.
- Core Enforcer execution: `BannedDependencies passed`.
- Negative boundary verification: a temporary `org.springframework:spring-core:6.2.11` dependency caused core validation to fail with `BannedDependencies`; the fixture was removed before the final build.
- Wrapper metadata: version 3.3.4 with Maven distribution 3.9.16.
- `git ls-files -s mvnw`: mode `100755`; wrapper scripts and metadata are not ignored; `target/` and IDE directories remain ignored.
- `.github/workflows/ci.yml` exists with push/pull-request triggers, Ubuntu, read-only contents permission, Java 25 Temurin setup, and `./mvnw -B verify`.
- `docs/QUALITY_GATES.md` exists and maps M1 acceptance criteria to automated, manual, and deferred gates; GATE-M1-05 records that artifact repository access is allowed while external runtime services and credentials are not required.
- No Spring AI, RAG, MCP, Docker, Ollama, or provider implementation exists in the M1 source tree, and no such service is required by default verification.
