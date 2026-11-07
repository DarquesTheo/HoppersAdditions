# Hoppers Additions

A Minecraft 1.21.11 hopper mod with shared Architectury code and Fabric and NeoForge builds.

## Compatibility

- Preserves all twelve variants: Reinforced, Pickaxe, Dropping, Magnetic, Filter, Monster, Animal, Batch, Small Batch, Splitting, Overflow, and Pulse. No new hoppers are added by this port.
- Minecraft **1.21.11 only**, Java **21**. Other game versions are not advertised.
- Fabric: Loader **0.20.1+**, Fabric API **0.141.6+1.21.11 or newer for 1.21.11**, Architectury API **19.0.1** (19.x).
- NeoForge: **21.11.48+ within 21.11**, Architectury API **19.0.1** (19.x).
- Install the matching loader's normal JAR and its separate dependencies; never install both loader JARs together.
- **1.21.11 is the active development target; 1.21.1 releases are legacy.** Parallel feature backports are out of scope.
- Back up 1.21.1 worlds and test disposable copies before upgrading. Preserve the original backup outside the test instance. Downgrade safety is not promised.
- See [migration evidence and release blockers](docs/1.21.11-migration.md). Build/harness success is not release readiness or proof of gameplay/save compatibility.
- Repository: https://github.com/DarquesTheo/HoppersAdditions; issue tracker: https://github.com/DarquesTheo/HoppersAdditions/issues.

## Build prerequisites

- Use a full **JDK 21**, not only a runtime. Validation uses Amazon Corretto **21.0.13+11-LTS** (21.0.13.0.11); all mod bytecode targets Java 21.
- The checked-in wrapper selects **Gradle 9.2.1**. Do not inherit the machine's default JDK or apply Minecraft 26.x prerequisites.
- Internet access is needed on the first build for Gradle, Minecraft, and mod dependencies.

The included Gradle wrapper avoids needing a separate Gradle installation. The distribution checksum is pinned in `gradle/wrapper/gradle-wrapper.properties`. Build plugins are pinned: Architectury Loom 1.14.476, Architectury plugin 3.4.164, and Shadow 9.3.0. Loom labels its 1.14 line beta; no snapshot coordinates are selected. Exact pins and official references are in the migration notes.

## Windows / PowerShell

Set `JAVA_HOME` to your installed compatible full JDK:

```powershell
$env:JAVA_HOME = "C:\path\to\your\jdk"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
java -version
.\gradlew.bat --version
.\gradlew.bat :common:build :fabric:build :neoforge:build --console=plain
```

Replace the example path with your JDK installation. Do not put a developer-specific JDK path in `gradle.properties`. These environment assignments affect only the current terminal and its child processes. In IntelliJ, select the same JDK as the Gradle JVM.

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
- Ordinary Gradle test tasks have no test sources. Run the opt-in Pulse regression with `./gradlew.bat -I tools/pulse-regression.gradle :common:pulseRegression --console=plain`. It is an isolated harness, not live gameplay.
- Release version: `0.4.0+mc1.21.11`; the game target is explicit in both distributable names.
- Verify the normal remapped JARs with `./tools/release/Verify-ReleaseArtifacts.ps1`. Build/package success alone does not prove farm behavior or item conservation.

## Automated world tests

See [GameTest coverage, commands and limitations](docs/gametest.md). Compile the
test-only sources with the documented JDK 21 and wrapper:

```powershell
./gradlew.bat :fabric:gametestClasses :neoforge:gametestClasses --console=plain
```

After reading the Minecraft EULA and explicitly authorizing disposable test
servers, run both loaders with one command:

```powershell
./tools/gametest/Run-GameTests.ps1 -EulaAccepted -ValidateGuards
```

This command fails on required failures, missing/zero project tests and skipped
tests. It does not establish rendering/UI correctness, genuine world reloads,
historical-world upgrades, or release readiness. CI requires explicit maintainer
EULA authorization; it does not silently accept it or skip required world tests.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for the GitHub issue/PR workflow,
parallel branch/worktree setup, and reusable feature-thread prompt.

## License

Hoppers Additions is licensed under the GNU General Public License, version 3
only (`GPL-3.0-only`). See [LICENSE](LICENSE) for the full license text and
[LICENSE.txt](LICENSE.txt) for the project notice.
