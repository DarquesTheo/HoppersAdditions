package me.Wikos.hoppersadditions.gametest;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import me.Wikos.hoppersadditions.BlockEntity.Hoppers.PickaxeHopperBlockEntity;
import me.Wikos.hoppersadditions.BlockEntity.ModdedHopperBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.Vec3;

public final class HopperGameTests {
    private static final BlockPos HOPPER = new BlockPos(4, 2, 4);
    private static final BlockPos FRONT = HOPPER.east();
    private static final BlockPos DOWN = HOPPER.below();
    private static final List<String> VARIANTS = List.of("reinforced", "pickaxe", "dropping", "magnetic",
            "filter", "monster", "animal", "batch", "small_batch", "splitting", "overflow", "pulse");

    private HopperGameTests() {}

    public static Identifier id(String name) {
        return Identifier.fromNamespaceAndPath("hoppersadditions_gametest", name);
    }

    public static Map<String, Consumer<GameTestHelper>> scenarios() {
        Map<String, Consumer<GameTestHelper>> tests = new LinkedHashMap<>();
        tests.put("reinforced_cooldown", HopperGameTests::reinforced);
        tests.put("pickaxe_mining", HopperGameTests::pickaxe);
        tests.put("pickaxe_fortune", HopperGameTests::fortune);
        tests.put("dropping_components", HopperGameTests::dropping);
        tests.put("magnetic_range", HopperGameTests::magnetic);
        tests.put("filter_reference", HopperGameTests::filter);
        tests.put("filter_blocked", HopperGameTests::filterBlocked);
        tests.put("monster_categories", helper -> mobs(helper, true));
        tests.put("animal_categories", helper -> mobs(helper, false));
        tests.put("batch_threshold", helper -> batch(helper, 64));
        tests.put("small_batch_threshold", helper -> batch(helper, 8));
        tests.put("splitting_alternation", HopperGameTests::splitting);
        tests.put("overflow_priority", HopperGameTests::overflow);
        tests.put("pulse_edges", HopperGameTests::pulseEdges);
        tests.put("pulse_collection", HopperGameTests::pulseCollection);
        tests.put("sided_components", HopperGameTests::sidedComponents);
        tests.put("stack_components", HopperGameTests::stackComponents);
        tests.put("redstone_comparator", HopperGameTests::redstoneComparator);
        tests.put("state_roundtrip", HopperGameTests::stateRoundtrip);
        tests.replaceAll((name, scenario) -> helper -> {
            floor(helper);
            if (Boolean.getBoolean("hoppers.gametest.controlledFailure")) {
                helper.fail("Controlled runner validation: " + name);
                return;
            }
            scenario.accept(helper);
        });
        return tests;
    }

    private static void floor(GameTestHelper helper) {
        for (int column = 0; column < 12; column++) {
            for (int row = 0; row < 12; row++) {
                helper.setBlock(new BlockPos(column, 0, row), Blocks.STONE);
            }
        }
    }

    private static Block block(String variant) {
        return BuiltInRegistries.BLOCK.getValue(Identifier.fromNamespaceAndPath("hoppersadditions", variant + "_hopper"));
    }

    private static Container hopper(GameTestHelper helper, String variant, BlockPos position) {
        helper.setBlock(position, block(variant).defaultBlockState().setValue(HopperBlock.FACING, Direction.EAST));
        return container(helper, position);
    }

    private static Container barrel(GameTestHelper helper, BlockPos position) {
        helper.setBlock(position, Blocks.BARREL);
        return container(helper, position);
    }

    private static Container container(GameTestHelper helper, BlockPos position) {
        BlockEntity entity = helper.getLevel().getBlockEntity(helper.absolutePos(position));
        helper.assertTrue(entity instanceof Container, "Expected inventory at " + position + ", got " + entity);
        return (Container) entity;
    }

    private static int count(Container inventory) {
        int total = 0;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            total += inventory.getItem(slot).getCount();
        }
        return total;
    }

    private static void expect(GameTestHelper helper, int actual, int expected, String label) {
        helper.assertTrue(actual == expected, label + ": expected " + expected + ", actual " + actual + ", tick " + helper.getTick());
    }

    private static void power(GameTestHelper helper, BlockPos position, boolean powered) {
        helper.setBlock(position.west(), powered ? Blocks.REDSTONE_BLOCK : Blocks.AIR);
    }

    private static ItemStack named(int amount, String name) {
        ItemStack stack = new ItemStack(Items.DIAMOND, amount);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(name));
        return stack;
    }

    private static void fill(Container inventory) {
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            inventory.setItem(slot, new ItemStack(Items.COBBLESTONE, 64));
        }
    }

    private static List<ItemEntity> items(GameTestHelper helper) {
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, helper.getBounds(), ItemEntity::isAlive);
    }

    private static ItemEntity item(GameTestHelper helper, ItemStack stack, double horizontal, double height, double depth) {
        Vec3 absolute = helper.absoluteVec(new Vec3(horizontal, height, depth));
        ItemEntity entity = new ItemEntity(helper.getLevel(), absolute.x, absolute.y, absolute.z, stack);
        entity.setNoGravity(true);
        entity.setDeltaMovement(Vec3.ZERO);
        helper.getLevel().addFreshEntity(entity);
        return entity;
    }

    private static void reinforced(GameTestHelper helper) {
        Container fast = hopper(helper, "reinforced", HOPPER);
        Container fastOutput = barrel(helper, FRONT);
        BlockPos vanillaPosition = new BlockPos(4, 2, 8);
        helper.setBlock(vanillaPosition, Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.EAST));
        Container vanilla = container(helper, vanillaPosition);
        Container vanillaOutput = barrel(helper, vanillaPosition.east());
        fast.setItem(0, named(12, "fast"));
        vanilla.setItem(0, named(12, "baseline"));
        helper.runAfterDelay(6, () -> {
            expect(helper, count(fastOutput), 2, "four-tick reinforced output");
            expect(helper, count(vanillaOutput), 1, "eight-tick vanilla output");
            expect(helper, count(fast) + count(fastOutput), 12, "reinforced conservation");
            expect(helper, count(vanilla) + count(vanillaOutput), 12, "vanilla conservation");
            helper.succeed();
        });
    }

    private static void pickaxe(GameTestHelper helper) {
        BlockPos slowPosition = new BlockPos(4, 2, 8);
        Container fast = hopper(helper, "pickaxe", HOPPER);
        Container slow = hopper(helper, "pickaxe", slowPosition);
        PickaxeHopperBlockEntity miner = (PickaxeHopperBlockEntity) fast;
        miner.setEnchantments(3, 2);
        helper.setBlock(HOPPER.above(), Blocks.STONE);
        helper.setBlock(slowPosition.above(), Blocks.STONE);
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(helper.getBlockState(HOPPER.above()).isAir(), "Efficiency III must mine stone by tick 40");
            helper.assertTrue(helper.getBlockState(slowPosition.above()).is(Blocks.STONE), "Unenchanted baseline must still be mining at tick 40");
            CompoundTag saved = miner.saveWithoutMetadata(helper.getLevel().registryAccess());
            expect(helper, saved.getIntOr("EfficiencyLevel", -1), 3, "efficiency state");
            expect(helper, saved.getIntOr("FortuneLevel", -1), 2, "fortune state");
            helper.setBlock(HOPPER.above(), Blocks.BEDROCK);
        });
        helper.runAfterDelay(150, () -> {
            helper.assertTrue(helper.getBlockState(slowPosition.above()).isAir(), "Unenchanted stone must eventually break");
            helper.assertTrue(helper.getBlockState(HOPPER.above()).is(Blocks.BEDROCK), "Bedrock must not break");
            expect(helper, count(fast) + count(slow) + items(helper).stream().mapToInt(entity -> entity.getItem().getCount()).sum(), 2, "two stone blocks yield two cobblestone items");
            helper.succeed();
        });
    }

    private static void fortune(GameTestHelper helper) {
        BlockPos plainPosition = new BlockPos(4, 2, 8);
        Container enchanted = hopper(helper, "pickaxe", HOPPER);
        Container plain = hopper(helper, "pickaxe", plainPosition);
        ((PickaxeHopperBlockEntity) enchanted).setEnchantments(3, 2);
        ((PickaxeHopperBlockEntity) plain).setEnchantments(3, 0);
        helper.setBlock(HOPPER.above(), Blocks.DIAMOND_ORE);
        helper.setBlock(plainPosition.above(), Blocks.DIAMOND_ORE);
        helper.runAfterDelay(120, () -> {
            helper.assertTrue(helper.getBlockState(HOPPER.above()).isAir()
                    && helper.getBlockState(plainPosition.above()).isAir(), "Both ore blocks mined");
            expect(helper, count(enchanted), 2, "deterministic Fortune II test loot");
            expect(helper, count(plain), 1, "deterministic unenchanted test loot");
            expect(helper, items(helper).size(), 0, "all mining output collected");
            helper.assertTrue(enchanted.getItem(0).is(Items.DIAMOND) && plain.getItem(0).is(Items.DIAMOND), "Mining output identity");
            helper.succeed();
        });
    }

    private static void dropping(GameTestHelper helper) {
        Container source = hopper(helper, "dropping", HOPPER);
        Container output = barrel(helper, FRONT);
        source.setItem(0, named(3, "dropped"));
        helper.runAfterDelay(2, () -> {
            power(helper, HOPPER, true);
            expect(helper, count(output), 0, "dropping never inserts into a barrel");
            expect(helper, count(source), 2, "one item emitted");
            List<ItemEntity> drops = items(helper);
            expect(helper, drops.stream().mapToInt(entity -> entity.getItem().getCount()).sum(), 1, "entity quantity");
            helper.assertTrue(drops.stream().allMatch(entity -> ItemStack.isSameItemSameComponents(entity.getItem(), named(1, "dropped"))), "Dropped components must match");
            expect(helper, count(source) + count(output) + drops.stream().mapToInt(entity -> entity.getItem().getCount()).sum(), 3, "dropping conservation");
            helper.succeed();
        });
    }

    private static void magnetic(GameTestHelper helper) {
        Container source = hopper(helper, "magnetic", HOPPER);
        ItemEntity inside = item(helper, named(2, "inside"), 7.5, 3, 4.5);
        ItemEntity outside = item(helper, named(3, "outside"), 10.5, 3, 4.5);
        helper.runAfterDelay(12, () -> {
            expect(helper, count(source), 2, "within collection AABB");
            helper.assertTrue(!inside.isAlive() && outside.isAlive(), "Inside consumed; outside retained");
            expect(helper, outside.getItem().getCount(), 3, "outside quantity unchanged");
            expect(helper, count(source) + items(helper).stream().mapToInt(entity -> entity.getItem().getCount()).sum(), 5, "magnetic conservation");
            helper.succeed();
        });
    }

    private static void filter(GameTestHelper helper) {
        Container filter = hopper(helper, "filter", HOPPER);
        Container input = barrel(helper, HOPPER.above());
        Container output = barrel(helper, FRONT);
        filter.setItem(4, named(1, "match"));
        input.setItem(0, named(3, "match"));
        input.setItem(1, named(4, "different components"));
        input.setItem(2, new ItemStack(Items.DIRT, 5));
        helper.runAfterDelay(45, () -> {
            expect(helper, count(output), 3, "matching items output");
            expect(helper, count(filter), 1, "reference retained");
            expect(helper, input.getItem(1).getCount(), 4, "nonmatching components rejected");
            expect(helper, input.getItem(2).getCount(), 5, "nonmatching item rejected");
            helper.assertTrue(ItemStack.isSameItemSameComponents(filter.getItem(4), named(1, "match")), "Reference identity unchanged");
            expect(helper, count(input) + count(filter) + count(output), 13, "filter conservation including prototype");
            helper.succeed();
        });
    }

    private static void filterBlocked(GameTestHelper helper) {
        Container source = hopper(helper, "filter", HOPPER);
        helper.setBlock(FRONT, Blocks.FURNACE);
        Container output = container(helper, FRONT);
        source.setItem(0, named(3, "not fuel"));
        source.setItem(4, named(1, "not fuel"));
        helper.runAfterDelay(12, () -> {
            expect(helper, count(output), 0, "sided furnace rejects diamonds");
            expect(helper, source.getItem(0).getCount(), 3, "blocked filter output retains complete stack");
            expect(helper, source.getItem(4).getCount(), 1, "blocked filter retains reference");
            helper.assertTrue(ItemStack.isSameItemSameComponents(source.getItem(0), named(1, "not fuel")), "Blocked source components preserved");
            helper.assertTrue(ItemStack.isSameItemSameComponents(source.getItem(4), named(1, "not fuel")), "Blocked reference components preserved");
            expect(helper, count(source) + count(output), 4, "blocked filter conservation including prototype");
            helper.succeed();
        });
    }

    private static void mobs(GameTestHelper helper, boolean monsters) {
        helper.setBlock(HOPPER, block(monsters ? "monster" : "animal").defaultBlockState().setValue(HopperBlock.FACING, Direction.EAST));
        power(helper, HOPPER, true);
        Mob selected = monsters ? helper.spawn(EntityType.HUSK, new Vec3(4.9, 3, 4.9))
                : helper.spawn(EntityType.COW, new Vec3(4.9, 3, 4.9));
        Mob excluded = monsters ? helper.spawn(EntityType.COW, new Vec3(4.1, 3, 4.1))
                : helper.spawn(EntityType.HUSK, new Vec3(4.1, 3, 4.1));
        selected.setNoAi(true);
        excluded.setNoAi(true);
        selected.setNoGravity(true);
        excluded.setNoGravity(true);
        selected.setInvulnerable(true);
        excluded.setInvulnerable(true);
        Vec3 selectedStart = selected.position();
        Vec3 excludedStart = excluded.position();
        helper.runAfterDelay(3, () -> {
            helper.assertTrue(selected.position().distanceTo(selectedStart) < 0.1, "Powered mob hopper must wait");
            power(helper, HOPPER, false);
        });
        helper.runAfterDelay(8, () -> {
            Vec3 target = Vec3.atBottomCenterOf(helper.absolutePos(FRONT));
            helper.assertTrue(selected.position().distanceTo(target) < 0.1, "Selected entity must move to facing output: " + selected.position());
            helper.assertTrue(excluded.position().distanceTo(excludedStart) < 0.1, "Excluded category must not move");
            expect(helper, helper.getLevel().getEntitiesOfClass(Mob.class, helper.getBounds()).size(), 2, "no entity duplication/loss");
            helper.assertTrue(helper.getLevel().getEntity(selected.getUUID()) == selected && helper.getLevel().getEntity(excluded.getUUID()) == excluded, "Original UUIDs preserved");
            helper.succeed();
        });
    }

    private static void batch(GameTestHelper helper, int threshold) {
        Container source = hopper(helper, threshold == 8 ? "small_batch" : "batch", HOPPER);
        Container output = barrel(helper, FRONT);
        source.setItem(0, new ItemStack(Items.DIAMOND, threshold - 1));
        helper.runAfterDelay(12, () -> {
            expect(helper, count(source), threshold - 1, "below-threshold waiting");
            expect(helper, count(output), 0, "below-threshold destination");
            source.setItem(0, new ItemStack(Items.DIAMOND, threshold));
        });
        helper.runAfterDelay(24, () -> {
            expect(helper, count(output), threshold, "exact batch transfer");
            expect(helper, count(source), 0, "exact batch source emptied");
            fill(output);
            output.setItem(0, new ItemStack(Items.DIAMOND, 62));
            source.setItem(0, new ItemStack(Items.DIAMOND, threshold));
        });
        helper.runAfterDelay(36, () -> {
            expect(helper, output.getItem(0).getCount(), 64, "only available capacity filled");
            expect(helper, count(source), threshold - 2, "partial batch remainder restored");
            expect(helper, count(output) + count(source), 26 * 64 + 62 + threshold, "capacity-limited conservation");
            helper.succeed();
        });
    }

    private static void splitting(GameTestHelper helper) {
        Container source = hopper(helper, "splitting", HOPPER);
        Container front = barrel(helper, FRONT);
        Container down = barrel(helper, DOWN);
        fill(front);
        source.setItem(0, named(3, "split"));
        helper.runAfterDelay(12, () -> {
            expect(helper, count(source), 3, "blocked front retains source");
            expect(helper, count(down), 0, "blocked front must not fall back");
            front.clearContent();
        });
        helper.runAfterDelay(14, () -> {
            expect(helper, count(front), 1, "first successful turn front");
            fill(down);
        });
        helper.runAfterDelay(28, () -> {
            expect(helper, count(front), 1, "blocked down must not fall back");
            expect(helper, count(source), 2, "blocked down retains source");
            down.clearContent();
        });
        helper.runAfterDelay(30, () -> {
            power(helper, HOPPER, true);
            expect(helper, count(down), 1, "second successful turn down");
            expect(helper, count(source) + count(front) + count(down), 3, "split conservation");
            helper.succeed();
        });
    }

    private static void overflow(GameTestHelper helper) {
        Container source = hopper(helper, "overflow", HOPPER);
        Container front = barrel(helper, FRONT);
        Container down = barrel(helper, DOWN);
        source.setItem(0, named(3, "overflow"));
        helper.runAfterDelay(2, () -> {
            expect(helper, count(front), 1, "front priority");
            expect(helper, count(down), 0, "down waits while front accepts");
            fill(front);
        });
        helper.runAfterDelay(10, () -> {
            expect(helper, count(down), 1, "same candidate falls back down");
            helper.assertTrue(ItemStack.isSameItemSameComponents(down.getItem(0), named(1, "overflow")), "Fallback components preserved");
            fill(down);
        });
        helper.runAfterDelay(22, () -> {
            expect(helper, count(source), 1, "both rejecting retains item");
            expect(helper, count(front) + count(down) + count(source), 54 * 64 + 1, "both-full conservation after explicit fixture replacement");
            helper.succeed();
        });
    }

    private static void pulseEdges(GameTestHelper helper) {
        Container source = hopper(helper, "pulse", HOPPER);
        Container output = barrel(helper, FRONT);
        source.setItem(0, named(3, "pulse"));
        helper.runAfterDelay(2, () -> power(helper, HOPPER, true));
        helper.runAfterDelay(14, () -> {
            expect(helper, count(output), 1, "held power fires only once");
            expect(helper, count(source), 2, "held-power source");
            power(helper, HOPPER, false);
            fill(output);
        });
        helper.runAfterDelay(16, () -> power(helper, HOPPER, true));
        helper.runAfterDelay(18, () -> {
            expect(helper, count(source), 2, "failed pulse retains source");
            output.clearContent();
        });
        helper.runAfterDelay(28, () -> {
            expect(helper, count(output), 0, "failed pulse must not replay");
            power(helper, HOPPER, false);
        });
        helper.runAfterDelay(30, () -> power(helper, HOPPER, true));
        helper.runAfterDelay(32, () -> {
            expect(helper, count(output), 1, "fresh edge transfers one");
            expect(helper, count(source) + count(output), 2, "pulse conservation after deliberate fixture removal");
            source.clearContent();
            power(helper, HOPPER, false);
        });
        helper.runAfterDelay(34, () -> power(helper, HOPPER, true));
        helper.runAfterDelay(36, () -> source.setItem(0, named(1, "late")));
        helper.runAfterDelay(46, () -> {
            expect(helper, count(source), 1, "empty pulse consumed, late insertion retained");
            expect(helper, count(output), 1, "empty pulse not replayed");
            helper.succeed();
        });
    }

    private static void pulseCollection(GameTestHelper helper) {
        Container source = hopper(helper, "pulse", HOPPER);
        Container input = barrel(helper, HOPPER.above());
        Container output = barrel(helper, FRONT);
        power(helper, HOPPER, true);
        input.setItem(0, named(3, "input"));
        helper.runAfterDelay(12, () -> {
            expect(helper, count(source), 0, "powered collection prohibited");
            expect(helper, count(input), 3, "powered input unchanged");
            power(helper, HOPPER, false);
        });
        helper.runAfterDelay(14, () -> {
            expect(helper, count(source), 1, "unpowered collects one");
            restore(helper, HOPPER);
        });
        helper.runAfterDelay(16, () -> {
            expect(helper, count(container(helper, HOPPER)), 1, "persisted input cooldown prevents early collection");
            power(helper, HOPPER, true);
            restore(helper, HOPPER);
        });
        helper.runAfterDelay(28, () -> {
            expect(helper, count(output), 1, "powered restoration must not synthesize another edge");
            expect(helper, count(input) + count(container(helper, HOPPER)) + count(output), 3, "collection/restore conservation");
            helper.succeed();
        });
    }

    private static void sidedComponents(GameTestHelper helper) {
        Container source = hopper(helper, "splitting", HOPPER);
        helper.setBlock(FRONT, Blocks.FURNACE);
        Container furnace = container(helper, FRONT);
        source.setItem(0, named(2, "not fuel"));
        helper.runAfterDelay(12, () -> {
            expect(helper, count(furnace), 0, "furnace side rejects nonfuel");
            expect(helper, count(source), 2, "sided rejection conserves source");
            source.setItem(1, new ItemStack(Items.COAL, 2));
        });
        helper.runAfterDelay(14, () -> {
            power(helper, HOPPER, true);
            expect(helper, furnace.getItem(1).getCount(), 1, "furnace fuel slot accepts coal from side");
            expect(helper, source.getItem(0).getCount(), 2, "earlier rejected component stack intact");
            expect(helper, count(source) + count(furnace), 4, "sided conservation");
            helper.succeed();
        });
    }

    private static void stackComponents(GameTestHelper helper) {
        Container source = hopper(helper, "splitting", HOPPER);
        Container output = barrel(helper, FRONT);
        fill(output);
        output.setItem(0, named(63, "existing"));
        source.setItem(0, named(2, "different"));
        BlockPos batchPosition = new BlockPos(4, 2, 8);
        Container batch = hopper(helper, "batch", batchPosition);
        Container batchOutput = barrel(helper, batchPosition.east());
        batch.setItem(0, new ItemStack(Items.ENDER_PEARL, 15));
        helper.runAfterDelay(12, () -> {
            expect(helper, count(source), 2, "incompatible components reject transfer");
            expect(helper, output.getItem(0).getCount(), 63, "incompatible components do not merge");
            expect(helper, count(batchOutput), 0, "sixteen-stack batch waits below item maximum");
            batch.setItem(0, new ItemStack(Items.ENDER_PEARL, 16));
            output.setItem(1, named(63, "different"));
        });
        helper.runAfterDelay(14, () -> {
            power(helper, HOPPER, true);
            expect(helper, output.getItem(1).getCount(), 64, "matching component stack fills only to limit");
            expect(helper, output.getItem(0).getCount(), 63, "different component stack unchanged");
            expect(helper, count(source), 1, "source remainder retained");
            helper.assertTrue(ItemStack.isSameItemSameComponents(source.getItem(0), named(1, "different")), "Source components retained");
        });
        helper.runAfterDelay(26, () -> {
            expect(helper, count(batchOutput), 16, "batch threshold capped by item stack maximum");
            expect(helper, count(batch), 0, "sixteen-stack batch source emptied");
            expect(helper, count(source) + count(output), 25 * 64 + 128, "component/stack-limit conservation");
            helper.succeed();
        });
    }

    private static void redstoneComparator(GameTestHelper helper) {
        Map<String, BlockPos> positions = new LinkedHashMap<>();
        int index = 0;
        for (String variant : VARIANTS) {
            if (variant.equals("monster") || variant.equals("animal")) { continue; }
            BlockPos position = new BlockPos(2 + index % 3 * 3, 2, 1 + index / 3 * 3);
            Container source = hopper(helper, variant, position);
            barrel(helper, position.east());
            power(helper, position, true);
            source.setItem(0, named(64, variant));
            positions.put(variant, position);
            index++;
        }
        helper.runAfterDelay(12, () -> {
            positions.forEach((variant, position) -> {
                Container source = container(helper, position);
                expect(helper, count(source), 64, variant + " powered hopper waits");
                expect(helper, count(container(helper, position.east())), 0, variant + " powered output empty");
                BlockState state = helper.getBlockState(position);
                int expected = AbstractContainerMenu.getRedstoneSignalFromContainer(source);
                expect(helper, state.getAnalogOutputSignal(helper.getLevel(), helper.absolutePos(position), Direction.EAST), expected, variant + " comparator signal");
                helper.assertTrue(expected > 0, variant + " positive comparator signal");
            });
            power(helper, positions.get("reinforced"), false);
        });
        helper.runAfterDelay(18, () -> {
            BlockPos position = positions.get("reinforced");
            Container source = container(helper, position);
            Container output = container(helper, position.east());
            helper.assertTrue(count(output) > 0, "unpowered reinforced hopper resumes");
            expect(helper, count(source) + count(output), 64, "redstone conservation");
            helper.succeed();
        });
    }

    private static void restore(GameTestHelper helper, BlockPos position) {
        BlockEntity entity = helper.getLevel().getBlockEntity(helper.absolutePos(position));
        CompoundTag saved = entity.saveWithoutMetadata(helper.getLevel().registryAccess());
        BlockState state = helper.getBlockState(position);
        if (entity instanceof Container inventory) { inventory.clearContent(); }
        helper.setBlock(position, Blocks.AIR);
        helper.setBlock(position, state);
        helper.getLevel().getBlockEntity(helper.absolutePos(position)).loadWithComponents(
                TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), saved));
    }

    private static void stateRoundtrip(GameTestHelper helper) {
        int index = 0;
        for (String variant : VARIANTS) {
            BlockPos position = new BlockPos(2 + index % 4 * 2, 2, 2 + index / 4 * 3);
            helper.setBlock(position, block(variant).defaultBlockState().setValue(HopperBlock.FACING, Direction.EAST));
            power(helper, position, true);
            BlockEntity original = helper.getLevel().getBlockEntity(helper.absolutePos(position));
            if (original instanceof Container inventory) {
                inventory.setItem(0, named(7, variant));
                if (variant.equals("filter")) { inventory.setItem(4, named(1, "prototype")); }
            }
            if (original instanceof PickaxeHopperBlockEntity miner) { miner.setEnchantments(3, 2); }
            CompoundTag seeded = original.saveWithoutMetadata(helper.getLevel().registryAccess());
            if (original instanceof ModdedHopperBlockEntity) { seeded.putInt("TransferCooldown", 6); }
            if (variant.equals("splitting")) { seeded.putBoolean("NextDown", true); }
            if (variant.equals("pickaxe")) { seeded.putFloat("BreakProgress", 0.375F); }
            if (variant.equals("pulse")) {
                seeded.putBoolean("PulseLastPowered", true);
                seeded.putLong("PulseLastGameTime", helper.getLevel().getGameTime());
                seeded.putLong("PulseNextInputGameTime", helper.getLevel().getGameTime() + 8);
            }
            original.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, helper.getLevel().registryAccess(), seeded));
            CompoundTag before = original.saveWithoutMetadata(helper.getLevel().registryAccess());
            restore(helper, position);
            BlockEntity restored = helper.getLevel().getBlockEntity(helper.absolutePos(position));
            CompoundTag after = restored.saveWithoutMetadata(helper.getLevel().registryAccess());
            before.remove("PulsePowerInitialized");
            after.remove("PulsePowerInitialized");
            helper.assertTrue(before.equals(after), variant + " serialization round trip: expected " + before + ", actual " + after);
            if (restored instanceof Container inventory) { expect(helper, count(inventory), variant.equals("filter") ? 8 : 7, variant + " restored quantity"); }
            index++;
        }
        helper.runAfterDelay(3, () -> {
            expect(helper, items(helper).size(), 0, "restoration fixture must not duplicate contents as dropped entities");
            helper.succeed();
        });
    }
}
