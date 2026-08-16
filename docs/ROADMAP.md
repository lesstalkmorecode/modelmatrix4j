# ModelMatrix4J roadmap

The roadmap is deliberately compressed so the actual product thesis is exercised by M3. A module is added only when its milestone has a concrete contract and tests. Every acceptance criterion is intended to be observable in a repository checkout or CI job.

## M0 — Specification

**Goal:** Establish a coherent product boundary, architecture, minimal domain model, test strategy, and implementation sequence.

**Why this milestone exists:** The project has high coupling risk between core, JUnit, Spring AI, Ollama, and later capability modules. Written boundaries must prevent framework leakage and a generic-kernel-only MVP.

**In scope:** `AGENTS.md`; product, architecture, domain, test-strategy, roadmap, and ADR guidance; critical review and reconciliation.

**Out of scope:** Production Java, Maven build, Spring configuration, Docker/Compose, Ollama integration, RAG infrastructure, MCP implementation, and provider integrations.

**Architecture constraints:** Core remains provider/framework-neutral; external services are opt-in; public abstractions are provisional until an implementation use case requires them.

**Implementation tasks:** Review and reconcile documents; record consequential decisions as ADRs; turn unresolved questions into milestone gates.

**Required tests:** Document consistency review; required-section/path check; prohibited-artifact check.

**Acceptance criteria:** All required documents exist; the roadmap contains M0–M8; M3 explicitly delivers Spring AI + two local Ollama configurations; MVP scope names the vertical slice; no production implementation is introduced.

**Definition of Done:** A principal-engineer review has addressed premature abstractions, hidden coupling, unnecessary infrastructure, and untestable acceptance criteria.

## M1 — Java 25 / Maven foundation

**Goal:** Create a reproducible Java 25 Maven multi-module foundation with only the initial core and JUnit modules.

**Why this milestone exists:** The specification needs an executable, quality-checked project foundation before execution contracts can be implemented.

**In scope:** Maven Wrapper; parent dependency management; `modelmatrix-core`; `modelmatrix-junit`; license metadata; minimal formatting/static checks; default CI verification.

**Out of scope:** Spring AI, Ollama, real providers, RAG, MCP, cloud credentials, reporting, and production containers.

**Architecture constraints:** Core has no prohibited dependency; JUnit depends inward; optional integrations are not required reactor modules; default verification is offline.

**Implementation tasks:** Set Java 25 release; configure reproducible dependency versions and compiler warnings; add dependency-direction guard; document build commands and quality-tool purpose; ensure Maven Wrapper scripts and `.mvn/wrapper` metadata remain commit-able even though `*.jar` is globally ignored, adding a narrow ignore exception if the chosen wrapper distribution includes `.mvn/wrapper/maven-wrapper.jar`.

**Required tests:** Clean wrapper build; core test smoke path; JUnit module smoke path; core dependency inspection; verification that required Maven Wrapper files are not ignored.

**Acceptance criteria:** `./mvnw -B verify` succeeds without external services; core’s dependency graph contains no Spring, Spring AI, MCP, PostgreSQL, or provider SDK; Java 25 compilation is enforced.

**Definition of Done:** A clean checkout builds in CI and no enabled quality tool lacks a documented problem it solves.

## M2 — Minimal core execution + JUnit integration

**Goal:** Implement the smallest execution path that can run one scenario through JUnit and compare deterministic model results.

**Why this milestone exists:** M3 needs a stable test-facing path, but the core must be proven without real models or a generic abstraction framework.

**In scope:** Minimal scenario input; model descriptor; adapter-backed model-under-test boundary; immutable minimal `RunResult`; minimal core compatibility result/matrix comparison; JUnit extension/facade; deterministic fake models.

**Out of scope:** Spring, Ollama, structured-output types, Java tool-call types, retrieval projections, MCP projections, cloud services, persistence, generic event/interaction abstractions, and public lifecycle objects separate from `RunResult`.

**Architecture constraints:** Core remains JDK-only and provider-neutral; no hidden retries; scenarios do not own assertions; assertions do not invoke models; only abstractions required by tests are public.

**Implementation tasks:** Implement one-run and matrix orchestration; define terminal statuses and the minimal compatibility comparison; implement JUnit lifecycle and failure mapping; add basic text normalization, redaction, and bounded diagnostics.

**Required tests:** Success, mismatch, execution failure, unavailable, timeout, cancellation, ordering, repetition, redaction, JUnit discovery, parameter resolution, invalid configuration, and parallel-isolation tests.

**Acceptance criteria:** One deterministic scenario runs against two fake descriptors through JUnit; the core compatibility result preserves each minimal `RunResult` and distinguishes mismatch from failure/unavailable; fixed inputs produce deterministic comparison ordering; core has no external-service dependency.

**Definition of Done:** The API review confirms that test code depends only on the smallest stable facade and `RunResult` is sufficient for callers.

## M3 — Spring AI + Ollama vertical slice

**Goal:** Demonstrate the product thesis with one real Spring AI scenario executed against two local Ollama model configurations.

**Why this milestone exists:** A generic execution kernel is not the product. The first end-to-end proof must show meaningful behavioral comparison in the target ecosystem without paid providers.

**In scope:** Optional `modelmatrix-spring-ai`; Spring AI request/response adapter; two explicitly configured local Ollama targets; availability handling; opt-in JUnit integration test; normalized run and compatibility result.

**Out of scope:** Structured-output specialization, Java tool-calling assertions, RAG/pgvector, MCP, cloud providers, hosted reporting, and mandatory Docker/Compose.

**Architecture constraints:** Core gains no Spring dependency; Ollama is never required by default; the same scenario definition crosses both model configurations; unavailable local services are explicit and never silently pass.

**Implementation tasks:** Choose the smallest supported Spring AI client boundary; adapt responses and failures; define local model configuration and availability policy; implement one representative scenario; document how to run the opt-in profile.

**Required tests:** Spring adapter contract tests with deterministic stubs; opt-in Ollama smoke/vertical-slice test; two-model comparison assertion; unavailable-service and cleanup tests.

**Acceptance criteria:** When enabled and both models are available, one Spring AI scenario executes against both local configurations and yields a compatibility result with per-model status, basic normalized text where applicable, timing, and bounded diagnostics; when disabled or unavailable, default verification remains offline and successful.

**Definition of Done:** A contributor can reproduce the vertical slice from documented local instructions, and the result demonstrates a meaningful behavioral comparison rather than only recording raw text.

## M4 — Structured output + Java tool calling

**Goal:** Add the first capability assertions that expose useful cross-model behavioral differences.

**Why this milestone exists:** Structured output and Java tool calling test normalized projections beyond generic text and validate that the vertical slice can grow without contaminating core.

**In scope:** Canonical structured-data projection/assertions; tool selection/argument/result observations; Spring AI adapter support; deterministic fakes; opt-in local-model coverage.

**Out of scope:** RAG, pgvector, MCP, arbitrary side-effectful tool registries, and cloud-provider guarantees.

**Architecture constraints:** Capability types stay outside core unless a concrete shared fact is proven; raw payloads are opt-in; tool side effects are test-scoped and isolated; scenario and assertion responsibilities remain separate.

**Implementation tasks:** Resolve structured-data representation; define mismatch/failure classifications; normalize tool calls and safe arguments; document model variability and adapter limitations.

**Required tests:** Canonicalization, malformed/missing output, valid/invalid/no tool call, timeout, tool failure, redaction, deterministic adapter, and opt-in Ollama tests.

**Acceptance criteria:** Equivalent structured values compare equal under documented rules; tool identity and normalized arguments can be asserted independently of prose; mismatch, tool failure, and unavailable target remain distinct.

**Definition of Done:** Capability APIs are reviewed for minimality and do not add framework/provider dependencies to core.

## M5 — RAG + pgvector

**Goal:** Add retrieval behavior as an isolated capability with deterministic fixtures and optional PostgreSQL + pgvector integration.

**Why this milestone exists:** RAG is a useful test capability, but it must not redefine the framework or force database infrastructure on all users.

**In scope:** `modelmatrix-rag` retrieval observations/assertions; fixed-corpus tests; optional Spring AI/vector-store adapter; PostgreSQL + pgvector/Testcontainers integration profile.

**Out of scope:** Production knowledge bases, mandatory PostgreSQL, a generic vector-database abstraction without a tested consumer, and cloud embedding requirements.

**Architecture constraints:** Retrieval facts remain capability data; stores and embeddings stay outside core; fixtures and containers are isolated and opt-in.

**Implementation tasks:** Define minimal evidence/ranking/filter/no-result contract; implement deterministic fixtures; add database adapter only after the fixture contract is stable; document lifecycle and cleanup.

**Required tests:** Offline ranking/filter/no-result/citation tests; adapter contract tests; opt-in pgvector lifecycle, cleanup, and parallel-safety tests.

**Acceptance criteria:** RAG dependencies are absent from core; deterministic retrieval tests run offline; pgvector tests are opt-in and isolated; retrieval mismatch is distinct from generation failure.

**Definition of Done:** The public RAG scope does not assume one vector store or embedding provider.

## M6 — MCP

**Goal:** Add MCP tool/resource behavior through an isolated capability and adapter boundary.

**Why this milestone exists:** MCP introduces protocol and session failure modes that must be tested without making MCP part of the core product contract.

**In scope:** `modelmatrix-mcp` normalized observations/assertions; deterministic in-process fixture; optional Spring AI MCP adapter; explicit transport/server integration profile.

**Out of scope:** Hosting an MCP service, reimplementing an MCP SDK, mandatory server/container runtime, and production tool security.

**Architecture constraints:** MCP types stop at the adapter; session/transport failures are explicit; default verification remains offline.

**Implementation tasks:** Define minimum tool/resource contract; implement capability negotiation and failure normalization; add isolated transport tests; document side-effect and security boundaries.

**Required tests:** Valid/invalid calls, resource retrieval, negotiation, timeout, disconnect, cleanup, and deterministic normalization tests.

**Acceptance criteria:** MCP protocol failure is not reported as a behavioral mismatch; deterministic tests cover the normalized contract; server tests run only when explicitly enabled.

**Definition of Done:** Supported transports and limitations are documented and no MCP dependency enters core.

## M7 — Provider matrix + reporting

**Goal:** Compare multiple model/provider configurations and emit stable CI-friendly artifacts.

**Why this milestone exists:** The minimal matrix comparison already exists in core from M2. M7 makes those results durable, shareable, and more expressive for CI and provider-matrix use.

**In scope:** Stable reporting; serialization; CI artifacts; human-readable summaries; richer presentation/comparison rules over the existing core compatibility result; optional provider job guidance.

**Out of scope:** Hosted dashboards, database persistence, alerting services, user accounts, and mandatory cloud providers.

**Architecture constraints:** Reports consume immutable results; no secrets by default; unavailable, failed, and mismatched runs remain distinct; reporting requires no network/database.

**Implementation tasks:** Stabilize result schema; define comparison semantics; implement serialization/redaction; add golden-file tests; document report versioning.

**Required tests:** Stable ordering, golden reports, redaction, bounded diagnostics, partial matrices, unavailable providers, and schema compatibility tests.

**Acceptance criteria:** Identical ordered results produce byte-stable reports; partial/unavailable targets are visible; default report generation is offline; provider-specific metadata is optional and namespaced.

**Definition of Done:** Report format and compatibility policy are documented before publication.

## M8 — OSS release hardening

**Goal:** Make the library publishable and maintainable as a real open-source project.

**Why this milestone exists:** The framework needs contributor ergonomics, compatibility policy, and release discipline in addition to working behavior.

**In scope:** Public API/Javadocs review; README usage; Maven Central/release metadata; Apache-2.0 notices; GitHub Actions; semantic versioning; security/dependency policy; changelog and contribution guidance.

**Out of scope:** Hosted service, mandatory infrastructure, and breaking API expansion for marketing purposes.

**Architecture constraints:** SemVer reflects public API; optional modules remain optional; release builds are reproducible; external tests are separate from required checks.

**Implementation tasks:** Establish compatibility baseline; perform dependency/license/security review; document release workflow; validate clean checkout and offline/default build; archive completed execution plans.

**Required tests:** Full default verification; module compatibility checks; sample consumer build; opt-in integration smoke jobs; release artifact inspection.

**Acceptance criteria:** A clean checkout builds and tests without paid credentials; artifacts contain only intended modules and metadata; supported versions and public APIs are documented; required CI checks are deterministic.

**Definition of Done:** Maintainers can cut a versioned release using documented steps and contributors can run the default build without proprietary services.
