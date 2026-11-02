# Contributing


Use GitHub issues and pull requests for organization. Keep `master` as the integrated baseline; use one short-lived branch per issue, for example `feat/8-hopper-name` or `fix/9-transfer-edge`.

For concurrent implementation, use **a branch plus a separate worktree per thread**, not several agents switching branches in the same folder. A branch separates commit history; a worktree separates working files. In T3 Code, choose **New worktree**, base it on the latest integrated `origin/master`, and give it the issue branch. Fetch first and verify the intended starting commit. Uncommitted changes in another checkout are not included. For sequential small fixes, one checkout and one feature branch are enough.

Each feature owns its feature-specific classes/resources. Coordinate shared registration, translations, mining tags, version files, and build infrastructure explicitly; separate worktrees do not eliminate merge conflicts. Keep this lightweight: one issue, one feature thread/branch, one reviewed PR. No extra project-management service or permanent integration branch is needed.

### Feature-thread starter prompt

Create the branch/worktree through T3 Code before sending this prompt; replace the issue number and branch name:

```text
Implement GitHub issue #N in DarquesTheo/HoppersAdditions.
Read the full live issue, its comments, README.md, and applicable AGENTS.md before editing.
Verify this thread is in its isolated worktree on branch feat/N-short-name, not master.
Check the starting commit and existing changes; preserve unrelated work.
If the workspace or branch is wrong, stop and report it rather than switching a shared checkout.
Use the documented build JDK and Gradle wrapper; keep changes scoped to the issue.
Coordinate shared registration, translations, tags, version, and build-file edits before changing them.
Run both loader builds, relevant regressions, and artifact checks; distinguish build/harness results from live gameplay.
Commit, push only the issue branch, and open a PR targeting master only when explicitly authorized.
Reference the issue in the PR; do not merge, push master, or close the issue yourself.
Finish with the branch/commit, changed files, validation results, PR if authorized, and remaining manual checks.
```

After feature work, use one integration pass to review PRs, reconcile shared-file edits, and merge approved work serially. Rebuild and validate the combined mod after integration; independent successful builds do not prove combined farm behavior. Close issues only when their acceptance criteria are met, and keep release blockers open separately.
