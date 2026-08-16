# ModelMatrix4J product specification

## 1. Problem statement

Spring AI applications can appear correct against one model and behave differently against another. Differences may appear in structured output, tool selection, tool arguments, retrieval use, latency, failures, or repeatability. Teams need a test-oriented way to run the same Spring AI scenario against multiple local model configurations and obtain a meaningful, assertion-friendly compatibility result.

ModelMatrix4J is a Java testing framework for expressing that scenario once, executing it through Spring AI against a local model matrix, and comparing observable outcomes. It is a compatibility and regression-testing library, not an application runtime.

## 2. Target users

- Java developers building Spring AI applications.
- Framework and platform teams validating model substitutions.
- Test engineers maintaining AI behavior regression suites.
- Open-source maintainers who need local, reproducible tests before enabling cloud providers.

The primary user knows Java, Maven, JUnit Jupiter, and Spring AI. The core module remains usable without Spring so framework behavior can be tested deterministically.

## 3. Technical use cases

1. Define one Spring AI scenario and run it against at least two local model configurations.
2. Produce a compatibility result that identifies matching, differing, failed, and unavailable runs.
3. Compare structured output and Java tool-calling behavior across models.
4. Repeat a scenario and evaluate reliability, failure rate, and bounded latency.
5. Evaluate retrieval behavior against a fixed corpus and optional pgvector store.
6. Exercise MCP behavior once the MCP capability is available.
7. Run deterministic framework tests without network access or paid credentials.
8. Opt into local Ollama integration tests for development and CI environments that provide Ollama.
9. Add cloud-provider tests only in explicitly protected, opt-in jobs.

## 4. Non-goals

ModelMatrix4J is not:

- a chatbot or end-user conversation UI;
- a business application or domain-specific test suite;
- a generic LLM client or provider SDK wrapper;
- a production inference gateway;
- an AI dashboard, hosted service, or user/account system;
- a replacement for application-level unit tests;
- a guarantee that models produce semantically identical language;
- a mandatory PostgreSQL, vector database, MCP server, cloud account, or container runtime.

## 5. Terminology

- **Scenario**: a provider-neutral description of one behavioral interaction and the observations it exposes; it does not decide pass/fail.
- **Model descriptor**: stable identity and non-secret metadata for one model configuration; it is not a live client.
- **Model under test**: an executable adapter plus its descriptor.
- **Run result**: the immutable M2/M3 outcome of one scenario/model execution, containing only identity, status, basic text, timing, and bounded diagnostics.
- **Matrix**: one scenario expanded over multiple model descriptors and optional repetitions.
- **Compatibility result**: the minimal core aggregate, introduced in M2, comparing matrix run results and separating behavioral mismatches from execution failures and unavailable targets; M7 adds richer presentation and comparison rules.
- **Assertion**: a separate evaluation of a run result or compatibility result.
- **Adapter**: an optional module translating Spring AI, MCP, retrieval, or provider APIs into the core contract.

## 6. Product capabilities

The product is built in layers:

1. A pure-Java kernel with minimal scenario, model, run-result, compatibility-result, and assertion contracts.
2. JUnit Jupiter integration for test lifecycle and failure reporting.
3. A Spring AI adapter that makes the same scenario executable against multiple model configurations.
4. A local Ollama vertical slice proving the product thesis without paid subscriptions.
5. Capability modules for structured output, Java tool calling, RAG, and MCP.
6. Optional M7 reporting, serialization, CI artifacts, and richer presentation/comparison rules.

The vertical slice is the MVP proof point. Capability modules are additive and never make core depend on their technology.

## 7. Example user experience

The eventual developer experience should be conceptually close to:

```java
@ModelMatrixTest(models = {"local-small", "local-large"})
void customerLookup(CompatibilityResult result) {
    assertThat(result).satisfies(customerLookupAssertions());
}
```

The exact annotation and assertion API remains provisional. The important behavior is that one Spring AI scenario is executed against two local configurations and the test receives a compatibility result that can say, for example, “both models returned the required customer fields” or “model B completed but selected the wrong tool.” A missing local model/service is an explicit unavailable outcome in the opt-in integration test, never a default-build failure.

## 8. Constraints

- Target Java 25, Maven, Spring Boot 4.1.x, Spring AI 2.0.x, JUnit Jupiter, AssertJ, Testcontainers, Jackson, and Micrometer only where justified by a milestone.
- `modelmatrix-core` has no Spring, Spring Boot, Spring AI, MCP, PostgreSQL, or provider SDK dependency.
- The default verification path is deterministic and offline.
- The real-model path is opt-in and uses local Ollama before any paid provider.
- External provider tests are never required for contributor builds.
- Network, clocks, random seeds, concurrency, and external process assumptions are explicit in integration tests.
- Public APIs are intentionally small and documented before release.
- Reports require no production database or hosted service.

## 9. MVP scope

The MVP is complete when the repository demonstrates the actual product thesis:

- one Spring AI scenario can be defined once and executed through ModelMatrix4J;
- the scenario can run against at least two locally available Ollama model configurations;
- the two runs produce a meaningful compatibility result with per-run status, basic normalized text where applicable, timing, and bounded diagnostics;
- a JUnit Jupiter test can assert that compatibility result;
- the same core and JUnit contracts remain covered by deterministic in-memory tests;
- the real-model test is explicitly opt-in and default verification succeeds offline with Ollama unavailable;
- missing models, services, or credentials are classified as unavailable rather than silently passing.

The MVP does not require structured-output, RAG, MCP, cloud-provider, or stable reporting features beyond the minimal core compatibility result needed to show the thesis. Those follow immediately after the vertical slice.

## 10. Future scope

- Structured-output and Java tool-calling compatibility assertions.
- Retrieval/RAG fixtures and optional PostgreSQL + pgvector integration.
- MCP tool/resource assertions and Spring AI MCP integration.
- Stable reporting, serialization, CI artifacts, and richer presentation/comparison rules over the core compatibility result.
- Optional cloud-provider adapters and compatibility guidance.
- OSS release hardening and broader supported-version policy.

Future scope does not authorize adding infrastructure or provider credentials to the default build.
