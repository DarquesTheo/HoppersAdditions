package me.Wikos.hoppersadditions.BlockEntity;

import me.Wikos.hoppersadditions.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.function.BooleanSupplier;

public class DroppingHopperBlockEntity extends ModdedHopperBlockEntity{
    public static final int MOVE_ITEM_SPEED = 8;

    public DroppingHopperBlockEntity(BlockPos pos, BlockState state) {
        super(Registration.DROPPING_HOPPER_ENTITY.get(), pos, state, MOVE_ITEM_SPEED);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, DroppingHopperBlockEntity blockEntity) {
        --blockEntity.cooldownTime;
        blockEntity.tickedGameTime = level.getGameTime();
        if (!blockEntity.isOnCooldown()) {
            blockEntity.setCooldown(0);
            tryDropItems(level, pos, state, blockEntity, () -> ModdedHopperBlockEntity.suckInItems(level, blockEntity));
        }
    }

    private static void tryDropItems(Level level, BlockPos pos, BlockState state, DroppingHopperBlockEntity blockEntity, BooleanSupplier booleanSupplier) {
        if (!level.isClientSide) {
            if (!blockEntity.isOnCooldown() && state.getValue(HopperBlock.ENABLED)) {
                boolean bl = false;
                if (!blockEntity.isEmpty()) {
                    bl = dropItems(level, pos, blockEntity);
                }

                if (!blockEntity.inventoryFull()) {
                    bl |= booleanSupplier.getAsBoolean();
                }

                if (bl) {
                    blockEntity.setCooldown(blockEntity.itemMoveSpeed);
                    setChanged(level, pos, state);
                }
            }
        }
    }

    private static boolean dropItems(Level level, BlockPos pos, DroppingHopperBlockEntity blockEntity) {
        for (int i = 0; i < blockEntity.getContainerSize(); ++i) {
            ItemStack itemStack = blockEntity.getItem(i);

            if (!itemStack.isEmpty()) {
                Direction direction = blockEntity.getBlockState().getValue(HopperBlock.FACING);
                double offset = (direction == Direction.DOWN) ? 0.9 : 0.7;

                double x = pos.getX() + 0.5 + offset * direction.getStepX();
                double y = pos.getY() + 0.5 + offset * direction.getStepY();
                double z = pos.getZ() + 0.5 + offset * direction.getStepZ();

                ItemStack toDrop = itemStack.copy();
                toDrop.setCount(1);

                net.minecraft.world.entity.item.ItemEntity itemEntity = new net.minecraft.world.entity.item.ItemEntity(
                        level, x, y, z, toDrop
                );

                itemEntity.setDeltaMovement(
                        direction.getStepX() * 0.1,
                        direction.getStepY() * 0.1,
                        direction.getStepZ() * 0.1
                );

                itemEntity.setPickUpDelay(10);

                level.addFreshEntity(itemEntity);
                itemStack.shrink(1);

                return true;
            }
        }
        return false;
    }

    @Override
    protected @NotNull net.minecraft.network.chat.Component getDefaultName() {
        return Component.translatable("container.dropping_hopper");
    }

}
