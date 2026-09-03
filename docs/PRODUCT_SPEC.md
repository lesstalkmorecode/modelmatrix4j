# ModelMatrix4J product specification

## Problem

Changing the model behind a Spring AI application can change behavior even when the Java code and prompt stay the same. Regressions may appear in generated text, structured output, tool selection, tool arguments, retrieved documents, MCP interactions, latency, or execution failures.

ModelMatrix4J provides a test-oriented way to run one scenario against at least two model configurations and compare the application-visible behavior those configurations produce.

The core question is:

> **If I replace the model behind this application, does the behavior my application relies on remain compatible?**

## Intended users

ModelMatrix4J is aimed at Java developers and test engineers working with Spring AI applications who want deterministic, assertion-friendly regression tests around model changes.

The provider-neutral core can also be used without Spring AI for deterministic tests and custom adapters.

## Core use cases

- define one scenario and run it against at least two model configurations;
- distinguish compatible behavior from mismatch, unavailable targets, and execution failures;
- repeat scenarios under deterministic execution identity and ordering;
- compare structured JSON behavior semantically rather than as raw text;
- compare ordered tool identity and semantic arguments;
- compare ordered logical retrieval document identities;
- compare application-visible MCP tool interactions;
- integrate the same execution model with JUnit Jupiter;
- generate deterministic persistence-safe reports from completed results;
- run the default verification path without requiring a live model/provider service.

## Non-goals

ModelMatrix4J is not:

- an LLM benchmark;
- an LLM-as-a-judge system;
- a chatbot or conversation UI;
- a generic provider SDK wrapper;
- a production inference gateway;
- an agent runtime or workflow engine;
- a hosted evaluation/dashboard service;
- a replacement for application-specific correctness assertions;
- a generic evidence/event framework;
- an MCP transport/session framework;
- a vector database abstraction.

## Product model

The core domain is intentionally small:

- **Scenario** — provider-neutral input for one behavioral interaction.
- **Model descriptor** — stable identity of one model configuration.
- **Model under test** — descriptor plus executable adapter.
- **Run result** — terminal outcome for one configuration/repetition execution.
- **Compatibility result** — aggregate status plus ordered run outcomes for a matrix.
- **Capability observation** — structured/tool/retrieval/MCP evidence captured from the same physical execution and retained outside the core result model.
- **Report** — deterministic downstream projection of completed results for persistence or CI.

A compatibility matrix requires at least two model configurations.

## Execution ownership

`modelmatrix-core` owns the physical execution lifecycle:

- timeout and cancellation;
- repetitions;
- bounded concurrency;
- deterministic result ordering;
- terminal status classification;
- plain-text compatibility.

Capability modules may wrap adapters or callbacks to capture additional evidence, but evaluation must reuse evidence from that same execution. A capability evaluator does not invoke the model again.

The detailed execution and capability semantics are defined in [`ARCHITECTURE.md`](ARCHITECTURE.md).

## Capability boundaries

### Structured output

Structured output is treated as JSON data. Object member order is ignored, array order is preserved, numeric formatting differences such as `1` versus `1.0` are equivalent, and malformed/schema-invalid values are classified separately from behavioral mismatch.

### Tool calling

Tool compatibility compares ordered tool identity and semantic JSON arguments. Tool results are not part of the compatibility comparison contract. Malformed argument JSON is invalid evidence rather than mismatch.

### Retrieval

Retrieval compatibility compares ordered stable logical document identities. Embeddings, similarity scores, vector-store row IDs, and raw retrieved text are not default compatibility facts.

### MCP

MCP compatibility compares application-visible ordered tool interactions and semantic arguments. Transport, discovery, session management, protocol frames, and resource semantics are outside the current contract.

## Spring AI integration

Spring AI integration terminates at the framework abstraction that owns the observable behavior:

- `ChatModel` for generated text and raw tool-call requests;
- `ChatClient` for retrieval advisor context and MCP-backed callback execution.

Spring context management and Boot autoconfiguration are outside the library boundary.

## Reporting and security

Reporting is downstream from execution. It does not trigger model/provider work.

The default durable report intentionally excludes model output, diagnostics, raw structured payloads, tool arguments/results, retrieved content, MCP arguments, and provider-native payloads. Its schema and compatibility rules are defined in [`REPORT_SCHEMA.md`](REPORT_SCHEMA.md).

Security boundaries for repository content and generated artifacts are summarized in [`../SECURITY.md`](../SECURITY.md).

## Verification model

The default project verification is deterministic with respect to model/provider services:

```bash
./mvnw -B clean verify
```

The repository also verifies standalone external consumers and reproducible build artifacts. Real Ollama, pgvector, and MCP integration paths are opt-in and do not participate in the default build.

## Public API

The reviewed consumer-facing Java surface is documented in [`PUBLIC_API.md`](PUBLIC_API.md). Public Java visibility alone does not make a type part of that reviewed API boundary.
