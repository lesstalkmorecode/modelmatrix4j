# Public API baseline

This document defines the current reviewed Java API surface for ModelMatrix4J. It is a repository-level compatibility and review boundary used by documentation and standalone consumer verification.

## Compatibility boundary

Only public top-level types explicitly listed in this document are part of the supported Java API. Package membership alone does not create a compatibility promise. A newly added `public` helper is therefore not automatically supported API; it must be reviewed and deliberately added to this baseline.

A listed supported top-level type includes its current public API surface: public constructors, methods, fields/constants, record components, and public nested types together with their public members. For example, `ModelMatrix` includes `ModelMatrix.Builder`; `ToolCallComparison` includes `ToolCallComparison.Status`; and `StructuredOutputResult` includes its public nested status/observation types. These nested types do not need separate allowlist entries because they are already part of the listed top-level type's public surface.

Adding, removing, renaming, or incompatibly changing a public member or public nested type of a listed supported top-level type still requires compatibility review. A future nested type is not exempt from review merely because its enclosing top-level type is already listed.

Package-private implementation types, public framework wiring types not listed below, test fixtures, integration-test infrastructure, Maven profiles, and internal execution helpers are not supported Java API.

The supported surface is grouped into four categories:

1. **Core domain and execution API** — stable provider-neutral contracts used directly by applications and tests.
2. **Capability API** — provider-neutral structured-output, tool, retrieval, and MCP contracts used when that capability is enabled.
3. **Adapter API** — Spring AI integration types whose signatures necessarily track the supported Spring AI line.
4. **Report API** — the Java projection/writer API. The durable report schema has its own compatibility contract in `REPORT_SCHEMA.md` and is not versioned implicitly by Java API changes.

## Supported public types

### `modelmatrix-core`

Supported consumer API:

- `com.modelmatrix4j.core.execution.ModelMatrix`
- `com.modelmatrix4j.core.scenario.Scenario`
- `com.modelmatrix4j.core.model.ModelAdapter`
- `com.modelmatrix4j.core.model.ModelDescriptor`
- `com.modelmatrix4j.core.model.ModelUnderTest`
- `com.modelmatrix4j.core.model.ModelUnavailableException`
- `com.modelmatrix4j.core.result.CompatibilityResult`
- `com.modelmatrix4j.core.result.CompatibilityStatus`
- `com.modelmatrix4j.core.result.RunResult`
- `com.modelmatrix4j.core.result.RunStatus`

`ModelMatrix` and its public builder API are the execution facade. Core remains JDK-only. No Spring, provider, capability, report, MCP, or persistence type may enter a supported core signature.

Package-private orchestration types such as executors, invocation runners, execution settings/outcomes, evaluators, and result mappers are implementation details.

### `modelmatrix-junit`

Supported consumer API:

- `com.modelmatrix4j.junit.ModelMatrixTest`
- `com.modelmatrix4j.junit.ModelMatrixSource`

`ModelMatrixExtension` is public because JUnit's composed annotation wiring must be able to reference and instantiate it, but it is **framework wiring, not direct consumer API**. Its constructor and direct `BeforeEachCallback` / `ParameterResolver` usage are not part of the compatibility baseline. Consumers should use `ModelMatrixTest` plus `ModelMatrixSource`.

If a future use case requires direct extension registration, that must be reviewed explicitly before `ModelMatrixExtension` is added to the supported type list.

### `modelmatrix-structured`

Supported consumer API:

- `com.modelmatrix4j.structured.JsonObjectSchema`
- `com.modelmatrix4j.structured.JsonValueComparator`
- `com.modelmatrix4j.structured.StructuredOutputEvaluator`
- `com.modelmatrix4j.structured.StructuredOutputExecution`
- `com.modelmatrix4j.structured.StructuredOutputObservation`
- `com.modelmatrix4j.structured.StructuredOutputResult`

These types own semantic JSON comparison, schema validation, structured observations/results, and composition with the core lifecycle. Public nested result/status vocabulary exposed by these listed types is included in the compatibility baseline under the rule above.

### `modelmatrix-tool`

Supported consumer API:

- `com.modelmatrix4j.tool.ToolAdapter`
- `com.modelmatrix4j.tool.ToolArgumentValidator`
- `com.modelmatrix4j.tool.ToolCallComparator`
- `com.modelmatrix4j.tool.ToolCallComparison`
- `com.modelmatrix4j.tool.ToolCallObservation`
- `com.modelmatrix4j.tool.ToolExecution`
- `com.modelmatrix4j.tool.ToolInvocation`
- `com.modelmatrix4j.tool.ToolModel`
- `com.modelmatrix4j.tool.ToolObservation`

These types own Java tool-call capture/composition, JSON argument validation, and comparison semantics. Public nested status vocabulary such as `ToolCallComparison.Status` is included in the listed top-level type's supported surface.

### `modelmatrix-rag`

Supported consumer API:

- `com.modelmatrix4j.rag.RetrievalAdapter`
- `com.modelmatrix4j.rag.RetrievalComparator`
- `com.modelmatrix4j.rag.RetrievalEvaluator`
- `com.modelmatrix4j.rag.RetrievalExecution`
- `com.modelmatrix4j.rag.RetrievalInvocation`
- `com.modelmatrix4j.rag.RetrievalModel`
- `com.modelmatrix4j.rag.RetrievalObservation`
- `com.modelmatrix4j.rag.RetrievalResult`
- `com.modelmatrix4j.rag.RetrievedDocument`

These types own provider-neutral retrieval observations, stable logical document identity, evaluation, comparison, and core-lifecycle composition.

### `modelmatrix-mcp`

Supported consumer API:

- `com.modelmatrix4j.mcp.McpAdapter`
- `com.modelmatrix4j.mcp.McpComparator`
- `com.modelmatrix4j.mcp.McpEvaluator`
- `com.modelmatrix4j.mcp.McpExecution`
- `com.modelmatrix4j.mcp.McpInvocation`
- `com.modelmatrix4j.mcp.McpModel`
- `com.modelmatrix4j.mcp.McpObservation`
- `com.modelmatrix4j.mcp.McpResult`
- `com.modelmatrix4j.mcp.McpToolInteraction`

These types own application-visible MCP tool-interaction observations and comparison. MCP transport/session/resource internals are not part of this baseline.

Capability-local evidence remains outside `RunResult`. Capability execution APIs compose with the core lifecycle and must not create a second model invocation merely for evaluation.

### `modelmatrix-spring-ai`

Supported adapter API:

- `com.modelmatrix4j.springai.SpringAiModelAdapter`
- `com.modelmatrix4j.springai.SpringAiToolCallAdapter`
- `com.modelmatrix4j.springai.SpringAiRetrievalAdapter`
- `com.modelmatrix4j.springai.SpringAiMcpToolAdapter`

These types translate supported Spring AI APIs into core/capability contracts. Spring AI types are allowed here and nowhere in provider-neutral modules.

Package-private callback decorators such as `SpringAiMcpToolObserver` are not API. Changes forced by a supported Spring AI major/minor line are adapter compatibility concerns; they must not silently widen or break core contracts.

### `modelmatrix-report`

Supported consumer API:

- `com.modelmatrix4j.report.CompatibilityReport`
- `com.modelmatrix4j.report.JsonReportWriter`
- `com.modelmatrix4j.report.ReportCompatibilityStatus`
- `com.modelmatrix4j.report.ReportProjector`
- `com.modelmatrix4j.report.ReportRunStatus`
- `com.modelmatrix4j.report.RunReport`
- `com.modelmatrix4j.report.TextReportWriter`

`ReportProjector` is the boundary from core results into the report model. The Java report API and the durable schema are deliberately separate contracts. Schema compatibility is defined in [`REPORT_SCHEMA.md`](REPORT_SCHEMA.md); core enum evolution must be mapped explicitly rather than leaking into an existing schema version.

## What is not supported public API

A Java type is not part of the compatibility baseline merely because it is declared `public`. In particular, the following are implementation/framework details unless they are explicitly added to the supported type list above:

- `ModelMatrixExtension` direct construction or direct extension-callback behavior;
- matrix executors, invocation runners, execution outcome carriers, result mappers, evidence stores, correlation keys, callback decorators, and similar implementation helpers;
- deterministic fixtures used only from tests;
- Ollama, pgvector, and MCP integration-test lifecycle code;
- Maven profile names as a Java compatibility promise;
- incidental `toString()` formats unless a document explicitly defines them as durable output;
- JSON produced by serializing arbitrary Java records directly. Only the documented report schema is durable serialized output.

No internal helper should be made public merely to simplify tests. When framework mechanics require a public type that consumers are not expected to use directly, this baseline must explicitly classify it as framework wiring rather than relying on package-level assumptions.

## Constructor and record policy

Public constructors, record canonical constructors/components, methods, constants, and public nested types of **supported top-level types** are API. Each must have a consumer-facing reason to exist. Validation performed by supported constructors is part of the observable contract and should be covered by tests.

A constructor or public nested type on an unlisted framework-wiring or implementation top-level type does not become supported API merely because Java visibility makes it callable.

New public members should be preferred only when they represent a stable consumer path. Internal correlation/state objects should remain package-private instead of exposing types or constructors that consumers are not expected to call.

## Compatibility policy

This document is a reviewed repository boundary and may be tightened deliberately as the project evolves.

Adding a new supported top-level type requires an explicit edit to this baseline. Simply adding a new top-level `public` declaration does not create a compatibility guarantee. Adding a public member or nested type to an already listed supported type is additive API, but still requires compatibility review because it expands the supported surface.

Additive APIs should still be justified by a concrete consumer. ModelMatrix4J does not use public generic extension frameworks such as `Evidence<T>` as placeholders for possible future capabilities.

The durable report schema follows its independent schema-version policy even when the corresponding Java change would otherwise be source/binary compatible.

## Review checklist for public API changes

Before adding or changing a supported public API:

- Is the top-level type explicitly listed in this baseline, or is the member/nested type part of a listed top-level type?
- Is there a concrete external consumer path?
- Can the type/member remain package-private or framework-only?
- Does the signature leak Spring/provider/capability types across the wrong module boundary?
- Does the change alter timeout, cancellation, repetition, ordering, failure, or evidence semantics?
- Does a public record/constructor expose data that should remain capability-local or persistence-restricted?
- Does a report change require a report schema-version decision?
- Is the API documented sufficiently for generated Javadocs without reading implementation code?
- Is the behavior covered through the public entry point?

Standalone external-consumer verification uses this baseline to guard the reviewed API surface.
