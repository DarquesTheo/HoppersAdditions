# Add Overflow Hopper with front-priority routing

## Goal and behavior
- Vanilla-style priority junction for storage before composting, fuel reserves, and excess disposal.
- ID: `hoppersadditions:overflow_hopper`; five slots, normal collection area, one-item output, 8-game-tick cooldown.
- Horizontal front is primary; bottom is overflow. Against vertical faces, face toward the clicked block; on top/bottom placement, face away from the player. Models/rotation/mirroring agree.
- Scan nonempty slots in normal order. Try one candidate forward; only if that exact item cannot insert, try down. If both reject, restore exactly and continue scanning. Stop after one successful output.
- Primary availability is item-specific, respecting components, stack limits, and sided restrictions, not just empty slots. Missing primary permits overflow.
- No loose-item output, double delivery, thresholds, filtering GUI, extra range/speed, or network inspection.
- Power locks automatic input/output, including powered placement. External insertion/extraction remains ordinary; priority governs only own output.
- Persist inventory/cooldown through saves/chunk unload. Breaking drops contents once; no inventory-carrying item.
- Recipe: shapeless vanilla hopper + comparator -> one Overflow Hopper. Restrained vanilla visuals.

## Deliverables and ownership
- Add `OverflowHopperBlock` under `common/src/main/java/me/Wikos/hoppersadditions/Block/Hoppers/` and `OverflowHopperBlockEntity` under the corresponding `BlockEntity/Hoppers/` directory.
- Include registration, creative entry, English name/tooltip/container text, blockstate, models, textures, recipe, loot, and mining/tool tags.
- Minecraft 1.21.1 / Java 21 / existing common, Fabric, NeoForge modules; no dependency upgrades or unrelated fixes.
- Own feature-specific files. Only narrowly additive edits to shared `Registration.java`, `assets/hoppersadditions/lang/en_us.json`, and mining tags; coordinator reconciles serially. No shared hopper refactor; agree hooks first. Verify actual custom-entity GUI/comparator/removal behavior.

## Acceptance
- [ ] Accepting primary gets the candidate; bottom gets nothing that attempt.
- [ ] Missing/full/rejecting primary routes that item down if accepted.
- [ ] Final primary space fills before later overflow; primary accepting A but rejecting B routes both correctly in slot order.
- [ ] Blocked/sided inventories, component-sensitive stacks, non-stackables, and failures preserve exact counts.
- [ ] Models/orientation, power, persistence, break drops, GUI/comparator, recipe, creative entry, mining, and loot work.
- [ ] Common/Fabric/NeoForge builds and live checks on both loaders are reported separately; unverified coverage is explicit.

## Handoff
One T3 worktree implementation chat; read local instructions, do not delegate again. No merge/push without user authorization. Report files, commands/results, shared edits, and gaps. Close only after integration and acceptance review.

Baseline: `efa29afa3a2b2ac91ce7bed7c879db25fe21752c`. Gradle 8.12.1 is referenced but wrapper launchers/JAR are absent; coordinate tooling resolution, not independent infrastructure edits per feature. Compilation is not runtime proof.
