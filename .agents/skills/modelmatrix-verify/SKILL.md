---
name: modelmatrix-verify
description: Verify the reconciled ModelMatrix4J workspace with scope inspection and the single canonical Maven command.
---

# ModelMatrix verify

1. Confirm prerequisites and review are reconciled and inspect `git status --short` plus the complete diff for unauthorized product, package, dependency, module, or credential changes.
2. Run the one canonical verification command from the repository root:

   ```text
   ./mvnw -B verify
   ```

3. Run `git diff --check` as handoff hygiene. Do not represent it as a second build command.
4. Report exact results and any blocker. Do not weaken checks, commit, or push without explicit authorization.
