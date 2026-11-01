# Splitting Hopper - issue #1 handoff

## Implemented behavior

- ID: `hoppersadditions:splitting_hopper`; five ordinary hopper slots and the vanilla collection area.
- One item per successful output, with an eight-game-tick transfer cooldown.
- The first successful output goes to the horizontal front, then down. Only successful own outputs toggle the saved `NextDown` flag. A missing, full, or rejecting next destination never falls back to the other branch.
- Source slots are scanned in order. Transfers respect sided slots, insertion/extraction rules, item components, item stack limits, and destination container limits. A rejected one-item transfer restores a copy of the original source stack.
- Automatic input and output honor the inherited redstone enabled state; placement sets that state from existing power. External inventory access remains ordinary.
- The front points toward a clicked vertical face; top/bottom placement points in the player's horizontal look direction (away from the player). Inherited rotation/mirroring follow the front. The model and collision shape include a bottom outlet as well as the front outlet.
- Inventory and cooldown use the existing container serialization; `NextDown` is saved separately. Ordinary block loot does not copy inventory or branch state into an item.
- Shapeless recipe: vanilla hopper + iron ingot + quartz -> one Splitting Hopper.

## File ownership

New feature-specific files:

- `common/src/main/java/me/Wikos/hoppersadditions/Block/Hoppers/SplittingHopperBlock.java`
- `common/src/main/java/me/Wikos/hoppersadditions/BlockEntity/Hoppers/SplittingHopperBlockEntity.java`
- `common/src/main/resources/assets/hoppersadditions/blockstates/splitting_hopper.json`
- `common/src/main/resources/assets/hoppersadditions/models/block/splitting_hopper.json`
- `common/src/main/resources/assets/hoppersadditions/models/item/splitting_hopper.json`
- `common/src/main/resources/assets/hoppersadditions/textures/block/splitting_hopper_outside.png`
- `common/src/main/resources/assets/hoppersadditions/textures/item/splitting_hopper.png` (flat, transparent 16x16 inventory icon; preserves the vanilla hopper pixels and adds a small second outlet using its existing palette).
- `common/src/main/resources/data/hoppersadditions/recipe/splitting_hopper.json`
- `common/src/main/resources/data/hoppersadditions/loot_table/blocks/splitting_hopper.json`
- This handoff document.

Narrowly additive shared edits requiring coordinator reconciliation:

- `Registration.java`: block, block item, block entity type, and creative-tab entry.
- `assets/hoppersadditions/lang/en_us.json`: block name, container title, tooltip.
- `data/minecraft/tags/block/mineable/pickaxe.json` and `needs_iron_tool.json`: one ID each.

No shared hopper implementation, dependency, wrapper, or build configuration was changed. Overflow and Pulse edits appeared concurrently in this working directory; they were not authored or removed by this implementation. The successful build therefore describes the working directory at build time, not an isolated Splitting-only checkout.

## Validation performed (November 1, 2026)

Selected the README's installed Corretto JDK 23.0.2 and included Gradle 8.12.1 wrapper:

```powershell
$env:JAVA_HOME = "$env:USERPROFILE\.jdks\corretto-23.0.2"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
java -version
.\gradlew.bat --version
.\gradlew.bat clean :common:build :fabric:build :neoforge:build --console=plain
```

- Clean common, Fabric, and NeoForge builds: PASS (42 seconds). An earlier feature compile error in the codec return type was fixed. A subsequent attempt encountered incomplete concurrent Pulse registration; no Pulse code was changed, and the final clean build passed after that registration appeared.
- Compiled Splitting entity bytecode: major version 65 (Java 21).
- Both remapped loader JARs contain both feature classes and all seven feature assets/data files, including the 16x16 block texture and flat item icon. Five feature JSON files parse successfully.
- Item-icon follow-up: switched the item model to `minecraft:item/generated` with a transparent 16x16 pixel-art sprite showing both outlets. Common, Fabric, and NeoForge builds passed again (11 seconds); in-game appearance remains unverified.
- `git diff --check`: PASS (Git prints line-ending conversion notices).
- A temporary, non-repository Java harness invoked the actual compiled private transfer methods against Minecraft 1.21.1 `SimpleContainer` and a one-slot `WorldlyContainer`: 12 checks PASS. Coverage: inaccessible side, sided rejection, exact count/component rollback, one-item success, container limit, full target, mismatching components, partial compatible capacity, non-stackable item, failed final-item rollback, and partial loose-stack insertion count conservation. This harness bootstraps Minecraft registries but does not start a world or either loader. It is not a persistent automated test suite.
- Static inspection confirms the custom block opens its own inherited five-slot HopperMenu. Vanilla HopperBlock comparator calculation accepts a generic container block entity; its removal path uses generic container destruction/drop handling, not a vanilla HopperBlockEntity-only check. Actual GUI, comparator, and removal behavior remain unverified in a running game.

Build output still reports existing deprecated Gradle features and a deprecated Minecraft API use. No unrelated tooling changes were made.

## Required live acceptance on BOTH loaders

None of the following world/client checks has been performed. Build and isolated method checks are not live gameplay proof.

1. Place two non-extracting destination chests at the front and below. Supply 20 items and verify ten in each, with the first arriving forward and at least eight game ticks between successful own outputs.
2. Remove/fill/reject the currently selected destination. Verify no fallback and unchanged source counts; restore it and verify that exact branch resumes. Try a rejected early source slot followed by an acceptable later slot.
3. Exercise furnaces/sided inventories, mixed component-sensitive stacks, single items, non-stackables, partial capacity, and full destinations; audit total item counts before/after. Test dropped-item collection with only partial hopper capacity and verify the remainder stays in the world.
4. Check every horizontal placement, top/bottom placement, rotation/mirroring, both visible outlets, and collision/selection shapes.
5. Place already powered, then toggle power while items are inside/above. Verify automatic collection and output lock, while other inventories/players may still insert/extract without advancing the branch.
6. Save/reload and unload/reload the chunk after an odd output and during cooldown. Verify inventory, cooldown, and next branch persist.
7. Open and use the five-slot GUI; verify title and comparator strength at empty/partial/full inventory. Break the block and audit contents drop exactly once. Replace it and confirm the next success goes forward again.
8. Verify creative entry, tooltip, shapeless recipe, iron-tool mining behavior, and ordinary block loot in survival. Check client/server logs for feature errors and missing models/textures.

No commit, merge, push, issue closure, or acceptance approval was performed.
