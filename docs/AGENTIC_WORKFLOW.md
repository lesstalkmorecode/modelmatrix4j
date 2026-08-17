# ModelMatrix4J multi-agent engineering workflow

This document defines how multiple agents collaborate on an approved roadmap milestone. It governs coordination; it does not approve a milestone, change the architecture, or expand product scope. The roadmap, product specification, architecture, domain model, test strategy, ADRs, and quality gates remain authoritative.

## Operating principles

1. Work only within the currently approved roadmap milestone. The orchestrator coordinates that milestone; it does not approve product scope and must not expand the milestone. Agents must not implement later-milestone functionality, infrastructure, or speculative public APIs.
2. When work is designated multi-agent, the orchestrator must delegate the defined roles to actual sub-agents. It must not simulate the Architect, Reviewer, or Test/adversarial reviewer within its own context and represent that as multi-agent work.
3. The orchestrator is the single coordination point. It assigns file ownership, resolves overlaps, accepts handoffs, and decides whether proposed architectural changes return to design work.
4. Two agents must not concurrently edit the same file. Writable file ownership is exclusive until the owning agent completes or explicitly returns its scope to the orchestrator.
5. Read-only agents may inspect files owned by a writer, but their findings are advisory until the orchestrator reconciles them against the writer's latest handoff.
6. Every delegated task uses the delegation contract below. An agent must stop and return to the orchestrator if the requested outcome cannot be achieved within that contract.
7. Passing tests does not authorize scope expansion or an architectural exception.

## Roles

### Orchestrator

The orchestrator coordinates the currently approved roadmap milestone and owns final verification. It:

- reads the approved milestone and the relevant specification, architecture, domain, test-strategy, ADR, and quality-gate documents;
- decomposes the milestone into narrow tasks with non-overlapping writable scopes;
- delegates each task using an explicit objective, allowed files, forbidden files, and acceptance criteria;
- tracks exclusive file ownership and sequences work when tasks would touch the same files;
- reconciles implementation, architecture advice, review findings, and adversarial test findings;
- routes architecture questions and requested scope changes back through design and, when consequential, the ADR process;
- decides which findings must be fixed within the approved milestone; and
- runs or directly supervises final verification and reports its evidence.

Delegation does not transfer responsibility for scope control or completion. The orchestrator must inspect the integrated result rather than treating agent success reports as sufficient evidence.

### Architect

The architect is read-only. It:

- proposes the smallest design that satisfies the delegated use case and current milestone;
- checks dependency direction, public API pressure, provider/framework isolation, and consistency with existing decisions;
- identifies design risks, invariants, and the tests needed to exercise them; and
- clearly labels any proposal that changes existing architecture or requires an ADR.

The architect does not modify files, implement code, or independently approve architecture changes. Its output returns to the orchestrator for a decision.

### Implementer

The implementer receives a narrow, exclusive writable scope. It:

- changes only the allowed files necessary to meet the stated objective;
- preserves architecture and behavior outside that scope;
- adds or updates tests required by the acceptance criteria when those test files are allowed;
- runs the relevant focused checks; and
- returns a handoff describing changed files, verification performed, assumptions, and remaining risks.

The implementer must not change architecture, edit forbidden files, claim additional file ownership, or broaden the milestone. If any of those becomes necessary, it stops that part of the work and asks the orchestrator to revise the delegation.

### Reviewer

The reviewer is read-only and independent from the implementer whose work it reviews. It evaluates the actual diff and relevant surrounding code against the delegation contract, repository documentation, and approved milestone. Findings use these severities:

- **BLOCKER:** The change is unsafe to accept or cannot satisfy a required milestone outcome or executable quality gate.
- **MAJOR:** A correctness, architecture, scope, compatibility, or test-coverage defect that should be resolved before completion.
- **MINOR:** A localized issue that does not invalidate the milestone outcome but has a concrete maintainability, clarity, or robustness cost.

Each finding identifies the affected file and location, explains the impact, and recommends a bounded correction. The reviewer does not modify files or silently negotiate scope with the implementer; findings return to the orchestrator for reconciliation. If there are no findings, it reports that explicitly and names any residual risks or checks it could not perform.

### Test/adversarial reviewer

The test/adversarial reviewer searches for missing edge cases and violations of documented invariants. It challenges happy-path assumptions with malformed inputs, boundary conditions, ordering and repetition, terminal-state behavior, isolation, failure classification, redaction, and deterministic execution as applicable to the approved milestone.

It reports uncovered risks, proposed test cases, and evidence from tests it was authorized to run. It is read-only with respect to production code and does not modify production files unless the orchestrator explicitly issues a new writable delegation. Any permission to add tests must name the allowed test files and must not overlap another agent's active writable scope.

## Delegation contract

Before an agent begins delegated work, the orchestrator records all of the following:

| Field | Required content |
| --- | --- |
| Task ID | A stable identifier used by the ledger, dependencies, handoffs, and reconciliation record. |
| Agent identity | The actual agent or delegation identity in addition to its role label. |
| Objective | One bounded outcome tied to the approved milestone or review phase. |
| Allowed files | Exact files or the narrowest practical path set the agent may modify; use `none (read-only)` for non-writing roles. |
| Forbidden files | Files or areas that must not change, including adjacent architecture or later-milestone work. |
| Acceptance criteria | Observable conditions, required tests or review output, and the expected handoff evidence. |
| Dependencies | Stable task IDs that must be reconciled before this task starts; use `none` when independent. |

The contract also names relevant source documents and points to its recorded location in the execution plan. Ambiguous writable scope is not permission to edit. Newly discovered files remain forbidden until the orchestrator explicitly revises ownership.

### Package and path ownership

Package and path ownership is a temporary task lease, never permanent authority attached to an agent or role. Documented or conditional packages are not pre-authorized writable scope, and source structure follows architectural responsibilities rather than agent roles.

- Prefer exact files. When files do not yet exist or a bounded path is necessary, the contract states whether it is recursive and names exclusions; a broad module root is not allowed when a narrower scope is knowable.
- No file is shared writable. Exact-file overlap and broad/narrow path overlap both require sequential work: the first owning task completes and returns its full scope, the orchestrator reconciles and records release, and only then may another task receive it.
- A task does not release only part of its scope. Work that must become independently releasable is delegated under separate stable task IDs, each with its own contract, handoff, reconciliation, and completion.
- A cohesive cross-package change may receive one explicit set of exact files when splitting it would require an invalid intermediate contract. Otherwise work is sequenced inward-to-outward: foundational public contracts, dependent results/execution, internal mechanics, then JUnit integration.
- Production files and their directly corresponding tests may share one bounded task. A separately delegated test owner is read-only over production and starts only after the production task is completed and reconciled.

A public or protected signature, shared API file, or cross-package dependency change is a shared-contract event. The agent stops before expanding scope and asks the orchestrator to record the reason, required files/paths, current owners, dependency and API impact, proposed sequencing, and changed acceptance/review needs. The revised task updates the public-API inventory with each exposed element, its current-milestone use case and consumer, signature dependencies, consumer-test evidence, owner, and independent-review disposition. The implementer and API reviewer must have different actual identities.

Conditional examples, valid only after a concrete package and files are approved, include exact scenario files and tests under `core/scenario`, descriptor files and tests under `core/model`, result files and tests under `core/result`, execution files and tests under `core/execution`, responsibility-named internal comparison files after result contracts, and exact JUnit public files before dependent lifecycle/resolution mechanics. These examples neither create paths nor permit claiming an entire package.

## Coordination and handoff

Every multi-agent execution plan must contain this agent ledger:

| Task | Agent role | Depends on | Writable scope | Status | Handoff |
| --- | --- | --- | --- | --- | --- |

Each ledger entry records the actual agent/delegation identity with the role and links or unambiguously refers to its delegation contract, returned handoff, and reconciliation evidence. A filled ledger alone does not prove that delegation occurred, that scopes were respected, or that review was independent; the orchestrator verifies those claims against actual handoffs and the integrated workspace.

The orchestrator keeps the ledger updated using these statuses:

- **Planned:** Contract drafted, but not delegated.
- **Delegated:** Assigned to an actual agent; work has not been reported as started.
- **In progress:** The agent is performing the delegated work and holds any writable scope.
- **Handoff returned:** The agent reported results; the orchestrator has not yet reconciled them.
- **Reconciled:** The orchestrator inspected and disposed the handoff; writable ownership is released.
- **Completed:** Acceptance evidence and all required reconciliation are recorded.
- **Blocked:** The contract cannot currently be completed; the blocker and required decision are recorded.

Normal progress follows the statuses in that order, omitting only inapplicable intermediate states. Correction work receives its own task or an explicitly revised contract; statuses are not moved backward to hide prior evidence. A task must not start until every task listed in `Depends on` is `Completed` and its handoff has been reconciled.

Architect and test-planning work may run in parallel. Implementation tasks start only after their required planning dependencies are reconciled. Review and adversarial-review tasks start only after the implementation they review is complete; independent review tasks may run in parallel with each other. Tasks with no dependency relationship may run in parallel only when their writable scopes do not overlap.

Before delegation, the orchestrator verifies that writable scopes do not overlap. Shared files are handled by sequencing: the first owner returns ownership in its handoff, the orchestrator records reconciliation and release, and only then may another agent receive those files.

The implementer and each reviewer of that implementation must have different actual agent/delegation identities. Different role labels assigned to the same agent do not establish independent review.

An agent immediately escalates to the orchestrator when it discovers:

- an architecture decision or reversal is required;
- acceptance criteria conflict with repository documentation;
- required work lies outside its allowed files or the approved milestone;
- another active task owns a file it needs; or
- a quality gate cannot be executed or fails for reasons outside its scope.

At handoff, every agent records its actual identity, task ID, assumptions, limitations, and remaining risks. A writing agent also lists changed files, commands and results, and explicitly returns writable ownership. A read-only agent lists evidence inspected and findings. The ledger points to this evidence rather than replacing it.

Reconciliation records the orchestrator's accepted, rejected, or deferred decisions and reasons, finding dispositions, any scope change, and the ownership release that unlocks dependent work. A returned handoff is not reconciled merely because the agent reported success. The orchestrator resolves BLOCKER and MAJOR findings before completion, and records the disposition of MINOR findings so they are either addressed, consciously accepted, or placed into an approved future scope.

## Completion and final verification

The orchestrator may declare milestone work complete only when:

1. the integrated result remains within the approved milestone and delegated scopes;
2. all acceptance criteria are satisfied;
3. all applicable executable quality gates in `docs/QUALITY_GATES.md` pass on the reconciled workspace;
4. required focused and regression tests pass without prohibited external services or credentials;
5. BLOCKER and MAJOR review findings are resolved and the disposition of MINOR findings is recorded; and
6. applicable manual gates are checked and reported, while deferred gates remain deferred rather than being represented as passed.

Final verification belongs to the orchestrator and must run after reconciliation, not only in isolated agent workspaces or before the final set of edits. A failed executable gate means the work is incomplete. If a gate cannot be run, the orchestrator reports the specific blocker and does not claim completion.
