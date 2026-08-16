# ModelMatrix4J quality gates

This document maps existing architectural rules to the smallest current enforcement. It does not duplicate the architecture or introduce future-milestone implementation.

## M1 gates

| ID | Architectural rule | Enforcement mechanism | Command | Introduced | Status |
| --- | --- | --- | --- | --- | --- |
| GATE-M1-01 | The project builds only on Java 25. | Maven Enforcer `requireJavaVersion [25,26)`. | `./mvnw -B verify` | M1 | AUTOMATED |
| GATE-M1-02 | The reactor has no conflicting dependency versions. | Maven Enforcer `dependencyConvergence` in the parent POM. | `./mvnw -B verify` | M1 | AUTOMATED |
| GATE-M1-03 | `modelmatrix-core` excludes Spring, Spring AI, MCP, PostgreSQL, and known provider SDKs. | Core-only Maven Enforcer `bannedDependencies`; the parent does not inherit this rule. | `./mvnw -B verify` | M1 | AUTOMATED |
| GATE-M1-04 | JUnit integration may depend on core; core must not depend on JUnit integration. | Maven parent/module/dependency structure; no artificial production API. | `./mvnw -B -pl modelmatrix-junit dependency:tree` | M1 | AUTOMATED |
| GATE-M1-05 | Default verification requires no external runtime service or credential. | CI runs only the deterministic Maven Wrapper verification. Maven artifact resolution may access configured repositories; absence of runtime-service use is reviewed manually. | `./mvnw -B verify` | M1 | MANUAL |
| GATE-M1-06 | Both current modules execute their test lifecycle correctly. | Maven Surefire with JUnit Jupiter; core JUnit dependencies are test-scoped. | `./mvnw -B verify` | M1 | AUTOMATED |
| GATE-M1-07 | CI executes the same default acceptance command as local verification. | Minimal GitHub Actions workflow uses Java 25 Temurin and `./mvnw -B verify`; it starts no services and requires no secrets. | GitHub Actions `CI` workflow | M1 | AUTOMATED |
| GATE-M1-08 | The M1 reactor contains only core and JUnit modules. | Parent `<modules>` list and reactor build review; no brittle custom script for this temporary scope. | `./mvnw -B verify` | M1 | MANUAL |

GATE-M1-05 remains partly manual because Maven is allowed to resolve artifacts over the network and a reliable test of “no external runtime service” would be fragile. GATE-M1-08 remains manual because exact milestone module enumeration is better reviewed than enforced with a temporary custom script.

## Deferred gates

| ID | Architectural rule | Enforcement mechanism | Command | Introduced | Status |
| --- | --- | --- | --- | --- | --- |
| GATE-M2-01 | Matrix results preserve deterministic ordering. | M2 runner and tests. | Not applicable before M2 | M2 | DEFERRED |
| GATE-M2-02 | Runs have explicit terminal status invariants. | M2 result model and tests. | Not applicable before M2 | M2 | DEFERRED |
| GATE-M2-03 | Failure classification distinguishes mismatch, failure, unavailable, timeout, and cancellation. | M2 result model and contract tests. | Not applicable before M2 | M2 | DEFERRED |
| GATE-M2-04 | Completed results are immutable. | M2 result model and tests. | Not applicable before M2 | M2 | DEFERRED |
| GATE-M2-05 | Scenarios describe behavior and assertions evaluate results without invoking models. | M2 API review and tests. | Not applicable before M2 | M2 | DEFERRED |
| GATE-M3-01 | Spring AI types remain outside core. | M3 adapter module boundary and dependency checks. | Not applicable before M3 | M3 | DEFERRED |
| GATE-M3-02 | Ollama tests remain opt-in. | M3 profile/job review and CI separation. | Not applicable before M3 | M3 | DEFERRED |
| GATE-M3-03 | Real-model tests never enter default verification. | M3 profile and CI workflow review. | Not applicable before M3 | M3 | DEFERRED |
| GATE-M4-01 | Structured-output and tool-call capability types do not leak into core without an architecture decision. | M4 module/dependency review and tests. | Not applicable before M4 | M4 | DEFERRED |
| GATE-M5-01 | RAG and vector-store dependencies remain outside core. | M5 module/dependency review and tests. | Not applicable before M5 | M5 | DEFERRED |
| GATE-M5-02 | pgvector integration remains opt-in. | M5 profile and CI review. | Not applicable before M5 | M5 | DEFERRED |
| GATE-M6-01 | MCP protocol types remain outside core. | M6 module/dependency review and tests. | Not applicable before M6 | M6 | DEFERRED |
| GATE-M6-02 | External MCP transport tests remain opt-in. | M6 profile and CI review. | Not applicable before M6 | M6 | DEFERRED |
