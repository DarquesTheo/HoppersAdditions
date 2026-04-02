package me.Wikos.hoppersadditions;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import me.Wikos.hoppersadditions.Block.*;
import me.Wikos.hoppersadditions.BlockEntity.*;
import me.Wikos.hoppersadditions.Item.PickaxeHopperItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
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

    public static final RegistrySupplier<Block> REINFORCED_HOPPER = BLOCKS.register("reinforced_hopper",
            () -> new ReinforcedHopperBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.HOPPER)));

    public static final RegistrySupplier<Block> PICKAXE_HOPPER = BLOCKS.register("pickaxe_hopper",
            () -> new PickaxeHopperBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.HOPPER)));

    public static final RegistrySupplier<Block> DROPPING_HOPPER = BLOCKS.register("dropping_hopper",
            () -> new DroppingHopperBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.HOPPER)));

    public static final RegistrySupplier<Block> MAGNETIC_HOPPER = BLOCKS.register("magnetic_hopper",
            () -> new MagneticHopperBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.HOPPER)));

    public static final RegistrySupplier<Block> FILTER_HOPPER = BLOCKS.register("filter_hopper",
            () -> new FilterHopperBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.HOPPER)));


    public static final RegistrySupplier<Item> REINFORCED_HOPPER_ITEM = ITEMS.register("reinforced_hopper",
            () -> new BlockItem(REINFORCED_HOPPER.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> PICKAXE_HOPPER_ITEM = ITEMS.register("pickaxe_hopper",
            () -> new PickaxeHopperItem(PICKAXE_HOPPER.get(), new Item.Properties().stacksTo(1)));

    public static final RegistrySupplier<Item> DROPPING_HOPPER_ITEM = ITEMS.register("dropping_hopper",
            () -> new BlockItem(DROPPING_HOPPER.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> MAGNETIC_HOPPER_ITEM = ITEMS.register("magnetic_hopper",
            () -> new BlockItem(MAGNETIC_HOPPER.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> FILTER_HOPPER_ITEM = ITEMS.register("filter_hopper",
            () -> new BlockItem(FILTER_HOPPER.get(), new Item.Properties()));


    public static final RegistrySupplier<BlockEntityType<ReinforcedHopperBlockEntity>> REINFORCED_HOPPER_ENTITY =
            BLOCK_ENTITIES.register("reinforced_hopper", () ->
                    BlockEntityType.Builder.of(ReinforcedHopperBlockEntity::new, REINFORCED_HOPPER.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<PickaxeHopperBlockEntity>> PICKAXE_HOPPER_ENTITY =
            BLOCK_ENTITIES.register("pickaxe_hopper", () ->
                    BlockEntityType.Builder.of(PickaxeHopperBlockEntity::new, PICKAXE_HOPPER.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<DroppingHopperBlockEntity>> DROPPING_HOPPER_ENTITY =
            BLOCK_ENTITIES.register("dropping_hopper", () ->
                    BlockEntityType.Builder.of(DroppingHopperBlockEntity::new, DROPPING_HOPPER.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<MagneticHopperBlockEntity>> MAGNETIC_HOPPER_ENTITY =
            BLOCK_ENTITIES.register("magnetic_hopper", () ->
                    BlockEntityType.Builder.of(MagneticHopperBlockEntity::new, MAGNETIC_HOPPER.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<FilterHopperBlockEntity>> FILTER_HOPPER_ENTITY =
            BLOCK_ENTITIES.register("filter_hopper", () ->
                    BlockEntityType.Builder.of(FilterHopperBlockEntity::new, FILTER_HOPPER.get()).build(null));

    public static final RegistrySupplier<CreativeModeTab> HOPPER_TAB = TABS.register("hoppers_tab", () ->
            CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
                    .title(Component.translatable("itemGroup." + MOD_ID + ".hoppers_tab"))
                    .icon(() -> new ItemStack(REINFORCED_HOPPER.get()))
                    .displayItems((parameters, output) -> {
                        output.accept(REINFORCED_HOPPER_ITEM.get());
                        output.accept(PICKAXE_HOPPER_ITEM.get());
                        output.accept(DROPPING_HOPPER_ITEM.get());
                        output.accept(MAGNETIC_HOPPER_ITEM.get());
                        output.accept(FILTER_HOPPER_ITEM.get());
                    })
                    .build()
    );

    public static void init() {
        BLOCKS.register();
        ITEMS.register();
        BLOCK_ENTITIES.register();
        TABS.register();
    }
}
