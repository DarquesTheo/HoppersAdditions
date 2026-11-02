# Hoppers Additions

A Minecraft 1.21.1 hopper mod with shared Architectury code and Fabric and NeoForge builds.

## Compatibility

- Includes Splitting, Overflow, and Pulse Hoppers alongside the existing specialized hoppers.
- Both loader descriptors restrict Minecraft to `>=1.21.1 <1.21.2`; Minecraft 1.21.2 and later are not advertised as supported.
- NeoForge is restricted to `>=21.1.220 <21.2`. The minimum matches the pinned build dependency, the reviewed build baseline; earlier 21.1 versions have not been validated. This range is not proof that every later 21.1 patch has been tested in gameplay.
- Fabric requires Loader >=0.18.4, Architectury API >=13.0.8, and Fabric API. The build pins Fabric API 0.116.9+1.21.1.
- NeoForge also requires Architectury API >=13.0.8.
- Repository: https://github.com/DarquesTheo/HoppersAdditions; issue tracker: https://github.com/DarquesTheo/HoppersAdditions/issues.

## Build prerequisites

- Use a full JDK, not only a Java runtime. The project targets Java 21 bytecode. JDK 21 is the baseline; the existing project also builds with Corretto JDK 23.0.2.
- Select a build JVM compatible with pinned Gradle 8.12.1. Use JDK 21 or the documented JDK 23 setup rather than an unvalidated newer JDK.
- Internet access is needed on the first build for Gradle, Minecraft, and mod dependencies.

The included Gradle wrapper avoids needing a separate Gradle installation. The distribution checksum is pinned in `gradle/wrapper/gradle-wrapper.properties`. Existing snapshot plugin declarations remain unchanged, so dependency resolution is not fully immutable.

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
- Verify the normal remapped JARs with `./tools/release/Verify-ReleaseArtifacts.ps1`. Build/package success alone does not prove farm behavior or item conservation.

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for the GitHub issue/PR workflow,
parallel branch/worktree setup, and reusable feature-thread prompt.

## License

Hoppers Additions is licensed under the GNU General Public License, version 3
only (`GPL-3.0-only`). See [LICENSE](LICENSE) for the full license text and
[LICENSE.txt](LICENSE.txt) for the project notice.
