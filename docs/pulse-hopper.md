# Pulse Hopper (issue #3)

ID: `hoppersadditions:pulse_hopper`. Shapeless recipe: one vanilla hopper and one repeater produce one Pulse Hopper. Five inventory slots; vanilla downward/horizontal placement, collection volume, hopper menu, comparator fullness, and inventory drops on removal. Mining follows the existing mod's pickaxe and iron-tool tags.

## Transfer and persistence design

- The block extends vanilla `HopperBlock` for its shape and container destruction/comparator behavior. Its menu, ticker, placement, neighbor callbacks, and item collision callback are feature-specific.
- The entity extends `RandomizableContainerBlockEntity` and implements `Hopper`, not either automatic hopper entity. It cannot enter the private combined input/output path in `ModdedHopperBlockEntity` or vanilla `pushItemsTick`/`entityInside`. Only input/insertion/container-lookup helpers are reused.
- Only a neighbor-observed unpowered-to-powered transition attempts output. The power latch and game-time attempt stamp are updated before notifying comparators or attempting insertion, preventing re-entrant/duplicate same-tick attempts. Failed attempts and additional same-tick edges are consumed, never queued. Falling edges and constant-power notifications do not output.
- An attempt removes at most one item, tries destination slots in vanilla sided order, restores the complete original source stack on rejection, and stops at the first success. Current block facing determines the destination and insertion face.
- Ticks and overlapping item collisions collect only while actually unpowered. Successful collection starts an independent eight-game-tick cooldown, including partial item-entity pickup. Output never consults that cooldown or collects an item itself. External insertion/extraction uses ordinary container behavior, including while powered.
- NBT fields: `PulsePowerInitialized` and `PulseLastPowered` store the power latch; `PulseLastGameTime` stores the last attempted pulse tick; `PulseNextInputGameTime` stores the collection deadline. World game time persists over saves/restarts; a deadline can expire while a chunk is unloaded.
- Powered placement explicitly initializes the latch without attempting output. Missing power initialization/state fields mean the first power observation only establishes a baseline. Missing time fields default to `Long.MIN_VALUE` (no previous attempt and collection immediately eligible). Ticking/loading does not synthesize output edges.

## Changed files and integration ownership

Feature-specific files under `common/src/main/`:

- `java/me/Wikos/hoppersadditions/Block/Hoppers/PulseHopperBlock.java`
- `java/me/Wikos/hoppersadditions/BlockEntity/Hoppers/PulseHopperBlockEntity.java`
- `resources/assets/hoppersadditions/blockstates/pulse_hopper.json`
- `resources/assets/hoppersadditions/models/block/pulse_hopper.json`
- `resources/assets/hoppersadditions/models/block/pulse_hopper_side.json`
- `resources/assets/hoppersadditions/models/item/pulse_hopper.json`
- `resources/assets/hoppersadditions/textures/item/pulse_hopper.png`
- `resources/assets/hoppersadditions/textures/block/pulse_hopper_top.png`
- `resources/assets/hoppersadditions/textures/block/pulse_hopper_outside.png`
- `resources/assets/hoppersadditions/textures/block/pulse_hopper_inside.png`
- `resources/data/hoppersadditions/recipe/pulse_hopper.json`
- `resources/data/hoppersadditions/loot_table/blocks/pulse_hopper.json`

The three 16x16 block textures retain the vanilla hopper surface with restrained red circuit accents. The inventory item uses a separate transparent 16x16 sprite with a red pulse marking, matching the mod's flat item icons rather than displaying the 3D block model. The tooltip uses three short lines; detailed timing and persistence semantics remain documented here. This document is also feature-specific.

Shared edits are additive only:

- `Registration.java`: Pulse block, item, entity type, and creative-tab entry.
- `assets/hoppersadditions/lang/en_us.json`: English name, menu title, and three tooltip lines.
- `data/minecraft/tags/block/mineable/pickaxe.json` and `data/minecraft/tags/block/needs_iron_tool.json`: one Pulse ID each.

Splitting/Overflow edits appeared concurrently in this checkout and were left intact. Build results include the checkout state at build time, not an isolated Pulse-only branch. Reconcile shared files serially before integration. No build configuration, dependency, wrapper, or shared transfer-class edits belong to this feature.

## Build validation

Use the JDK and wrapper commands in the root README: Corretto 23.0.2 on this machine, Gradle 8.12.1, Java 21 bytecode target.

- The documented command is `.\gradlew.bat :common:build :fabric:build :neoforge:build --console=plain`. A diagnostic retry added `--stacktrace` after one attempt could not create the Fabric output ZIP; that retry succeeded without any infrastructure edits.
- The final documented-command build after the last Java change succeeded in 11 seconds (31 actionable tasks; 13 executed). Its local, ignored log is `build/pulse-build-final.log`. Both final remapped JARs were rechecked after this build.
- Before the inventory-icon revision, both normal remapped loader JARs were inspected: all eleven original Pulse class/resource entries were present, and both Pulse classes had class major version 65 (Java 21).
- Initial static resource checks passed for all five facings, model/texture links, three block PNGs, the original block-based item model, exact shapeless ingredients/result, block loot, one entry per mining tag, and all five English translation keys. `git diff --check` passed. The inventory-icon revision adds a twelfth Pulse JAR entry and replaces the item-model reference.
- The tooltip/flat-icon revision was rebuilt with the documented command: all three modules passed in 15 seconds (`build/pulse-icon-build.log`). Both remapped loader JARs contain all twelve Pulse entries, the generated-item model, and the transparent 16x16 item sprite. An enlarged sprite preview was visually inspected; in-game rendering remains unverified.
- The project has no automated test sources (`test NO-SOURCE`); compilation, packaging, bytecode inspection, and static checks are not gameplay proof. Gradle deprecation warnings remain.

## Unverified live acceptance checklist

Run every scenario separately on Fabric and NeoForge with the normal remapped JAR. These checks have **not** been performed in a running game:

- [ ] Ten stocked, unobstructed rising edges on separate ticks deliver exactly ten items. Inspect after each edge, not just at the end.
- [ ] Hold power, send duplicate neighbor notifications, wait idle ticks, then remove power: no extra output.
- [ ] Pulse empty, full, missing, and rejecting destinations. Restore stock/capacity while still powered: no retry. A later genuine edge may output.
- [ ] Pull from a chest above and collect dropped items while unpowered. Verify eight-tick spacing, partial pickup cooldown, and zero collection while powered. Collisions must never cause unpulsed output.
- [ ] Immediately pulse after collection while its input cooldown is active. Perform two off/on edges in one game tick; at most one output. A failed first edge must also consume that tick's attempt.
- [ ] Place into constant power, including with preloaded inventory. Save/reload, restart, and unload/reload a powered chunk: no phantom output. Remove the Pulse NBT fields in a disposable world and verify safe first-observation defaults.
- [ ] Test all five facings, a sided furnace (valid and rejected faces/items), mixed component stacks, a non-stackable item, and a full target. Verify complete source/destination counts and source components after rejection.
- [ ] Check ordinary external insertion/extraction while powered; do not count those transfers as the Pulse's own output.
- [ ] Open the five-slot GUI, shift-click both directions, check custom names, comparator strength at empty/partial/full, and comparator updates following collection/output. Exercise a comparator feedback circuit for re-entrancy.
- [ ] Check the creative entry, tooltip, all block orientations, item rendering, shapeless recipe, correct/incorrect tools, block loot, and inventory contents dropped exactly once on break. Power-state changes must not drop inventory.

No client/server gameplay session was started; no merge, push, commit, or issue closure was performed.
