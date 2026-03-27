package me.Wikos.hoppersadditions.BlockEntity;

import me.Wikos.hoppersadditions.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class DroppingHopperBlockEntity extends ModdedHopperBlockEntity{
    public static final int MOVE_ITEM_SPEED = 8;

    public DroppingHopperBlockEntity(BlockPos pos, BlockState state) {
        super(Registration.DROPPING_HOPPER_ENTITY.get(), pos, state, MOVE_ITEM_SPEED);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, DroppingHopperBlockEntity blockEntity) {
        ModdedHopperBlockEntity.pushItemsTick(level, pos, state, blockEntity);
    }

    @Override
    protected @NotNull net.minecraft.network.chat.Component getDefaultName() {
        return Component.translatable("container.dropping_hopper");
    }

}
