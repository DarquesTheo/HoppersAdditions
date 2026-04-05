package me.Wikos.hoppersadditions.BlockEntity.Hoppers;

import me.Wikos.hoppersadditions.BlockEntity.ModdedHopperBlockEntity;
import me.Wikos.hoppersadditions.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class FilterHopperBlockEntity extends ModdedHopperBlockEntity {
    public static final int MOVE_ITEM_SPEED = 8;
    public static final int FILTER_SLOT = 4;

    public FilterHopperBlockEntity(BlockPos pos, BlockState state) {
        super(Registration.FILTER_HOPPER_ENTITY.get(), pos, state, MOVE_ITEM_SPEED);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, FilterHopperBlockEntity blockEntity) {
        --blockEntity.cooldownTime;
        blockEntity.tickedGameTime = level.getGameTime();

        if (!blockEntity.isOnCooldown()) {
            blockEntity.setCooldown(0);

            if (!level.isClientSide && state.getValue(HopperBlock.ENABLED)) {
                boolean changed = false;

                if (!blockEntity.isEmpty()) {
                    changed = ejectFiltered(level, pos, blockEntity);
                }

                if (!blockEntity.inventoryFull()) {
                    changed |= blockEntity.suckInItems(level);
                }

                if (changed) {
                    blockEntity.setCooldown(blockEntity.itemMoveSpeed);
                    setChanged(level, pos, state);
                }
            }
        }
    }

    private static boolean ejectFiltered(Level level, BlockPos pos, FilterHopperBlockEntity hopper) {
        Container target = getAttachedContainer(level, pos, hopper);
        if (target == null) return false;

        Direction facingSide = hopper.facing.getOpposite();

        for (int i = 0; i < 4; i++) {
            ItemStack stack = hopper.getItem(i);
            if (!stack.isEmpty()) {
                ItemStack moved = addItem(hopper, target, hopper.removeItem(i, 1), facingSide);
                if (moved.isEmpty()) {
                    target.setChanged();
                    return true;
                }
                hopper.setItem(i, stack);
            }
        }
        return false;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (slot == FILTER_SLOT) {
            return false;
        }

        ItemStack filterStack = this.getItem(FILTER_SLOT);
        return filterStack.isEmpty() || ItemStack.isSameItemSameComponents(stack, filterStack);
    }

    @Override
    public boolean canTakeItem(Container container, int slot, ItemStack stack) {
        if (container == this && slot == FILTER_SLOT) {
            return false;
        }

        ItemStack filterStack = this.getItem(FILTER_SLOT);
        return filterStack.isEmpty() || ItemStack.isSameItemSameComponents(stack, filterStack);
    }


    @Override
    public boolean suckInItems(Level level) {
        ItemStack filterStack = this.getItem(FILTER_SLOT);

        BlockPos abovePos = this.getBlockPos().above();
        BlockState aboveState = level.getBlockState(abovePos);
        Container sourceContainer = getContainerAt(level, abovePos);

        if (sourceContainer != null) {
            Direction side = Direction.DOWN;
            for (int slot : getSlots(sourceContainer, side)) {
                ItemStack stackInSlot = sourceContainer.getItem(slot);

                //Only pull if filter is empty or item matches
                if (!stackInSlot.isEmpty() && (filterStack.isEmpty() || ItemStack.isSameItemSameComponents(stackInSlot, filterStack))) {
                    if (tryTakeInItemFromSlot(this, sourceContainer, slot, side)) {
                        return true;
                    }
                }
            }
            return false;
        } else {
            for (ItemEntity itemEntity : getItemsAtAndAbove(level, this)) {
                ItemStack entityStack = itemEntity.getItem();
                if (filterStack.isEmpty() || ItemStack.isSameItemSameComponents(entityStack, filterStack)) {
                    if (addItem(this, itemEntity)) {
                        return true;
                    }
                }
            }
            return false;
        }
    }

    @Override
    protected @NotNull Component getDefaultName() {
        return Component.translatable("container.filter_hopper");
    }
}
