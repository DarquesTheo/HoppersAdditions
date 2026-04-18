package me.Wikos.hoppersadditions.BlockEntity.Hoppers;

import me.Wikos.hoppersadditions.BlockEntity.ModdedHopperBlockEntity;
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

public class BatchHopperBlockEntity extends ModdedHopperBlockEntity {
    public static final int MOVE_ITEM_SPEED = 8;
    private final int batchAmount;

    public BatchHopperBlockEntity(BlockPos pos, BlockState state, int batchAmount) {
        super(Registration.BATCH_HOPPER_ENTITY.get(), pos, state, MOVE_ITEM_SPEED);
        this.batchAmount = batchAmount;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, BatchHopperBlockEntity blockEntity) {
        pushItemsTickLocal(level, pos, state, blockEntity);
    }

    private static void pushItemsTickLocal(Level level, BlockPos blockPos, BlockState blockState, BatchHopperBlockEntity batchEntity) {
        --batchEntity.cooldownTime;
        batchEntity.tickedGameTime = level.getGameTime();
        if (!batchEntity.isOnCooldown()) {
            batchEntity.setCooldown(0);
            tryMoveItemsLocal(level, blockPos, blockState, batchEntity, () -> batchEntity.suckInItems(level));
        }
    }

    private static void tryMoveItemsLocal(Level level, BlockPos blockPos, BlockState blockState, BatchHopperBlockEntity batchEntity, BooleanSupplier booleanSupplier) {
        if (!level.isClientSide) {
            if (!batchEntity.isOnCooldown() && blockState.getValue(HopperBlock.ENABLED)) {
                boolean bl = false;
                if (!batchEntity.isEmpty()) {
                    bl = ejectItemsLocal(level, blockPos, batchEntity);
                }

                if (!batchEntity.inventoryFull()) {
                    bl |= booleanSupplier.getAsBoolean();
                }

                if (bl) {
                    batchEntity.setCooldown(batchEntity.itemMoveSpeed);
                    batchEntity.setChanged();
                }
            }
        }
    }

    private static boolean ejectItemsLocal(Level level, BlockPos blockPos, BatchHopperBlockEntity batchEntity) {
        Container container = ModdedHopperBlockEntity.getAttachedContainer(level, blockPos, batchEntity);
        if (container != null) {
            Direction direction = batchEntity.facing.getOpposite();
            if (!ModdedHopperBlockEntity.isFullContainer(container, direction)) {
                for (int i = 0; i < batchEntity.getContainerSize(); ++i) {
                    ItemStack itemStack = batchEntity.getItem(i);
                    int targetAmount = Math.min(batchEntity.batchAmount, itemStack.getMaxStackSize());
                    
                    if (!itemStack.isEmpty() && itemStack.getCount() >= targetAmount) {
                        ItemStack removed = batchEntity.removeItem(i, targetAmount);
                        
                        // Push the stack into the container
                        ItemStack remainder = ModdedHopperBlockEntity.addItem(batchEntity, container, removed, direction);
                        
                        if (remainder.getCount() < targetAmount) {
                            container.setChanged();
                            if (!remainder.isEmpty()) {
                                // Put any items that didn't fit back into the slot
                                batchEntity.setItem(i, remainder);
                            }
                            return true;
                        } else {
                            // Put all items back if nothing was inserted
                            batchEntity.setItem(i, remainder);
                        }
                    }
                }
            }
        }
        return false;
    }

    @Override
    protected @NotNull Component getDefaultName() {
        String key = this.batchAmount == 8 ? "container.small_batch_hopper" : "container.batch_hopper";
        return Component.translatable(key);
    }
}
