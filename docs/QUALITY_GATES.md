# ModelMatrix4J quality gates

This document maps existing architectural rules to the smallest current enforcement. It does not duplicate the architecture or introduce future-milestone implementation.

## Cross-cutting gates

| ID | Architectural rule | Enforcement mechanism | Command | Introduced | Status |
| --- | --- | --- | --- | --- | --- |
| GATE-AGENT-01 | Multi-agent milestone work records actual agent/delegation identity, dependency ordering, exclusive writable ownership, handoff and reconciliation evidence, ownership release, and independent reviewer identity. | Review the active plan's contracts, ledger, actual handoffs, reconciliation decisions, and integrated workspace; a filled ledger alone is not proof. | Manual review | Cross-cutting | MANUAL |
| GATE-AGENT-02 | Package/path delegations resolve exact or explicitly bounded recursive scopes and exclusions, broad/narrow overlap, cross-package dependency order, public/shared API escalation and inventory, independent API review, production/test mapping, and task-complete ownership release. | Review contracts, API inventory, ownership/escalation records, sequenced handoffs, reconciliation, and the integrated diff. | Manual review | M1.6 | MANUAL |

The agentic gates remain manual because repository records cannot automatically prove actual delegation, scope compliance, reconciliation, or reviewer independence.

## M1 gates

| ID | Architectural rule | Enforcement mechanism | Command | Introduced | Status |
| --- | --- | --- | --- | --- | --- |
| GATE-M1-01 | The project builds only on Java 25. | Maven Enforcer `requireJavaVersion [25,26)`. | `./mvnw -B verify` | M1 | AUTOMATED |
| GATE-M1-02 | The reactor has no conflicting dependency versions. | Maven Enforcer `dependencyConvergence` in the parent POM. | `./mvnw -B verify` | M1 | AUTOMATED |
| GATE-M1-03 | `modelmatrix-core` excludes Spring, Spring AI, MCP, PostgreSQL, and known provider SDKs. | Core-only Maven Enforcer `bannedDependencies`; the parent does not inherit this rule. | `./mvnw -B verify` | M1 | AUTOMATED |
| GATE-M1-04 | JUnit integration may depend inward on core; core must not depend on the JUnit module or production JUnit libraries. | Manual review of both module POMs and dependency trees. The JUnit-module `dependency:tree` reports current dependencies but does not itself assert the prohibited core edge or exclude production-scoped JUnit from core. | Manual review, informed by `./mvnw -B -pl modelmatrix-junit dependency:tree` | M1 | MANUAL |
| GATE-M1-05 | Default verification requires no external runtime service or credential. | CI runs only the deterministic Maven Wrapper verification. Maven artifact resolution may access configured repositories; absence of runtime-service use is reviewed manually. | `./mvnw -B verify` | M1 | MANUAL |
| GATE-M1-06 | Both current modules execute their test lifecycle correctly. | Maven Surefire with JUnit Jupiter; core JUnit dependencies are test-scoped. | `./mvnw -B verify` | M1 | AUTOMATED |
| GATE-M1-07 | CI executes the same default acceptance command as local verification. | Minimal GitHub Actions workflow uses Java 25 Temurin and `./mvnw -B verify`; it starts no services and requires no secrets. | GitHub Actions `CI` workflow | M1 | AUTOMATED |
| GATE-M1-08 | The M1 reactor contains only core and JUnit modules. | Parent `<modules>` list and reactor build review; no brittle custom script for this temporary scope. | `./mvnw -B verify` | M1 | MANUAL |

GATE-M1-04 remains manual because the current build has no rule asserting the prohibited core-to-JUnit edge or excluding production-scoped JUnit and other unapproved libraries from core. GATE-M1-05 remains partly manual because Maven is allowed to resolve artifacts over the network and a reliable test of “no external runtime service” would be fragile. GATE-M1-08 remains manual because exact milestone module enumeration is better reviewed than enforced with a temporary custom script.

## M1.6 gates

| ID | Architectural rule | Enforcement mechanism | Command | Introduced | Status |
| --- | --- | --- | --- | --- | --- |
| GATE-M16-01 | Documented package responsibilities are cohesive and explicitly approved, conditional, rejected, or deferred; no unapproved package, empty directory, or placeholder pre-creates future structure. | Review `docs/ARCHITECTURE.md`, the active plan reconciliation, repository tree, and diff. | Manual review | M1.6 | MANUAL |

This gate is manual because M1.6 defines conditional documentation boundaries and creates no source packages or executable package graph.

## Deferred gates

| ID | Architectural rule | Enforcement mechanism | Command | Introduced | Status |
| --- | --- | --- | --- | --- | --- |
| GATE-M2-01 | Matrix results preserve deterministic ordering. | M2 runner and tests. | Not applicable before M2 | M2 | DEFERRED |
| GATE-M2-02 | Runs have explicit terminal status invariants. | M2 result model and tests. | Not applicable before M2 | M2 | DEFERRED |
| GATE-M2-03 | Execution classification distinguishes failure, unavailable, timeout, and cancellation; behavioral mismatch is a separate compatibility/assertion outcome. | M2 result model, comparison, and contract tests. | Not applicable before M2 | M2 | DEFERRED |
| GATE-M2-04 | Completed results are immutable. | M2 result model and tests. | Not applicable before M2 | M2 | DEFERRED |
| GATE-M2-05 | Scenarios describe behavior and assertions evaluate results without invoking models. | M2 API review and tests. | Not applicable before M2 | M2 | DEFERRED |
| GATE-M2-06 | Core has no production JUnit or other unapproved compile/runtime dependency, and each module contains only dependencies authorized for M2. | M2 dependency-scope review and, when implementation exists, an explicitly approved executable check. | Not applicable before M2 | M2 | DEFERRED |
| GATE-M2-07 | Public APIs are limited to current-milestone use cases; internal types and later-capability projections are not exposed. | M2 public-API inventory, consumer tests, and independent API review. | Not applicable before M2 | M2 | DEFERRED |
| GATE-M2-08 | Default M2 tests are deterministic and isolated from runtime services, credentials, network calls, and mutable global environment. | M2 deterministic tests plus source and configuration review. | Not applicable before M2 | M2 | DEFERRED |
| GATE-M2-09 | Sensitive values are redacted from results, exceptions, and assertion diagnostics. | M2 redaction tests with representative sensitive inputs and review of diagnostic paths. | Not applicable before M2 | M2 | DEFERRED |
| GATE-M2-10 | Captured output and diagnostics have explicit, deterministic bounds, including truncation behavior. | M2 boundary tests for configured limits and diagnostic rendering. | Not applicable before M2 | M2 | DEFERRED |
| GATE-M2-11 | Implemented packages follow approved acyclic responsibility dependencies; exposed signature graphs do not contain internal types, and invoked internal implementation creates no dependency path back to its calling entry-point package. | M2 source/package dependency review, public-API inventory, independent API review, and consumer tests outside implementation packages. | Not applicable before M2 | M2 | DEFERRED |
| GATE-M3-01 | Spring AI types remain outside core. | M3 adapter module boundary and dependency checks. | Not applicable before M3 | M3 | DEFERRED |
| GATE-M3-02 | Ollama tests remain opt-in. | M3 profile/job review and CI separation. | Not applicable before M3 | M3 | DEFERRED |
| GATE-M3-03 | Real-model tests never enter default verification. | M3 profile and CI workflow review. | Not applicable before M3 | M3 | DEFERRED |
| GATE-M4-01 | Structured-output and tool-call capability types do not leak into core without an architecture decision. | M4 module/dependency review and tests. | Not applicable before M4 | M4 | DEFERRED |
| GATE-M5-01 | RAG and vector-store dependencies remain outside core. | M5 module/dependency review and tests. | Not applicable before M5 | M5 | DEFERRED |
| GATE-M5-02 | pgvector integration remains opt-in. | M5 profile and CI review. | Not applicable before M5 | M5 | DEFERRED |
| GATE-M6-01 | MCP protocol types remain outside core. | M6 module/dependency review and tests. | Not applicable before M6 | M6 | DEFERRED |
| GATE-M6-02 | External MCP transport tests remain opt-in. | M6 profile and CI review. | Not applicable before M6 | M6 | DEFERRED |
