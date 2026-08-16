# ModelMatrix4J test strategy

## 1. Principles

Tests are layered by determinism and external cost. Every test states its required services, credentials, network behavior, and parallelism assumptions. Default verification is deterministic and requires no external runtime services or credentials; normal Maven dependency resolution may access configured artifact repositories. The M3 Spring AI + Ollama vertical slice is real-model coverage, but remains explicitly opt-in.

## 2. Test layers

| Layer | What it proves | External services | Default status |
| --- | --- | --- | --- |
| Pure deterministic unit tests | Value objects, scenario expansion, result normalization, comparison, assertion diagnostics, redaction | None | Always on |
| Framework contract tests | Core/model execution contracts using controlled fakes and failures | None | Always on |
| JUnit extension tests | Discovery, parameter resolution, lifecycle, display names, failure mapping, isolation | None; in-memory model | Always on, introduced M2 |
| Spring AI adapter tests | Translation between Spring AI types and the core result contract | Spring runtime; fake/stub model where possible | Always on for the optional module |
| Local Ollama vertical-slice tests | One Spring AI scenario executed against at least two local model configurations and compared | Ollama service and selected local models | Explicit M3 profile/job |
| Structured-output/tool-calling tests | Cross-model structured values, tool selection, arguments, and tool failures | Deterministic fakes; optional Ollama | Deterministic tests on; real models opt-in |
| RAG integration tests | Retrieval behavior and optional pgvector integration | Fixed fixture; PostgreSQL + pgvector/Testcontainers when enabled | M5; database opt-in |
| MCP integration tests | Protocol, tool/resource, session, and transport behavior | In-process fixture or MCP server/container | M6; explicit profile |
| Optional cloud-provider tests | Provider adapter behavior and compatibility observations | Credentials, network, paid quota possible | Never default; protected job |

## 3. MVP vertical-slice test

The decisive M3 test should:

1. define one Spring AI scenario once;
2. resolve two local Ollama model configurations;
3. execute the scenario through the same ModelMatrix4J/JUnit path for both;
4. normalize each outcome into a `RunResult`;
5. produce a compatibility result with per-model status and meaningful comparison facts;
6. assert a stable behavioral contract, not exact natural-language text.

The test is skipped or classified as unavailable when Ollama or a selected model is absent, according to the explicit integration profile. It must not run as part of default verification.

## 4. Reproducibility

- Use fixed scenario inputs, fixture data, model settings, seeds where supported, and explicit repetition counts.
- Record model descriptor, adapter version, configuration identity, timing policy, and test profile in results.
- Do not assert exact natural-language text for real-model tests unless the assertion is deliberately narrow. Prefer structured facts, bounded predicates, tool calls, and capability assertions.
- Distinguish expected model variability from framework defects; variability must be represented in the assertion contract.
- Avoid wall-clock exactness. Use monotonic durations and generous, documented bounds in external tests.
- Pin or document the local models used by the M3 vertical slice; do not imply that every Ollama model provides the same behavior.

## 5. Isolation and parallel safety

- Unit and framework tests must be safe to run in parallel.
- Each external test owns its process/container/session or uses an explicitly thread-safe shared fixture.
- Temporary directories, ports, model state, and credentials are scoped to the test run.
- Matrix parallelism is disabled by default until an adapter documents thread safety and result ordering.
- A timeout or failed external call must be cancelled and cleaned up without poisoning unrelated tests.
- Tests must not mutate a developer's global Ollama model set or a shared production database.

## 6. CI profiles

The intended profiles/jobs are:

1. **default verification**: compile, unit, contract, JUnit extension, and deterministic comparison tests; no external runtime services, Ollama, Docker, PostgreSQL, MCP server, or cloud credentials. Maven artifact resolution may access configured repositories.
2. **local-model**: opt-in M3 Spring AI/Ollama job with availability checks and pinned model/setup instructions.
3. **rag**: deterministic retrieval tests, then opt-in PostgreSQL/Testcontainers tests.
4. **mcp**: deterministic normalization tests, then opt-in server/container tests.
5. **cloud**: manually triggered or protected-secret job, separated by provider and never a required merge check for contributors without credentials.

Testcontainers is a tool for isolated integration tests, not a runtime dependency of core and not a reason to require Docker for default verification.

## 7. Failure classification

Tests and results must distinguish:

- assertion mismatch: execution completed but behavior did not satisfy the contract;
- execution failure: adapter/model call failed;
- unavailable: optional service, adapter, model, or credential is missing;
- timeout/cancellation: execution policy terminated the run;
- invalid configuration: the test itself cannot be meaningfully executed.

Only the first category is a behavioral compatibility failure. The others remain visible and are handled according to profile policy rather than being marked as passes.

## 8. Contract-test matrix

Every adapter should be tested against the smallest shared contract using a fake or local implementation. The contract covers successful text output, malformed/partial output, timeout, cancellation, safe diagnostics, unsupported capability, repeated execution, and concurrency declaration. Provider-specific tests then add translation cases without weakening the common contract.

The M3 Spring AI adapter additionally proves two distinct local configurations reach the same scenario boundary and produce comparable normalized results. Later M4/M5/M6 modules extend the observation contract only when their assertions and adapters exist.

## 9. Coverage and quality signals

Line coverage is a diagnostic, not the sole quality gate. The important gates are deterministic behavior, dependency direction, contract completeness, and absence of credential/network requirements in default verification. A coverage threshold, mutation testing, ArchUnit, or additional static analyzer requires a milestone decision identifying the defect it prevents.
