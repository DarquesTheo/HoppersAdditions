# Add Splitting Hopper with strict alternating outputs

## Goal and behavior
- Vanilla-style routing for crop allocation, twin furnace lines, and ingredient distribution.
- ID: `hoppersadditions:splitting_hopper`; five slots, normal collection area, one-item output, 8-game-tick cooldown.
- Two outputs: horizontal front and bottom. Against vertical faces, face toward the clicked block; on top/bottom placement, face away from the player. Rotation/mirroring and visible outlets agree.
- First success goes forward, then down. Advance only on success. Missing/full/rejecting next branch pauses output without fallback. Scan slots in normal order for an acceptable item.
- Honor sided insertion, components, and stack limits. Restore failed transfers exactly. No loose-item output, loss, or duplication.
- Power locks automatic input/output, including powered placement. External insertion/extraction stays ordinary; alternation governs only own output.
- Persist inventory, cooldown, and next branch across saves/chunk unload. Breaking drops contents once; replacing resets branch, with no inventory-carrying item.
- Recipe: shapeless vanilla hopper + iron ingot + quartz -> one Splitting Hopper.
- Restrained vanilla geometry/colors; no configuration GUI, extra range/speed, energy, or networks.

## Deliverables and ownership
- Add `SplittingHopperBlock` under `common/src/main/java/me/Wikos/hoppersadditions/Block/Hoppers/` and `SplittingHopperBlockEntity` under the corresponding `BlockEntity/Hoppers/` directory.
- Include registration, creative entry, English name/tooltip/container text, blockstate, models, textures, recipe, loot, and mining/tool tags.
- Target existing Minecraft 1.21.1 / Java 21 / Architectury common, Fabric, and NeoForge. No dependency upgrades or unrelated fixes.
- Own feature-specific files. Only narrowly additive edits to shared `Registration.java`, `assets/hoppersadditions/lang/en_us.json`, and mining tags; coordinator reconciles them serially. No shared hopper refactor; agree necessary hooks first. Verify actual custom-entity GUI/comparator/removal behavior.

## Acceptance
- [ ] Twenty successful outputs deliver ten per branch, starting forward.
- [ ] Blocked/missing next branch waits and resumes correctly.
- [ ] Sided inventories, mixed/component-sensitive stacks, non-stackables, partial capacity, and failed transfers preserve counts.
- [ ] Orientation/models, power locking, save/reload/chunk unload, break drops, GUI/comparator, recipe, creative entry, mining, and loot work.
- [ ] Common/Fabric/NeoForge builds and gameplay checks on both loaders are reported separately; unverified coverage is explicit.

## Handoff
One T3 worktree implementation chat; read local instructions and do not delegate again. No merge/push without user authorization. Report files, commands/results, shared edits, and gaps. Close only after coordinator integration and acceptance review.

Baseline: `efa29afa3a2b2ac91ce7bed7c879db25fe21752c`. Gradle 8.12.1 wrapper properties exist but launcher scripts/JAR are absent; coordinate tooling resolution rather than independently changing infrastructure per feature. Build success is not runtime proof.
