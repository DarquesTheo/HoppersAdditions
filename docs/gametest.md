# Minecraft 1.21.11 world tests

## Scope and toolchain

Issue #9 is explicitly authorized to stack on the Minecraft 1.21.11 migration
PR #10 (baseline `80a27bfd5e6616bdce02402532832685968eddd6`). No second port,
production behavior changes, merge, or release publication is part of this work.

Pins inherited from that port: Minecraft 1.21.11, Java 21 (Corretto
21.0.13+11), Gradle 9.2.1, Architectury API 19.0.1/plugin 3.4.164,
Architectury Loom 1.14.476, Shadow 9.3.0, Fabric Loader 0.20.1,
Fabric API 0.141.6+1.21.11, NeoForge 21.11.48. No extra test dependency is
bundled. The Fabric API already supplies its headless test launcher.

Official starting points, checked against the resolved jars and APIs:

- https://docs.fabricmc.net/1.21.11/develop/automatic-testing
- https://docs.neoforged.net/docs/1.21.11/misc/gametest/
- https://github.com/architectury/architectury-loom/tree/1.14

The Fabric Loom DSL described by Fabric is not assumed to exist identically in
Architectury Loom. This project defines its own test-only source set and native
Loom run configuration. Both loaders share vanilla TEST_FUNCTION consumers and
data-driven `minecraft:function` test instances. Fabric registers functions in a
test-only ModInitializer; NeoForge uses its RegisterEvent. No obsolete vanilla
GameTest annotations are assumed. NeoForge's actual userdev `gameTestServer`
template selects `net.neoforged.fml.startup.GameTestServer`; Fabric uses its
`fabric-api.gametest` server properties. Task names are our explicit
`:fabric:runGameTest` and `:neoforge:runGameTest`, not guessed loader defaults.

## Prerequisites and EULA

Select the full JDK 21 using JAVA_HOME as in the README. Use PowerShell 5.1 or
7 on Windows; install PowerShell 7 on Linux/macOS. First dependency resolution
requires network access. Normal builds and test compilation do not start a
Minecraft world and do not accept an EULA.

**Read https://www.minecraft.net/eula. Obtain explicit maintainer authorization
before starting either headless runner.** Fabric's test runner bypasses the normal
EULA prompt; NeoForge's special test main also does not use the ordinary dedicated
server prompt. For that reason the Gradle task and wrapper both fail closed unless
authorization is explicit. Do not infer authorization from a request to compile
or from the absence of an eula.txt file.

## Commands

```powershell
./gradlew.bat :fabric:gametestClasses :neoforge:gametestClasses --console=plain
./tools/gametest/Test-ResultGate.ps1

# Only after explicit EULA authorization:
./tools/gametest/Run-GameTests.ps1 -EulaAccepted -ValidateGuards
./tools/gametest/Run-GameTests.ps1 -Loader fabric -EulaAccepted
./tools/gametest/Run-GameTests.ps1 -Loader neoforge -EulaAccepted
./tools/gametest/Run-GameTests.ps1 -Loader fabric -Filter 'hoppersadditions_gametest:pulse_*' -EulaAccepted

# Equivalent native tasks; still use the report gate afterward:
./gradlew.bat :fabric:runGameTest -PgameTestEulaAccepted=true --console=plain
./tools/gametest/Assert-GameTestReport.ps1 -Report fabric/build/gametest/results.xml
./gradlew.bat :neoforge:runGameTest -PgameTestEulaAccepted=true --console=plain
./tools/gametest/Assert-GameTestReport.ps1 -Report neoforge/build/gametest/results.xml

# Complementary isolated/package checks, no world execution:
./gradlew.bat -I tools/pulse-regression.gradle :common:pulseRegression --console=plain
./gradlew.bat :common:build :fabric:build :neoforge:build --console=plain
./tools/release/Verify-ReleaseArtifacts.ps1
./tools/release/Test-ReleaseArtifacts.ps1
```

On Linux/macOS replace `./gradlew.bat` with `sh ./gradlew`. Invoke the entry
script with `pwsh -File tools/gametest/Run-GameTests.ps1 ...`. The entry script
invokes the Unix Gradle wrapper through sh; no executable-bit workaround is needed.

## Isolation, determinism and diagnostics

- Disposable worlds are below each module's ignored `build/gametest/run`;
  no player save path is accepted by the script. Native test servers replace
  their own test world, never a normal run/client save directory.
- A generated 12 x 8 x 12 empty NBT template plus a stone floor is reset for
  every test. Scenarios place real registered blocks and use their actual server
  tickers, inventories, item entities, redstone notifications and mob entities.
- All tests are required, one attempt, setup one tick, timeout 240 ticks.
  Scheduled assertions use game ticks, not sleeps, and do not succeed until all
  scheduled checks have run. Each JavaExec is bounded to five wall-clock minutes;
  CI jobs are bounded to twenty minutes. No blind retry or optional coverage.
- No global random seed is changed (parallel tests share a server). Pickaxe stone
  drops are deterministic; a **test-only diamond ore loot override** yields two
  diamonds with Fortune II and one without, proving the actual tool enchantment
  is passed into the loot context without probabilistic assertions. This does
  not validate vanilla Fortune's statistical distribution.
- Counts include source, destination and relevant live item entities. When a
  fixture deliberately replaces/removes stacks to stage a blocked path, the
  following expected totals explicitly account for that operation.
- Failures name the assertion, expected/actual quantities and tick. Native XML,
  `<loader>/build/gametest/suite.xml` and `suite.log`, and native run logs
  contain the test ID and engine diagnostics. Keep the first failure; do not
  rerun until green and discard the original evidence.

## Coverage matrix (implementation, not a passing-run claim)

The authoritative discovery list is `tools/gametest/scenarios.json`. The 19
required scenarios map to all twelve variants:

| Variant | Test IDs | Assertions |
| --- | --- | --- |
| Reinforced | reinforced_cooldown; redstone_comparator | Actual vanilla baseline, four versus eight ticks, correct facing, conservation, power gate, comparator |
| Pickaxe | pickaxe_mining; pickaxe_fortune; state_roundtrip | Stone eventually mined, Efficiency faster, bedrock excluded, real tool Fortune in deterministic loot, enchantment persistence |
| Dropping | dropping_components | One entity emitted, components retained, adjacent barrel not inserted into, conservation |
| Magnetic | magnetic_range | Eligible in-range item absorbed, outside item and quantity retained, conservation |
| Filter | filter_reference; filter_blocked | Exact item/component matching, mismatches rejected, reference retained, blocked sided output conservation |
| Monster | monster_categories | Husk moved, cow excluded, held power gates movement, original UUIDs and entity total retained |
| Animal | animal_categories | Cow moved, husk excluded, held power gates movement, original UUIDs and entity total retained |
| Batch | batch_threshold; stack_components | Wait below 64, exact batch, partial-capacity remainder, 16-stack item threshold capped at maximum |
| Small Batch | small_batch_threshold | Wait below eight, exact eight, partial-capacity remainder |
| Splitting | splitting_alternation; sided_components; stack_components | Front/down alternation, both selected blocked paths wait without fallback/turn advance, furnace side rejects/accepts, component equality and limits |
| Overflow | overflow_priority | Front priority, same candidate down fallback, both full retain source, components and quantities |
| Pulse | pulse_edges; pulse_collection | One/rising edge, held power, blocked and empty pulses consumed, unpowered-only input, persisted cooldown and baseline |

Cross-cutting `redstone_comparator` checks all ten inventory-bearing variants
while powered. `state_roundtrip` serializes/restores every variant in an actual
world, seeds nondefault cooldown/turn/enchantment state where applicable, and
checks inventory components and filter prototype via saved tag equality. Mob
hoppers have no custom inventory. Pulse's intentionally reinitialized power flag
is excluded from tag equality and covered behaviorally by `pulse_collection`.

The Batch implementation intentionally accepts a partial batch when the output
has limited capacity; the remainder is restored. Tests preserve this existing
contract rather than imposing an unapproved all-or-nothing mechanic.

### Confirmed production defect; not silently fixed

Source inspection found that Filter's `ejectFiltered` keeps a reference to its
original stack, calls `removeItem(slot, 1)` and puts the now-shrunken original
back when the target rejects the item. Unlike the base hopper's rollback, it
does not restore the previous count. `filter_blocked` is a required reproducer:
three named diamonds in slot zero, matching reference in slot four, east-facing
into the side of an empty furnace; wait twelve ticks, expect three plus the
prototype and zero diamonds in the furnace. Do not lower that expectation or
change production code without separately authorized bug-fix scope. An isolated
regression using the production method and real containers confirms the loss
(`source=2`, `prototype=1`, `target=0`, `moved=false`). That isolated test uses
a fake level. The in-world `filter_blocked` test also fails on Fabric and
NeoForge: the source count is zero instead of three after twelve ticks. The
failure is tracked separately as issue #11; production behavior is unchanged.

## Failure and discovery guards

The wrapper requires fresh XML, every manifest-selected project ID exactly once,
no failures/errors/skips, and a successful native process. It fails for zero
selected IDs, zero discovered project tests, missing IDs, malformed/missing
reports and nonzero native exit status. Stale results.xml is removed before each
attempt; previous normal results are copied to suite.xml before guard probes.

`-ValidateGuards` first runs the normal suite. Only after it passes, it launches
one temporary controlled assertion failure using a process-local property and
requires both nonzero native status and XML containing that assertion. Then it
launches a nonexistent test selector and requires a discovery diagnostic and a
failed wrapper gate. The property is never set in normal runs; no failing test
instance or temporary source edit is committed. Both probes are mandatory in CI
after the normal suite passes. If a real test fails first, guard execution is
unverified, not reported passed. `Test-ResultGate.ps1` separately checks the
report parser with synthetic valid/invalid reports; it is not server evidence.

## CI and package boundaries

The workflow uses safe pull_request/push events, contents:read permission,
independent Fabric/NeoForge jobs and no repository secrets. A maintainer must
explicitly authorize server execution by setting repository variable
`HOPPERS_GAMETEST_EULA_ACCEPTED` to the exact value `true` after reading the
EULA. Until then the world-test step fails with a clear blocker, not a green
skip. This change does not set the variable on the maintainer's behalf.

Normal JARs do not consume gametest outputs. The release verifier rejects test
classes, fixtures, registries and test dependencies, including nested JAR
entries. Its corruption fixtures explicitly inject each kind of leak into both
loaders. Build/package checks and report parser fixtures run before the EULA
gate. XML/log/crash artifacts upload with always(), including failure runs;
whole test saves are not uploaded. Workflow configuration is not hosted CI proof.

## Explicit remaining acceptance boundaries

- Serialization/replacement **is not** actual chunk unload/reload or a complete
  server save/restart. Those operations, especially preserving partially mined
  progress and Pulse's timeline after a full restart, remain unautomated here.
  GameTest fixtures are kept loaded by the test engine; do not claim unload by
  merely replacing a block entity or re-reading an NBT tag.
- The scenarios do not exhaust all enchantment levels, vanilla loot randomness,
  large farms/chains/minecarts, all entity species, rotations or chunk boundaries.
- Tests use development classpaths, not the final packaged dedicated-server
  launch. Normal packaged startup/client connection remains separate acceptance.
- UI/rendering, recipes in client screens, and upgrading disposable copies of
  backed-up 1.21.1 saves remain separate from automation of gameplay logic.
- Per-loader execution, actual hosted CI state, guard execution, and production
  artifact results must be recorded separately in the PR validation receipt.
