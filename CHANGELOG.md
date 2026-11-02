# Changelog

## 0.3 - Unreleased

Not published or approved for release. The maintainer reports manual checks passed
on November 2, 2026; the licensing decision remains a release gate. See
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
- Licensing is unresolved: `LICENSE` contains GPL v3 text, while `LICENSE.txt`
  and loader descriptors declare all rights reserved. These notes do not grant
  rights or select a license. Maintainer direction is required before release.
- Snapshot build plugins remain in use; dependency resolution is not fully
  immutable. No release date or successful publication is claimed.
