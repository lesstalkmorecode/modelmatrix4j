---
name: modelmatrix-review
description: Run independent ModelMatrix4J review of an integrated diff for correctness, scope, architecture, tests, and adversarial gaps.
---

# ModelMatrix review

If already running as a delegated reviewer, inspect directly and do not redelegate.

Otherwise, after writing is complete, delegate the integrated diff in parallel to different actual `reviewer` and `adversarial-reviewer` agents. Both remain read-only and independent from the implementer.

Require BLOCKER, MAJOR, and MINOR findings with precise locations, impact, and bounded corrections, or an explicit no-findings result. Reconcile all findings; resolve BLOCKER and MAJOR issues before verification and record the disposition of MINOR issues.
