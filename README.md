# ModelMatrix4J

Compatibility and regression testing for AI models in Java.

ModelMatrix4J runs the same scenario against multiple model configurations and compares their results.

The project is designed primarily for Spring AI applications, while keeping the core execution layer independent from Spring and model providers.

> **Status:** M2 complete  
> **Next:** M3 — Spring AI + Ollama vertical slice

## Why?

Changing an AI model can change application behavior even when the prompt and application code stay the same.

ModelMatrix4J makes the model configuration part of the test matrix:

```text
                 Scenario
                    |
          +---------+---------+
          |                   |
          v                   v
       Model A             Model B
          |                   |
          +---------+---------+
                    |
                    v
           CompatibilityResult
```

Current M2 comparison is intentionally small: normalized text output, execution status, timing, repetitions, and failures.

Structured output, tool calling, RAG, and MCP are later milestones.

## Current capabilities

M2 provides:

- provider-neutral scenarios
- model descriptors
- a small `ModelAdapter` extension point
- multiple models per matrix
- repetitions
- concurrent execution across different models
- sequential repetitions for the same model
- bounded physical invocation concurrency
- timeout and cancellation handling
- unavailable-model classification
- deterministic result ordering
- normalized textual comparison
- immutable run results
- bounded and sanitized diagnostics
- JUnit Jupiter integration
- deterministic offline tests

The core has no production dependency on Spring, Spring AI, Ollama, MCP, PostgreSQL, or provider SDKs.

## Core usage

```java
import com.modelmatrix4j.core.execution.ModelMatrix;
import com.modelmatrix4j.core.model.ModelDescriptor;
import com.modelmatrix4j.core.model.ModelUnderTest;
import com.modelmatrix4j.core.result.CompatibilityResult;
import com.modelmatrix4j.core.scenario.Scenario;

import java.time.Duration;

public class Example {

    public static void main(String[] args) {
        Scenario scenario = new Scenario(
                "greeting",
                "Say hello"
        );

        ModelUnderTest first = new ModelUnderTest(
                new ModelDescriptor("model-a"),
                ignored -> "Hello world"
        );

        ModelUnderTest second = new ModelUnderTest(
                new ModelDescriptor("model-b"),
                ignored -> "  Hello   world "
        );

        ModelMatrix matrix = ModelMatrix.builder()
                .models(first, second)
                .repetitions(1)
                .timeout(Duration.ofSeconds(1))
                .build();

        CompatibilityResult result = matrix.run(scenario);

        System.out.println(result.status());
    }
}
```

The result is:

```text
COMPATIBLE
```

because M2 normalizes surrounding and repeated whitespace before comparison.

Changing one model to:

```java
ignored -> "Goodbye world"
```

produces:

```text
MISMATCH
```

Both executions completed successfully, but their normalized outputs differ.

## Compatibility results

A matrix returns a `CompatibilityResult`.

Current compatibility statuses are:

| Status | Meaning |
| --- | --- |
| `COMPATIBLE` | Completed runs produced the same normalized output |
| `MISMATCH` | Completed runs produced different normalized output |
| `UNAVAILABLE` | At least one requested model was unavailable |
| `EXECUTION_FAILURE` | At least one run failed, timed out, or was cancelled |

Each individual run also has a `RunStatus`:

```text
COMPLETED
FAILED
UNAVAILABLE
TIMED_OUT
CANCELLED
```

Execution failures and behavioral mismatches are intentionally separate.

## JUnit Jupiter

`modelmatrix-junit` integrates the core execution path with JUnit Jupiter.

```java
import com.modelmatrix4j.core.model.ModelDescriptor;
import com.modelmatrix4j.core.model.ModelUnderTest;
import com.modelmatrix4j.core.result.CompatibilityResult;
import com.modelmatrix4j.core.result.CompatibilityStatus;
import com.modelmatrix4j.core.scenario.Scenario;
import com.modelmatrix4j.junit.ModelMatrixSource;
import com.modelmatrix4j.junit.ModelMatrixTest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GreetingCompatibilityTest implements ModelMatrixSource {

    @Override
    public Scenario scenario() {
        return new Scenario(
                "greeting",
                "Say hello"
        );
    }

    @Override
    public List<ModelUnderTest> models() {
        return List.of(
                new ModelUnderTest(
                        new ModelDescriptor("model-a"),
                        ignored -> "Hello world"
                ),
                new ModelUnderTest(
                        new ModelDescriptor("model-b"),
                        ignored -> "Hello world"
                )
        );
    }

    @ModelMatrixTest
    void modelsAreCompatible(CompatibilityResult result) {
        assertEquals(
                CompatibilityStatus.COMPATIBLE,
                result.status()
        );
    }
}
```

The JUnit extension executes the configured matrix before the test method and injects the resulting `CompatibilityResult`.

JUnit does not contain model-provider-specific execution logic.

## ModelAdapter

`ModelAdapter` is the boundary between ModelMatrix4J and an actual model implementation.

```java
@FunctionalInterface
public interface ModelAdapter {

    String invoke(Scenario scenario) throws Exception;
}
```

For deterministic tests, an adapter can be a lambda:

```java
scenario -> "expected response"
```

M3 will use the same boundary for Spring AI:

```text
Spring AI
    |
    v
SpringAiModelAdapter
    |
    v
ModelAdapter
    |
    v
ModelMatrix
```

Provider-specific code stays outside `modelmatrix-core`.

## Architecture

The repository currently contains two Maven modules:

```text
modelmatrix4j
├── modelmatrix-core
└── modelmatrix-junit
```

Dependency direction is:

```text
modelmatrix-junit
        |
        v
modelmatrix-core
```

Core does not depend on JUnit.

The main core execution path is:

```text
ModelMatrix
    |
    v
MatrixExecutor
    |
    v
ModelExecution
    |
    v
InvocationRunner
    |
    v
ModelAdapter
```

Execution produces internal outcomes that are used separately for compatibility evaluation and safe public result mapping:

```text
ExecutionOutcome
      |
      +----> CompatibilityEvaluator
      |
      +----> RunResultMapper ----> RunResult
```

`ModelMatrix` is the public facade. Most execution implementation types remain package-private.

More detailed architecture decisions are documented in [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md).

## Safe results

`RunResult` represents one model execution result.

It contains the M2 public result data:

- run identity
- scenario identity
- model descriptor
- repetition index
- terminal status
- normalized textual output when completed
- execution duration
- bounded diagnostic information

Compatibility comparison happens before lossy public redaction.

This matters because two different internal outputs such as:

```text
token=abc
token=xyz
```

must still produce:

```text
MISMATCH
```

even if both public outputs are sanitized to:

```text
token=[REDACTED]
```

Raw provider payloads are not part of the M2 core result contract.

## Concurrency

Different declared models may execute concurrently.

Repetitions of the same declared model execute sequentially.

A matrix also has a bounded number of physical model invocations.

Timeouts include time spent waiting for physical invocation capacity.

If an adapter ignores interruption after a timeout, its physical invocation continues to hold its capacity until the adapter actually exits.

A timed-out repetition prevents later repetitions of that same declared model from starting. Other models continue independently.

Admission state belongs to one matrix and is not global.

## Build

Requirements:

- Java 25
- Maven Wrapper included in the repository

Run the full default verification:

```bash
./mvnw -B verify
```

Windows:

```text
mvnw.cmd -B verify
```

The default verification path does not require:

- Ollama
- Docker
- PostgreSQL
- cloud credentials
- paid providers
- external runtime services

Normal Maven dependency resolution may still use configured artifact repositories.

## Project principles

The current implementation follows a few deliberate constraints:

- core remains provider-neutral and framework-neutral
- no hidden model retries
- scenarios describe execution, not assertions
- assertions do not invoke models
- external integrations are opt-in
- public API stays small
- internal implementation details do not automatically become extension points
- abstractions are added when a concrete milestone requires them

## Roadmap

The next milestone is **M3 — Spring AI + Ollama**.

M3 will add an optional Spring AI module and prove the full path with one scenario executed against two local Ollama model configurations.

Later milestones cover:

```text
M4  Structured output + Java tool calling
M5  RAG + pgvector
M6  MCP
M7  Provider matrix + reporting
M8  OSS release hardening
```

See [`docs/ROADMAP.md`](docs/ROADMAP.md) for the full milestone plan.