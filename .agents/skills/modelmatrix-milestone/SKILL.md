---
name: modelmatrix-milestone
description: Orchestrate authorized ModelMatrix4J work with real specialized agents, dependency-ordered delegation, independent review, and Maven verification.
---

# ModelMatrix milestone

1. Read `AGENTS.md`, `docs/ROADMAP.md`, and the relevant product and architecture sections. A roadmap milestone or explicit user maintenance request must authorize the work.
2. Delegate independent read-only architecture and test planning to the actual `architect` and `test-planner` agents, then reconcile their advice.
3. Delegate each writing task to an actual `implementer` with an objective, exact allowed and forbidden files, prerequisites, reviewer, and acceptance evidence. Parallel writable tasks require separate Git worktrees and non-overlapping scopes; otherwise writers run sequentially. Worktrees provide filesystem isolation, not architectural permission. Sequence shared or public contract changes through the orchestrator.
4. After implementation, use `$modelmatrix-review` with independent actual reviewers and reconcile every finding.
5. Use `$modelmatrix-verify` on the integrated workspace. Do not commit or push without explicit authorization.

The workflow does not authorize later milestones, product expansion, speculative packages, APIs, or dependencies.
