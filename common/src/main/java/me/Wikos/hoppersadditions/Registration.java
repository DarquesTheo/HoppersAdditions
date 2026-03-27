package me.Wikos.hoppersadditions;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import me.Wikos.hoppersadditions.Block.PickaxeHopperBlock;
import me.Wikos.hoppersadditions.Block.ReinforcedHopperBlock;
import me.Wikos.hoppersadditions.BlockEntity.PickaxeHopperBlockEntity;
import me.Wikos.hoppersadditions.BlockEntity.ReinforcedHopperBlockEntity;
import me.Wikos.hoppersadditions.Item.PickaxeHopperItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class Registration {
    public static final String MOD_ID = "hoppersadditions";

    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(MOD_ID, Registries.BLOCK);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(MOD_ID, Registries.ITEM);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(MOD_ID, Registries.BLOCK_ENTITY_TYPE);

    public static final RegistrySupplier<Block> REINFORCED_HOPPER = BLOCKS.register("reinforced_hopper",
            () -> new ReinforcedHopperBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.HOPPER)));

    public static final RegistrySupplier<Block> PICKAXE_HOPPER = BLOCKS.register("pickaxe_hopper",
            () -> new PickaxeHopperBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.HOPPER)));

    public static final RegistrySupplier<Item> REINFORCED_HOPPER_ITEM = ITEMS.register("reinforced_hopper",
            () -> new BlockItem(REINFORCED_HOPPER.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> PICKAXE_HOPPER_ITEM = ITEMS.register("pickaxe_hopper",
            () -> new PickaxeHopperItem(PICKAXE_HOPPER.get(), new Item.Properties().stacksTo(1)));

    public static final RegistrySupplier<BlockEntityType<ReinforcedHopperBlockEntity>> REINFORCED_HOPPER_ENTITY =
            BLOCK_ENTITIES.register("reinforced_hopper", () ->
                    BlockEntityType.Builder.of(ReinforcedHopperBlockEntity::new, REINFORCED_HOPPER.get()).build(null));

    public static final RegistrySupplier<BlockEntityType<PickaxeHopperBlockEntity>> PICKAXE_HOPPER_ENTITY =
            BLOCK_ENTITIES.register("pickaxe_hopper", () ->
                    BlockEntityType.Builder.of(PickaxeHopperBlockEntity::new, PICKAXE_HOPPER.get()).build(null));

    public static void init() {
        BLOCKS.register();
        ITEMS.register();
        BLOCK_ENTITIES.register();
    }
}
