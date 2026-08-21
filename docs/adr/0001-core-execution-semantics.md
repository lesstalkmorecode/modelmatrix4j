# ADR-0001: Core execution semantics

**Status:** Accepted

## Context

ModelMatrix4J compares repeated executions across multiple model configurations.

Several cross-cutting behaviors affect execution, compatibility classification, and the public result contract. These semantics must remain stable regardless of provider integration.

This ADR fixes the rules for:

- compatibility status precedence
- repetition and timeout behavior
- comparison and diagnostic security boundaries

## Decision

### 1. Compatibility status precedence

A matrix result is classified using this precedence:

```text
EXECUTION_FAILURE
    >
UNAVAILABLE
    >
MISMATCH
    >
COMPATIBLE
```

Rules:

- Any failed, timed-out, or cancelled run makes the matrix `EXECUTION_FAILURE`.
- If there is no execution failure but at least one model is explicitly unavailable, the matrix is `UNAVAILABLE`.
- If all required executions complete but normalized outputs differ, the matrix is `MISMATCH`.
- Otherwise the matrix is `COMPATIBLE`.

`MISMATCH` represents behavioral disagreement between successful executions.

Infrastructure or execution failures must never be reported as behavioral mismatches.

### 2. Repetition semantics

Repetitions belong to one declared model configuration.

For a given model configuration:

- repetitions execute sequentially
- repetition indexes preserve declaration order
- one failed or unavailable repetition does not automatically suppress later repetitions
- a timed-out repetition suppresses later repetitions for that same model configuration

Different model configurations may execute concurrently.

A timeout covers the complete invocation lifecycle, including waiting for physical invocation capacity.

If the underlying adapter ignores interruption after timeout, the physical invocation continues to consume its execution permit until it actually exits.

No hidden retries are performed by ModelMatrix4J.

### 3. Comparison and security boundary

Compatibility evaluation operates on internal normalized output before public redaction.

The order is:

```text
raw adapter output
        |
        v
normalization
        |
        +----> compatibility evaluation
        |
        v
public result mapping
        |
        v
redaction / diagnostic bounding
        |
        v
RunResult
```

This is intentional.

For example:

```text
token=abc
token=xyz
```

must remain a `MISMATCH` even if both public values later become:

```text
token=[REDACTED]
```

Public `RunResult` values must not expose known sensitive diagnostic values.

Diagnostics are:

- redacted at the public result boundary
- bounded in length
- not used as compatibility inputs

Raw provider payloads are not part of the core public result contract.

## Provider integrations

Provider integrations must adapt into these semantics rather than redefine them.

In particular, provider-specific exceptions must not be classified as `UNAVAILABLE` merely because a request failed.

`UNAVAILABLE` requires an explicit, reliable availability signal.

If an integration cannot reliably distinguish unavailability from another provider or transport failure, it must allow the failure to be classified as `EXECUTION_FAILURE`.

Provider adapters must not add hidden retries that violate the one-invocation semantics of the core execution model.

## Consequences

These rules make status classification deterministic and provider-neutral.

They also ensure that:

- execution failures cannot masquerade as behavioral differences
- timeout behavior remains safe under non-cooperative adapters
- public sanitization cannot change compatibility decisions
- provider integrations cannot silently weaken core execution guarantees

Future milestones may introduce new capability-specific results, but changing these core semantics requires an explicit architecture decision.