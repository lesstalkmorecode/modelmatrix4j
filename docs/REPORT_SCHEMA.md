# Report schema

ModelMatrix4J report schema `1` is an explicit persistence contract, independent of the library artifact version.

## Machine-readable shape

The JSON object contains fields in this order:

1. `schemaVersion`
2. `status`
3. `runs`

Each run contains fields in this order:

1. `runId`
2. `scenarioId`
3. `configurationId`
4. `repetition`
5. `status`
6. `durationNanos`

Run order is preserved exactly from `CompatibilityResult.runs()`; the reporter never sorts or re-groups executions.

Schema enum values are owned by the report module (`ReportCompatibilityStatus` and `ReportRunStatus`), not by core enums. `ReportProjector` maps core values exhaustively into this stable vocabulary so adding or changing a core enum value cannot silently extend schema 1; the mapping must be reviewed deliberately.

## Security boundary

Schema 1 intentionally has no model-output or diagnostic field. `RunResult.output()` and `RunResult.diagnostic()` are not projected, even when sanitized for in-process use. Capability-local evidence is also outside this schema.

Adding model output is not a compatible schema-1 extension. It requires explicit opt-in and a separately reviewed persistence/security policy.

The persistence rationale is summarized in [`ARCHITECTURE.md`](ARCHITECTURE.md) and [`../SECURITY.md`](../SECURITY.md).

## Compatibility policy

Within schema version `1`:

- enum spellings and existing field meanings are stable;
- field and run ordering are deterministic;
- removing or renaming a field is incompatible;
- changing units or identity semantics is incompatible;
- adding a required field is incompatible;
- any incompatible change requires a new schema version.

Writers emit only the current schema. Consumers should reject unsupported schema versions rather than guessing semantics.

## CI usage

Generate the report from an already completed `CompatibilityResult`, write the returned JSON string to a workspace file, then persist that file with the CI system's normal artifact mechanism. Report generation itself performs no model invocation, network request, database access, or artifact upload.

Human-readable summaries are generated from the same `CompatibilityReport` projection and therefore inherit the same output-exclusion policy.
