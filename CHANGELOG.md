# Changelog

## 0.4.0+mc1.21.11 - Unreleased

Compatibility port for issue #8. Not tagged or published; startup, gameplay,
representative saved-world upgrades and artifact evidence are separate release
requirements. See [migration notes](docs/1.21.11-migration.md).

- Target Minecraft 1.21.11 on Fabric and NeoForge through Architectury; preserve
  all twelve existing hopper variants, IDs, recipes and transfer contracts.
- Port block/item registration, value-based saved-state serialization, redstone
  and entity callbacks, enchantability, tooltips, recipe ingredients and item
  model definitions. Keep existing textures/models and custom save keys.
- Pin Architectury API 19.0.1, Architectury plugin 3.4.164, Loom 1.14.476,
  Fabric Loader 0.20.1, Fabric API 0.141.6+1.21.11, NeoForge 21.11.48,
  Shadow 9.3.0, Gradle 9.2.1 and Java 21. No Minecraft 26.x requirements.
- 1.21.11 is the active target; 1.21.1 remains legacy with no parallel feature
  backports. Test backed-up disposable world copies; do not downgrade an
  upgraded world or overwrite the only original world.

## 0.3 - Unreleased

Not published. The maintainer reports manual checks passed on November 2, 2026.
GPLv3 licensing was confirmed on November 3, 2026. See
`docs/v0.3-release-preparation.md` for evidence and the maintainer handoff.

### New hoppers

- **Splitting Hopper:** five slots; outputs one item at a time, alternating strictly
  between horizontal front and bottom, starting forward. Only successful outputs
  advance the saved branch. A missing, full, or rejecting scheduled destination
  applies backpressure: the hopper waits instead of using the other output.
  Successful transfers have an eight-game-tick cooldown. Redstone power stops
  automatic input and output.
- **Overflow Hopper:** five slots; scans source slots in order, trying the front
  first for each candidate item and then the bottom if that same item cannot fit
  forward. Missing/full/rejecting primary inventories permit overflow. At most
  one item succeeds per transfer cycle, followed by an eight-game-tick cooldown;
  it never intentionally delivers to both destinations in one attempt. Redstone
  power stops automatic input and output.
- **Pulse Hopper:** five slots; each real rising redstone edge attempts to output
  one item in the facing direction, at most once per game tick. Falling edges,
  constant power, and repeated notifications are not extra pulses. Empty or
  blocked pulses are consumed: adding items or freeing space while still powered
  does not retry. Automatic collection runs only while unpowered, with independent
  eight-game-tick spacing after successful input; input cooldown does not consume
  an output pulse. Powered placement and first power observation after loading
  establish a baseline without deliberately generating an output pulse.

### Crafting

Each recipe is shapeless and produces one hopper:

| Hopper | Ingredients |
| --- | --- |
| Splitting | 1 vanilla hopper + 1 iron ingot + 1 nether quartz |
| Overflow | 1 vanilla hopper + 1 redstone comparator |
| Pulse | 1 vanilla hopper + 1 redstone repeater |

### Compatibility and dependencies

- Minecraft **1.21.1 only** (metadata excludes 1.21.2 and later), Java **21 or newer**
  for the game. Build with the compatible JDK/Gradle wrapper described in README,
  not the machine's default JDK 25. Both artifacts target Java 21 bytecode.
- Fabric: Loader **0.18.4 or newer**, Architectury API **13.0.8 or newer**, and
  **Fabric API** required. The build pins Fabric API **0.116.9+1.21.1**; the
  descriptor does not impose a Fabric API version minimum.
- NeoForge: **21.1.220 or newer within 21.1** (`>=21.1.220 <21.2`), plus
  Architectury API **13.0.8 or newer**. Earlier 21.1 builds are not validated.
- Install only the normal remapped JAR matching the loader; do not use a common,
  dev, dev-shadow, or sources JAR. Dependencies are separate installations.

### Fixes and known limitations

- The Pulse reload fix resets its power-initialization latch after loading
  so the first observed power becomes the baseline. Isolated regression evidence
  is not proof of chunk reloads, server restarts, or redstone circuits in-game.
- The maintainer reports all manual acceptance checks passed (issue #6). This
  attestation is distinct from assistant-observed runs; per-case logs, counts,
  and exact tested artifact hashes were not supplied. Use backed-up/disposable
  worlds for future validation.
- Accepted loader/dependency ranges are metadata constraints, not a claim that
  every allowed version combination has been tested.
- Removed the stale optional Fabric mod-list icon declaration. No gameplay
  textures, models, recipes, or hopper behavior changed in this metadata cleanup.
- Aligned the project notice and both loader descriptors with GPL version 3
  only (`GPL-3.0-only`), as selected by the maintainer. Both distributables
  include the complete license and project notice.
- Snapshot build plugins remain in use; dependency resolution is not fully
  immutable. No release date or successful publication is claimed.
