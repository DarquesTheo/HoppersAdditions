package me.Wikos.hoppersadditions.BlockEntity;

import me.Wikos.hoppersadditions.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class ReinforcedHopperBlockEntity extends ModdedHopperBlockEntity {
    public static final int MOVE_ITEM_SPEED = 4;

    public ReinforcedHopperBlockEntity(BlockPos pos, BlockState state) {
        super(Registration.REINFORCED_HOPPER_ENTITY.get(), pos, state, MOVE_ITEM_SPEED);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ReinforcedHopperBlockEntity blockEntity) {
        ModdedHopperBlockEntity.pushItemsTick(level, pos, state, blockEntity);
    }

    @Override
    protected @NotNull net.minecraft.network.chat.Component getDefaultName() {
        return Component.translatable("container.reinforced_hopper");
    }
}
