# ModelMatrix4J

Compatibility and regression testing for AI models in Java.

ModelMatrix4J runs the same scenario against multiple model configurations and compares their results.

> **Status:** M3 complete  
> **Next:** M4 — Structured output + Java tool calling

## Features

- provider-neutral core
- multiple models per matrix
- repetitions
- timeout and cancellation handling
- deterministic result ordering
- normalized text comparison
- JUnit Jupiter integration
- Spring AI `ChatModel` integration
- opt-in Ollama integration testing
- offline default verification

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

        CompatibilityResult result = ModelMatrix.builder()
                .models(first, second)
                .repetitions(1)
                .timeout(Duration.ofSeconds(1))
                .build()
                .run(scenario);

        System.out.println(result.status());
    }
}
```

The result is:

```text
COMPATIBLE
```

because ModelMatrix compares normalized text output.

## Spring AI

`modelmatrix-spring-ai` adapts Spring AI `ChatModel` to ModelMatrix4J.

```java
import com.modelmatrix4j.core.model.ModelDescriptor;
import com.modelmatrix4j.core.model.ModelUnderTest;
import com.modelmatrix4j.springai.SpringAiModelAdapter;
import org.springframework.ai.chat.model.ChatModel;

public class SpringAiExample {

    public ModelUnderTest createModel(ChatModel chatModel) {
        return new ModelUnderTest(
                new ModelDescriptor("spring-ai-model"),
                new SpringAiModelAdapter(chatModel)
        );
    }
}
```

`SpringAiModelAdapter` only forwards the scenario input to `ChatModel` and returns the raw text response.

Timeouts, normalization, failure classification, timing, and result mapping remain core responsibilities.

## Ollama

M3 includes an opt-in integration test using two local Ollama models.

Default models:

```text
mistral
llama2
```

Prepare Ollama:

```bash
ollama pull mistral
ollama pull llama2
```

Run the integration test:

```bash
./mvnw -B verify -Pollama-it
```

Custom Ollama configuration:

```bash
./mvnw -B verify -Pollama-it \
  -Dollama.baseUrl=http://localhost:11434 \
  -Dollama.model1=mistral \
  -Dollama.model2=llama2
```

The integration test executes the same scenario against both model configurations through:

```text
ModelMatrix
    |
    v
SpringAiModelAdapter
    |
    v
Spring AI ChatModel
    |
    v
Ollama
```

The two models may produce either `COMPATIBLE` or `MISMATCH` depending on their normalized responses.

The Ollama integration test is not part of the default build.

## Modules

```text
modelmatrix4j
├── modelmatrix-core
├── modelmatrix-junit
└── modelmatrix-spring-ai
```

Dependency direction:

```text
modelmatrix-junit -------> modelmatrix-core
modelmatrix-spring-ai ---> modelmatrix-core
```

`modelmatrix-core` has no production dependency on Spring AI or model providers.

## Build

Requirements:

- Java 25
- Maven Wrapper

Run the default verification:

```bash
./mvnw -B verify
```

The default verification path does not require Ollama or other external runtime services.

Run the real Ollama integration path explicitly:

```bash
./mvnw -B verify -Pollama-it
```

## Compatibility results

A matrix returns a `CompatibilityResult`.

Current compatibility statuses:

| Status | Meaning |
| --- | --- |
| `COMPATIBLE` | Completed runs produced the same normalized output |
| `MISMATCH` | Completed runs produced different normalized output |
| `UNAVAILABLE` | At least one requested model was unavailable |
| `EXECUTION_FAILURE` | At least one run failed, timed out, or was cancelled |

## Roadmap

```text
M3  Spring AI + Ollama                         ✅
M4  Structured output + Java tool calling
M5  RAG + pgvector
M6  MCP
M7  Provider matrix + reporting
M8  OSS release hardening
```

See [`docs/ROADMAP.md`](docs/ROADMAP.md) for the full roadmap.