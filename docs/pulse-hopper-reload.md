# Pulse Hopper reload regression (issue #4)

## Fix and scope

Every call to `PulseHopperBlockEntity.loadAdditional` now sets `powerInitialized` to false, regardless of the saved latch. The first actual observation through `canCollect` or `powerChanged` initializes from current neighbor power without attempting output. This also works if the first collection check is blocked by its input cooldown. Subsequent neighbor-observed rising edges use the unchanged output path.

The existing latch NBT keys remain serialized for compatibility; a saved latch is not evidence of current power. Inventory, last-attempt game time, and the independent input cooldown still load normally. In particular, reloading does not clear the same-tick attempt guard. No transfer logic, other hopper, registration, translation, resource, dependency, or shared build file is changed by this fix.

Changed files for this issue:

- `common/src/main/java/me/Wikos/hoppersadditions/BlockEntity/Hoppers/PulseHopperBlockEntity.java`: one-line initialization reset.
- `tools/pulse/PulseReloadRegression.java`: standalone regression main; no new test framework.
- `tools/pulse-regression.gradle`: opt-in init script, not part of normal builds or distributable JARs.
- `docs/pulse-hopper.md`: corrected persistence contract and link to this evidence.
- `docs/pulse-hopper-reload.md`: reproduction, validation, and remaining checks.

There were no local changes at initial inspection. Unrelated edits appeared during execution in `README.md`, `gradle.properties`, `fabric/src/main/resources/fabric.mod.json`, and `neoforge/src/main/resources/META-INF/neoforge.mods.toml`. They were left untouched. The successful loader build includes the checkout at build time, including the concurrent v0.3 metadata; it is not an isolated feature-only build.

## Reproducible isolated regression

Use the README's build JVM and existing wrapper. On this machine:

```powershell
$env:JAVA_HOME = "$env:USERPROFILE\.jdks\corretto-23.0.2"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
.\gradlew.bat -I tools/pulse-regression.gradle :common:pulseRegression --console=plain
```

The harness compiles against the common module's Minecraft runtime classpath and invokes the production NBT, power-change, collection eligibility, and item-transfer methods. It uses real Minecraft ItemStacks and barrel containers, but a fake Level with explicitly controlled game time/power. Unsafe allocation bypasses loader registration and constructors, and the test hopper suppresses `setChanged` world notifications. Private methods are accessed reflectively. No chunk manager, redstone engine, client, dedicated server, loader lifecycle, comparator feedback, or actual item-entity pickup is exercised. Cooldown assertions call the production collection-completion method directly, not a live collection simulation. Unsafe warnings are expected and confined to the harness.

Before the production edit, against HEAD `42dca8a60b3ff60ea388095d1398480703e6c32a`:

- Running with `-PpulseExpectHistoricalFailure` reproduced the exact lost-edge sequence: stock 8, save powered, load with actual power false, call collection eligibility without a falling callback, then send the next real rising edge. Destination remained 0 and source remained 8 instead of delivering one.
- The same run reproduced saved-unpowered -> load powered -> first powered neighbor callback: a phantom item was delivered.
- The normal regression command failed with `AssertionError: first observation resynchronizes saved=false actual=true` before the fix.
- Local ignored logs: `build/pulse-issue4-before.log` and `build/pulse-issue4-red.log`.

To repeat the historical reproduction, copy the two feature-specific tools into a disposable source copy of that baseline and append `-PpulseExpectHistoricalFailure` to the command above. That option asserts the historical bugs and must fail against the corrected implementation; it is not a fix-validation mode. The normal command is the permanent regression check and fails against the old implementation.

After the fix, the normal command passes **71 assertions**, including:

- All saved/current power combinations, with either collection eligibility or a neighbor callback observing power first; no immediate or delayed unchanged-power phantom, and the next genuine rising edge delivers exactly one of eight items.
- Repeated constant-powered NBT round trips, powered-placement initialization, and loading into an already initialized entity.
- Individually missing latch keys, all Pulse keys missing, and absent timing defaults.
- Duplicate callbacks, multiple edges in one tick, preserved attempt stamp across a same-tick reload, and delivery on a later tick.
- Empty and full-destination failures consume the attempt; stock/capacity restoration and same-tick re-edges do not retry or queue delivery. Source/destination counts remain exact on rejection and successful delivery.
- Preserved input cooldown, baseline initialization during cooldown, output independent of that cooldown, and collection eligibility exactly at eight ticks.

Final isolated result: `PASS: 71 assertions against production pulse/NBT/transfer methods and real Minecraft containers; fake level, no loader or gameplay session.` Local ignored log: `build/pulse-issue4-regression.log`.

## Build proof

Corretto JDK 23.0.2 and the checked-in Gradle 8.12.1 wrapper were verified with `java -version` and `.\gradlew.bat --version`. The Java bytecode target remains 21.

```powershell
.\gradlew.bat :common:build :fabric:build :neoforge:build --console=plain
```

The documented command succeeded: **31 actionable tasks, 12 executed, 19 up-to-date**. Local ignored log: `build/pulse-issue4-build.log`. Existing Gradle deprecation notices remain. Normal Gradle test tasks still have no test sources; the regression main runs only with the opt-in script.

Build success proves compilation and packaging, not live chunk behavior or gameplay on Fabric/NeoForge.

## Unverified live acceptance: both loaders separately

- [ ] In a disposable world, save a stocked hopper powered, unload it, change the power source while the hopper remains unloaded, then load it unpowered without a falling-edge callback. Record source/destination counts and callback ordering. After its first observation, the next real rising edge must move exactly one item (8/0 -> 7/1).
- [ ] Repeat saved-unpowered -> load powered with tick-first and callback-first ordering where possible. Hold power through later ticks/notifications: source/destination stay 8/0; a later real off/on edge moves one item.
- [ ] Constant-power save/reload, actual chunk unload/reload, dedicated-server restart, and powered placement, including preloaded inventory: no synthesized output. The harness's NBT round trips are not proof of these lifecycle events.
- [ ] In a disposable saved world, omit Pulse NBT fields and verify safe baseline initialization. Exercise empty/full/missing/sided-rejecting targets, restore stock/capacity under constant power, and count all items.
- [ ] Verify multiple same-tick edges and comparator feedback cannot duplicate output. Verify real chest/item-entity input retains its independent eight-tick cooldown and immediate subsequent output remains allowed.

These live checks remain separate v0.3 milestone work. No commit, merge, push, issue comment, or issue closure is performed for this implementation.
