# Hoppers Additions

A Minecraft 1.21.1 hopper mod with shared Architectury code and Fabric and NeoForge builds.

## v0.3 release metadata

- Includes Splitting, Overflow, and Pulse Hoppers alongside the existing specialized hoppers.
- Both loader descriptors restrict Minecraft to `>=1.21.1 <1.21.2`; Minecraft 1.21.2 and later are not advertised as supported.
- NeoForge is restricted to `>=21.1.220 <21.2`. The minimum matches the pinned build dependency, the reviewed build baseline; earlier 21.1 versions have not been validated. This range is not proof that every later 21.1 patch has been tested in gameplay.
- Fabric's existing loader, Java, Architectury, and Fabric API requirements remain unchanged.
- Release licensing remains unresolved: `LICENSE` contains GPL v3 text, while `LICENSE.txt` and both loader descriptors declare all rights reserved. Maintainer direction is required before aligning these declarations; v0.3 metadata work does not choose or change licensing rights.
- Repository: https://github.com/DarquesTheo/HoppersAdditions; issue tracker: https://github.com/DarquesTheo/HoppersAdditions/issues.

## Build prerequisites

- Use a full JDK, not only a Java runtime. The project targets Java 21 bytecode. JDK 21 is the baseline; the existing project also builds with Corretto JDK 23.0.2.
- Select a build JVM compatible with pinned Gradle 8.12.1. Do not use the machine's default JDK 25 with this wrapper.
- Internet access is needed on the first build for Gradle, Minecraft, and mod dependencies.

The included Gradle wrapper avoids needing a separate Gradle installation. The distribution checksum is pinned in `gradle/wrapper/gradle-wrapper.properties`. Existing snapshot plugin declarations remain unchanged, so dependency resolution is not fully immutable.

## Windows / PowerShell

Set `JAVA_HOME` to your installed build JDK. For this machine's existing Corretto installation:

```powershell
$env:JAVA_HOME = "$env:USERPROFILE\.jdks\corretto-23.0.2"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
java -version
.\gradlew.bat --version
.\gradlew.bat :common:build :fabric:build :neoforge:build --console=plain
```

On another machine, substitute its actual JDK path. Do not put a developer-specific JDK path in `gradle.properties`. These environment assignments affect only the current terminal and its child processes. In IntelliJ, select the same JDK as the Gradle JVM.

For a clean rebuild:

```powershell
.\gradlew.bat clean :common:build :fabric:build :neoforge:build --console=plain
```

## Linux / macOS

Select an installed compatible JDK, then run:

```sh
export JAVA_HOME=/path/to/your/jdk
export PATH="$JAVA_HOME/bin:$PATH"
sh ./gradlew :common:build :fabric:build :neoforge:build --console=plain
```

The Unix launcher is included, but this setup has only been validated on Windows.

## Outputs and validation

- Fabric distributable: `fabric/build/libs/hoppers-additions-fabric-<version>.jar`.
- NeoForge distributable: `neoforge/build/libs/hoppers-additions-neoforge-<version>.jar`.
- Use the normal remapped JAR for the matching loader, not a `dev`, `dev-shadow`, or `sources` JAR.
- Ordinary Gradle test tasks have no test sources. Run the opt-in Pulse regression with `./gradlew.bat -I tools/pulse-regression.gradle :common:pulseRegression --console=plain` (71 assertions). It is an isolated harness, not live gameplay.
- Verify the normal remapped JARs with `./tools/release/Verify-ReleaseArtifacts.ps1`. Build/package success alone does not prove farm behavior or item conservation.
- The maintainer reports all manual checks passed on November 2, 2026; this is user-reported acceptance, not an assistant-observed gameplay run. See `docs/v0.3-live-validation.md`.

## GitHub feature workflow

Use GitHub issues and pull requests for organization. Keep `master` as the integrated baseline; use one short-lived branch per issue, for example `feat/8-hopper-name` or `fix/9-transfer-edge`.

For concurrent implementation, use **a branch plus a separate worktree per thread**, not several agents switching branches in the same folder. A branch separates commit history; a worktree separates working files. In T3 Code, choose **New worktree**, base it on the latest integrated `origin/master`, and give it the issue branch. Fetch first and verify the intended starting commit. Uncommitted changes in another checkout are not included. For sequential small fixes, one checkout and one feature branch are enough.

Each feature owns its hopper-specific classes/resources. Coordinate shared registration, translations, mining tags, version files, and build infrastructure explicitly; separate worktrees do not eliminate merge conflicts. Keep this lightweight: one issue, one feature thread/branch, one reviewed PR. No extra project-management service or permanent integration branch is needed.

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

T3 Code's public thread guide documents New worktree as a separate branch and working directory: https://github.com/pingdotgg/t3code/blob/main/docs/user/thread-sidebar.md. This is a documented application workflow, not a claim about Theo's private day-to-day routine.
