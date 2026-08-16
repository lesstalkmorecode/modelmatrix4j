# Architecture decision records

Architecture decision records capture decisions that materially affect public API, module boundaries, dependency direction, compatibility, or operational behavior. The specification documents the intended design; ADRs document why a consequential choice was made and whether it supersedes an earlier choice.

## Process

1. Create a numbered file such as `0001-core-structured-value-representation.md`.
2. Include status, date, context, decision, considered alternatives, consequences, and verification plan.
3. Link the ADR from the affected specification section or roadmap milestone.
4. Do not rewrite history when a decision changes; create a superseding ADR and mark the earlier one superseded.

No ADR is required for routine implementation details that do not affect the public architecture. The first expected ADRs concern the Spring AI client boundary for the M3 slice, structured-data representation, compatibility comparison semantics, and report schema versioning; they should be written when those decisions become implementation-blocking.
