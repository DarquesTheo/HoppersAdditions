# Overflow Hopper (issue #2)

## Implemented contract

- Five slots, vanilla collection volume and one successful output at most per transfer cycle, followed by an eight-game-tick cooldown.
- Scan slots in order. Remove one candidate, attempt the horizontal front first, then the bottom with the same candidate. Restore the original stack, including components, before trying a later slot if neither accepts.
- Insertion checks the destination's exposed slots, placement predicate, sided face restrictions, component equality, and container/item stack limit. Missing primary inventory permits bottom output. No item entities are emitted.
- Side-face placement points into the clicked block. Top/bottom placement points away from the player. Inherited rotation/mirroring follows the front; the model and collision shape include both outlets.
- Neighbor power, including power already present on placement, disables automatic output and collection. Ordinary external container access is unchanged.
- Inventory, components, custom names, and transfer cooldown use the existing container persistence. The existing five-slot menu is reused. Vanilla hopper comparator and removal methods operate on the custom Container; no duplicate removal/drop override is introduced.
- Shapeless vanilla hopper plus comparator produces one hopper. The loot table drops only the block item, without inventory components; the inherited removal hook drops the contents separately.

## Changed files owned by this feature

- common/src/main/java/me/Wikos/hoppersadditions/Block/Hoppers/OverflowHopperBlock.java
- common/src/main/java/me/Wikos/hoppersadditions/BlockEntity/Hoppers/OverflowHopperBlockEntity.java
- common/src/main/resources/assets/hoppersadditions/blockstates/overflow_hopper.json
- common/src/main/resources/assets/hoppersadditions/models/block/overflow_hopper_side.json
- common/src/main/resources/assets/hoppersadditions/models/item/overflow_hopper.json
- common/src/main/resources/assets/hoppersadditions/textures/block/overflow_hopper_outside.png
- common/src/main/resources/assets/hoppersadditions/textures/item/overflow_hopper.png
- common/src/main/resources/data/hoppersadditions/recipe/overflow_hopper.json
- common/src/main/resources/data/hoppersadditions/loot_table/blocks/overflow_hopper.json
- docs/overflow-hopper.md

Shared, additive integration edits:

- common/src/main/java/me/Wikos/hoppersadditions/Registration.java: block, item, block entity, creative entry.
- common/src/main/resources/assets/hoppersadditions/lang/en_us.json: name, tooltip, container title.
- common/src/main/resources/data/minecraft/tags/block/mineable/pickaxe.json: overflow entry.
- common/src/main/resources/data/minecraft/tags/block/needs_iron_tool.json: overflow entry.

No shared hopper refactor, dependency upgrade, wrapper, or build-configuration change. Concurrent Splitting and Pulse changes appeared in this same checkout during implementation; they were not authored, reverted, or claimed by the Overflow work. Build results describe the checkout at invocation, not isolated-feature or final integration proof.

## Validation on November 1, 2026

Build JVM: Amazon Corretto JDK 23.0.2, with Java 21 compilation target. Wrapper: Gradle 8.12.1.

Commands (PowerShell, after the README's JAVA_HOME/PATH selection):

~~~powershell
.\gradlew.bat --version
.\gradlew.bat :common:build :fabric:build :neoforge:build --console=plain
~~~

The initial three-module build succeeded. An intermediate incremental compilation could not resolve shared base classes; a subsequent forced three-module rebuild with --rerun-tasks succeeded (32 tasks, including an ignored temporary classpath-export task). No tracked build configuration was altered to recover. Gradle reports existing deprecation/beta tooling warnings; there are no automated test sources in the project.

The final exact README build command also succeeded (31 tasks up to date, two seconds). Both normal remapped loader JARs contain all eight expected Overflow classes/resources; the entity class has bytecode major version 65 (Java 21). JSON parsing, texture dimensions, mining-tag membership, and git diff --check passed. Build logs are retained locally in build/overflow-build.log and build/overflow-final-build.log.

An ignored temporary harness in build/overflow-validation/OverflowRoutingCheck.java executed the actual private routing method against Minecraft containers: **150 assertions passed**. Coverage includes front priority, last primary space, full/missing/rejecting primary, continuing after a rejected slot with exact restoration, item-specific predicates, component-sensitive stacks, non-stackables, sided access, destination stack caps, zero capacity, bottom UP face, and 128-item conservation. It also checks the inherited five-slot menu factory, comparator fullness, and inventory/components/cooldown NBT round trip. The harness bypasses block-entity construction with Unsafe and does not start a level, loader, or gameplay session. Its warnings do not affect production source.

Feature JSON parses; the custom texture is 16x16; the model has both outlet elements; mining-tag membership is present. Static inspection confirms vanilla HopperBlock's comparator uses AbstractContainerMenu.getRedstoneSignalFromBlockEntity and removal uses Containers.dropContentsOnDestroy, which accepts any Container. This is not live GUI or break-drop proof.

## Remaining live acceptance checks: run separately on Fabric and NeoForge

Item-art follow-up: the inventory item uses the vanilla transparent 16x16 hopper sprite as its base, with only a small horizontal outlet added using its original gray palette, via minecraft:item/generated. The vanilla bowl, shading, and bottom outlet are preserved; the earlier custom silhouette and brass accent were removed. This adds a ninth feature class/resource entry to the earlier eight-entry package audit. In-game rendering remains unverified. Local build log: build/overflow-item-build.log.

- [ ] Install the normal matching remapped loader JAR; confirm registration and creative entry, translated name and tooltip, five-slot GUI and title, recipe, pickaxe/tool requirement, and loot.
- [ ] Chest in front and below: accepting front receives the candidate; bottom stays unchanged. Fill the final front space before overflow starts.
- [ ] Test no front inventory, full front, blocked/double chests, furnace-sided inventories, component-sensitive items, non-stackables, and item-specific A/B acceptance. Count every item before and after repeated failures and successes.
- [ ] Verify side placement and top/bottom placement in all directions, structure rotation/mirroring, both visible outlets, model lighting, hit shape, and no missing textures.
- [ ] Verify eight-game-tick throughput and ordinary collection volume; items colliding with the hopper must not bypass priority or power lock.
- [ ] Power before placement and toggle power during operation: no automatic input/output while locked. Confirm external insertion/extraction remains ordinary.
- [ ] Save/reload and unload/reload a chunk mid-cooldown with component-bearing inventory; verify no resets, losses, or duplication.
- [ ] Verify actual comparator output and updates; break a filled hopper and confirm exactly one set of inventory drops and one appropriate block drop, with no inventory-carrying item. Confirm same-block power/rotation updates do not drop contents.
- [ ] Reconcile all shared-file edits serially and rerun combined builds and cross-feature gameplay checks.

No live gameplay, client rendering, or loader-world acceptance has been claimed. No commit, merge, push, or issue closure is authorized or performed.
