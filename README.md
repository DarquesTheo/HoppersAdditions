# Hoppers Additions

A Minecraft 1.21.1 hopper mod with shared Architectury code and Fabric and NeoForge builds.

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
- There are currently no automated test sources. Build success proves compilation/packaging, not farm behavior, redstone, item conservation, or live gameplay on either loader.

## Parallel feature work

Each feature owns its hopper-specific classes/resources. Shared registration, translations, and mining tags need coordinator integration. Do not change build infrastructure independently in each feature branch.

New worktrees start from committed source. Start all three feature worktrees from the local `master` baseline containing this setup, not an older `origin/master`. A local commit is not automatically pushed to GitHub. Commits/pushes require user authorization.

### Starting the feature threads

Create one separate worktree thread for each GitHub issue in `DarquesTheo/HoppersAdditions`: #1 Splitting, #2 Overflow, and #3 Pulse. Do not run three implementation chats in the same working directory.

Use this prompt, replacing the issue number:

```text
Implement GitHub issue #N in DarquesTheo/HoppersAdditions in this worktree.
Read the full live issue and README.md before editing.
Use the documented build JDK and Gradle wrapper.
Treat the issue baseline/tooling note as historical; the wrapper now exists.
Keep changes scoped to this feature and report any shared-file edits.
Run the documented loader builds and distinguish build proof from live gameplay proof.
Do not merge, push, or close the issue without authorization.
Finish with changed files, validation results, and any unverified acceptance checks.
```

After all three implementations finish, use one integration thread to reconcile shared-file edits, combine the branches, build the combined mod, and validate the acceptance scenarios. Three independently successful builds do not prove that the combined features work.
