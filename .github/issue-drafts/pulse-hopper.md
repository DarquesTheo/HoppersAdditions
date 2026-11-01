# Add Pulse Hopper with one-item rising-edge output

## Goal and behavior
- Vanilla-style metering for timed ingredients, counting, furnace loading, and controlled supplies.
- ID: `hoppersadditions:pulse_hopper`; five slots, normal collection area and down/horizontal placement, restrained redstone accents.
- Separate input/output. Collect while unpowered on an 8-game-tick input cooldown; automatic collection stops powered. External insertion/extraction stays ordinary; pulse guarantees cover only own output.
- Genuine unpowered-to-powered transition immediately attempts one already-stored item into the facing container, honoring insertion sides/slot order. Input cooldown does not suppress edge output.
- Constant power, falling edges, and duplicate notifications do not output. Maximum one own output per game tick; additional same-tick edges are consumed, not queued.
- Empty/missing/full/rejected pulses are consumed: no credits or delayed retry when stock/capacity returns under constant power.
- Powered placement initializes without output. Save/reload, restart, and chunk unload/reload under constant power must not invent edges. Persist necessary power/edge state and define defaults for missing fields.
- No collecting and spending a new item inside one edge callback. Tickers, collisions, neighbor callbacks, and inherited paths cannot bypass edge-only output. Power-lock logic must not suppress the rising-edge output itself.
- Restore failed insertions exactly; breaking drops inventory once.
- Recipe: shapeless vanilla hopper + repeater -> one Pulse Hopper. No clock, delay GUI, wireless behavior, or crafting system.

## Deliverables and ownership
- Add `PulseHopperBlock` under `common/src/main/java/me/Wikos/hoppersadditions/Block/Hoppers/` and `PulseHopperBlockEntity` under the corresponding `BlockEntity/Hoppers/` directory.
- Include registration, creative entry, English name/tooltip/container text, blockstate, models, textures, recipe, loot, and mining/tool tags. Tooltip explains pulse semantics.
- Minecraft 1.21.1 / Java 21 / existing common, Fabric, NeoForge modules; no dependency upgrades.
- Own feature-specific files. Only narrowly additive edits to shared `Registration.java`, `assets/hoppersadditions/lang/en_us.json`, and mining tags; coordinator reconciles serially. No shared refactor; agree hooks first.
- Inspect private combined transfer logic and `entityInside` in `ModdedHopperBlockEntity`; avoid unrestricted automatic output. Verify actual GUI/comparator/removal behavior.

## Acceptance
- [ ] Ten stocked, unblocked rising edges on separate ticks deliver ten items.
- [ ] Constant power/falling edges/duplicate notifications/idle ticks deliver nothing extra.
- [ ] Failed pulses are consumed; restoring stock/capacity while powered does not deliver.
- [ ] Input works unpowered, stops powered; collisions never trigger unpulsed output.
- [ ] Input cooldown does not swallow edges; same-tick events obey output limit.
- [ ] Powered placement/save/reload/chunk unload never cause phantom output.
- [ ] Sided inventories, component stacks, non-stackables, and failures preserve counts.
- [ ] Models, recipe, mining, loot, creative entry, GUI/comparator, break drops work.
- [ ] Common/Fabric/NeoForge builds and live redstone checks on both loaders are reported separately; unverified coverage is explicit.

## Handoff
One T3 worktree implementation chat; read local instructions, do not delegate again. No merge/push without user authorization. Report files, commands/results, power/persistence design, shared edits, and gaps. Close only after integration and acceptance review.

Baseline: `efa29afa3a2b2ac91ce7bed7c879db25fe21752c`. Gradle 8.12.1 is referenced but wrapper launchers/JAR are absent; coordinate tooling resolution, not independent infrastructure changes per feature. Compilation is not live gameplay proof.
