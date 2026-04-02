package me.Wikos.hoppersadditions.BlockEntity;

import me.Wikos.hoppersadditions.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.NotNull;

public class MagneticHopperBlockEntity extends ModdedHopperBlockEntity {
    public static final int MOVE_ITEM_SPEED = 8; // 1x speed

    public MagneticHopperBlockEntity(BlockPos pos, BlockState state) {
        super(Registration.MAGNETIC_HOPPER_ENTITY.get(), pos, state, MOVE_ITEM_SPEED);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MagneticHopperBlockEntity blockEntity) {
        ModdedHopperBlockEntity.pushItemsTick(level, pos, state, blockEntity);
    }

    @NotNull
    @Override
    public AABB getSuckAabb() {
        return new AABB(-4.0D, -4.0D, -4.0D, 5.0D, 5.0D, 5.0D);
    }

    @Override
    protected @NotNull Component getDefaultName() {
        return Component.translatable("container.magnetic_hopper");
    }
}
