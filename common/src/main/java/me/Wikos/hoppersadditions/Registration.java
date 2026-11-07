package me.Wikos.hoppersadditions;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import me.Wikos.hoppersadditions.Block.Hoppers.*;
import me.Wikos.hoppersadditions.BlockEntity.Hoppers.*;
import me.Wikos.hoppersadditions.Item.PickaxeHopperItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import me.Wikos.hoppersadditions.Item.HopperBlockItem;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import java.util.Set;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class Registration {
    public static final String MOD_ID = "hoppersadditions";

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(MOD_ID, Registries.BLOCK);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(MOD_ID, Registries.ITEM);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(MOD_ID, Registries.BLOCK_ENTITY_TYPE);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(MOD_ID, Registries.CREATIVE_MODE_TAB);

    // --- BLOCKS ---
    public static final RegistrySupplier<Block> OVERFLOW_HOPPER = BLOCKS.register("overflow_hopper",
            () -> new OverflowHopperBlock(blockProperties("overflow_hopper")));

    public static final RegistrySupplier<Block> REINFORCED_HOPPER = BLOCKS.register("reinforced_hopper",
            () -> new ReinforcedHopperBlock(blockProperties("reinforced_hopper")));

    public static final RegistrySupplier<Block> PICKAXE_HOPPER = BLOCKS.register("pickaxe_hopper",
            () -> new PickaxeHopperBlock(blockProperties("pickaxe_hopper")));

    public static final RegistrySupplier<Block> DROPPING_HOPPER = BLOCKS.register("dropping_hopper",
            () -> new DroppingHopperBlock(blockProperties("dropping_hopper")));

    public static final RegistrySupplier<Block> MAGNETIC_HOPPER = BLOCKS.register("magnetic_hopper",
            () -> new MagneticHopperBlock(blockProperties("magnetic_hopper")));

    public static final RegistrySupplier<Block> FILTER_HOPPER = BLOCKS.register("filter_hopper",
            () -> new FilterHopperBlock(blockProperties("filter_hopper")));

    public static final RegistrySupplier<Block> MONSTER_HOPPER = BLOCKS.register("monster_hopper",
            () -> new MobHopperBlock(blockProperties("monster_hopper"), true));

    public static final RegistrySupplier<Block> ANIMAL_HOPPER = BLOCKS.register("animal_hopper",
            () -> new MobHopperBlock(blockProperties("animal_hopper"), false));

    public static final RegistrySupplier<Block> BATCH_HOPPER = BLOCKS.register("batch_hopper",
            () -> new me.Wikos.hoppersadditions.Block.Hoppers.BatchHopperBlock(blockProperties("batch_hopper"), 64));

    public static final RegistrySupplier<Block> SMALL_BATCH_HOPPER = BLOCKS.register("small_batch_hopper",
            () -> new me.Wikos.hoppersadditions.Block.Hoppers.BatchHopperBlock(blockProperties("small_batch_hopper"), 8));

    // --- ITEMS ---
    public static final RegistrySupplier<Item> OVERFLOW_HOPPER_ITEM = ITEMS.register("overflow_hopper",
            () -> new HopperBlockItem(OVERFLOW_HOPPER.get(), itemProperties("overflow_hopper")));


    public static final RegistrySupplier<Item> REINFORCED_HOPPER_ITEM = ITEMS.register("reinforced_hopper",
            () -> new HopperBlockItem(REINFORCED_HOPPER.get(), itemProperties("reinforced_hopper")));

    public static final RegistrySupplier<Item> PICKAXE_HOPPER_ITEM = ITEMS.register("pickaxe_hopper",
            () -> new PickaxeHopperItem(PICKAXE_HOPPER.get(), itemProperties("pickaxe_hopper").stacksTo(1).enchantable(10)));

    public static final RegistrySupplier<Item> DROPPING_HOPPER_ITEM = ITEMS.register("dropping_hopper",
            () -> new HopperBlockItem(DROPPING_HOPPER.get(), itemProperties("dropping_hopper")));

    public static final RegistrySupplier<Item> MAGNETIC_HOPPER_ITEM = ITEMS.register("magnetic_hopper",
            () -> new HopperBlockItem(MAGNETIC_HOPPER.get(), itemProperties("magnetic_hopper")));

    public static final RegistrySupplier<Item> FILTER_HOPPER_ITEM = ITEMS.register("filter_hopper",
            () -> new HopperBlockItem(FILTER_HOPPER.get(), itemProperties("filter_hopper")));

    public static final RegistrySupplier<Item> MONSTER_HOPPER_ITEM = ITEMS.register("monster_hopper",
            () -> new HopperBlockItem(MONSTER_HOPPER.get(), itemProperties("monster_hopper")));

    public static final RegistrySupplier<Item> ANIMAL_HOPPER_ITEM = ITEMS.register("animal_hopper",
            () -> new HopperBlockItem(ANIMAL_HOPPER.get(), itemProperties("animal_hopper")));

    public static final RegistrySupplier<Item> BATCH_HOPPER_ITEM = ITEMS.register("batch_hopper",
            () -> new HopperBlockItem(BATCH_HOPPER.get(), itemProperties("batch_hopper")));

    public static final RegistrySupplier<Item> SMALL_BATCH_HOPPER_ITEM = ITEMS.register("small_batch_hopper",
            () -> new HopperBlockItem(SMALL_BATCH_HOPPER.get(), itemProperties("small_batch_hopper")));

    // --- BLOCKS_ENTITY ---
    public static final RegistrySupplier<BlockEntityType<OverflowHopperBlockEntity>> OVERFLOW_HOPPER_ENTITY =
            BLOCK_ENTITIES.register("overflow_hopper", () ->
                    createBlockEntityType(OverflowHopperBlockEntity::new, OVERFLOW_HOPPER.get()));


    public static final RegistrySupplier<BlockEntityType<ReinforcedHopperBlockEntity>> REINFORCED_HOPPER_ENTITY =
            BLOCK_ENTITIES.register("reinforced_hopper", () ->
                    createBlockEntityType(ReinforcedHopperBlockEntity::new, REINFORCED_HOPPER.get()));

    public static final RegistrySupplier<BlockEntityType<PickaxeHopperBlockEntity>> PICKAXE_HOPPER_ENTITY =
            BLOCK_ENTITIES.register("pickaxe_hopper", () ->
                    createBlockEntityType(PickaxeHopperBlockEntity::new, PICKAXE_HOPPER.get()));

    public static final RegistrySupplier<BlockEntityType<DroppingHopperBlockEntity>> DROPPING_HOPPER_ENTITY =
            BLOCK_ENTITIES.register("dropping_hopper", () ->
                    createBlockEntityType(DroppingHopperBlockEntity::new, DROPPING_HOPPER.get()));

    public static final RegistrySupplier<BlockEntityType<MagneticHopperBlockEntity>> MAGNETIC_HOPPER_ENTITY =
            BLOCK_ENTITIES.register("magnetic_hopper", () ->
                    createBlockEntityType(MagneticHopperBlockEntity::new, MAGNETIC_HOPPER.get()));

    public static final RegistrySupplier<BlockEntityType<FilterHopperBlockEntity>> FILTER_HOPPER_ENTITY =
            BLOCK_ENTITIES.register("filter_hopper", () ->
                    createBlockEntityType(FilterHopperBlockEntity::new, FILTER_HOPPER.get()));

    public static final RegistrySupplier<BlockEntityType<MobHopperBlockEntity>> MONSTER_HOPPER_ENTITY =
            BLOCK_ENTITIES.register("monster_hopper", () ->
                    createBlockEntityType((pos, state) -> new MobHopperBlockEntity(pos, state, true), MONSTER_HOPPER.get()));

    public static final RegistrySupplier<BlockEntityType<MobHopperBlockEntity>> ANIMAL_HOPPER_ENTITY =
            BLOCK_ENTITIES.register("animal_hopper", () ->
                    createBlockEntityType((pos, state) -> new MobHopperBlockEntity(pos, state, false), ANIMAL_HOPPER.get()));

    public static final RegistrySupplier<BlockEntityType<me.Wikos.hoppersadditions.BlockEntity.Hoppers.BatchHopperBlockEntity>> BATCH_HOPPER_ENTITY =
            BLOCK_ENTITIES.register("batch_hopper", () ->
                    createBlockEntityType((pos, state) -> {
                        int amount = state.getBlock() == SMALL_BATCH_HOPPER.get() ? 8 : 64;
                        return new me.Wikos.hoppersadditions.BlockEntity.Hoppers.BatchHopperBlockEntity(pos, state, amount);
                    }, BATCH_HOPPER.get(), SMALL_BATCH_HOPPER.get()));

    public static final RegistrySupplier<Block> SPLITTING_HOPPER = BLOCKS.register("splitting_hopper",
            () -> new SplittingHopperBlock(blockProperties("splitting_hopper")));

    public static final RegistrySupplier<Item> SPLITTING_HOPPER_ITEM = ITEMS.register("splitting_hopper",
            () -> new HopperBlockItem(SPLITTING_HOPPER.get(), itemProperties("splitting_hopper")));

    public static final RegistrySupplier<BlockEntityType<SplittingHopperBlockEntity>> SPLITTING_HOPPER_ENTITY =
            BLOCK_ENTITIES.register("splitting_hopper", () ->
                    createBlockEntityType(SplittingHopperBlockEntity::new, SPLITTING_HOPPER.get()));

    public static final RegistrySupplier<Block> PULSE_HOPPER = BLOCKS.register("pulse_hopper",
            () -> new PulseHopperBlock(blockProperties("pulse_hopper")));

    public static final RegistrySupplier<Item> PULSE_HOPPER_ITEM = ITEMS.register("pulse_hopper",
            () -> new HopperBlockItem(PULSE_HOPPER.get(), itemProperties("pulse_hopper")));

    public static final RegistrySupplier<BlockEntityType<PulseHopperBlockEntity>> PULSE_HOPPER_ENTITY =
            BLOCK_ENTITIES.register("pulse_hopper", () ->
                    createBlockEntityType(PulseHopperBlockEntity::new, PULSE_HOPPER.get()));

    // --- TABS ---

    public static final RegistrySupplier<CreativeModeTab> HOPPER_TAB = TABS.register("hoppers_tab", () ->
            CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
                    .title(Component.translatable("itemGroup." + MOD_ID + ".hoppers_tab"))
                    .icon(() -> new ItemStack(REINFORCED_HOPPER.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(OVERFLOW_HOPPER_ITEM.get());
                        output.accept(REINFORCED_HOPPER_ITEM.get());
                        output.accept(PICKAXE_HOPPER_ITEM.get());
                        output.accept(DROPPING_HOPPER_ITEM.get());
                        output.accept(MAGNETIC_HOPPER_ITEM.get());
                        output.accept(FILTER_HOPPER_ITEM.get());
                        output.accept(MONSTER_HOPPER_ITEM.get());
                        output.accept(ANIMAL_HOPPER_ITEM.get());
                        output.accept(BATCH_HOPPER_ITEM.get());
                        output.accept(SMALL_BATCH_HOPPER_ITEM.get());
                        output.accept(PULSE_HOPPER_ITEM.get());
                        output.accept(SPLITTING_HOPPER_ITEM.get());
                    })
                    .build()
    );

    private static BlockBehaviour.Properties blockProperties(String name) {
        return BlockBehaviour.Properties.ofFullCopy(Blocks.HOPPER)
                .setId(ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(MOD_ID, name)));
    }

    private static Item.Properties itemProperties(String name) {
        return new Item.Properties().useBlockDescriptionPrefix()
                .setId(ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(MOD_ID, name)));
    }

    private static <T extends BlockEntity> BlockEntityType<T> createBlockEntityType(
            BlockEntityType.BlockEntitySupplier<T> factory, Block... blocks) {
        return new BlockEntityType<>(factory, Set.of(blocks));
    }

    public static void init() {
        BLOCKS.register();
        ITEMS.register();
        BLOCK_ENTITIES.register();
        TABS.register();
    }
}
