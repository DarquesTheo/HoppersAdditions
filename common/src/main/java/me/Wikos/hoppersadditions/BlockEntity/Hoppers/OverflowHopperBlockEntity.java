package me.Wikos.hoppersadditions.BlockEntity.Hoppers;

import me.Wikos.hoppersadditions.BlockEntity.ModdedHopperBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

import java.util.function.BooleanSupplier;

public class OverflowHopperBlockEntity extends ModdedHopperBlockEntity {
    public static final int MOVE_ITEM_SPEED = 8;

    public OverflowHopperBlockEntity(BlockPos pos, BlockState state) {
        super(me.Wikos.hoppersadditions.Registration.OVERFLOW_HOPPER_ENTITY.get(), pos, state, MOVE_ITEM_SPEED);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, OverflowHopperBlockEntity hopper) {
        --hopper.cooldownTime;
        hopper.tickedGameTime = level.getGameTime();
        if (!hopper.isOnCooldown()) {
            hopper.setCooldown(0);
            hopper.transfer(level, pos, state, () -> HopperBlockEntity.suckInItems(level, hopper));
        }
    }

    public void collectItem(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (entity instanceof ItemEntity itemEntity && !itemEntity.getItem().isEmpty()
                && entity.getBoundingBox().move(-pos.getX(), -pos.getY(), -pos.getZ()).intersects(this.getSuckAabb())) {
            this.transfer(level, pos, state, () -> HopperBlockEntity.addItem(this, itemEntity));
        }
    }

    private void transfer(Level level, BlockPos pos, BlockState state, BooleanSupplier collect) {
        if (level.isClientSide() || this.isOnCooldown() || !state.getValue(HopperBlock.ENABLED)) {
            return;
        }
        boolean moved = this.pushOneItem(level, pos, state);
        if (!this.inventoryFull()) {
            moved |= collect.getAsBoolean();
        }
        if (moved) {
            this.setCooldown(MOVE_ITEM_SPEED);
            setChanged(level, pos, state);
        }
    }

    private boolean pushOneItem(Level level, BlockPos pos, BlockState state) {
        if (this.isEmpty()) {
            return false;
        }
        Direction front = state.getValue(HopperBlock.FACING);
        Container primary = HopperBlockEntity.getContainerAt(level, pos.relative(front));
        Container overflow = HopperBlockEntity.getContainerAt(level, pos.below());
        return this.pushOneItem(primary, overflow, front);
    }

    private boolean pushOneItem(Container primary, Container overflow, Direction front) {
        for (int slot = 0; slot < this.getContainerSize(); ++slot) {
            if (this.getItem(slot).isEmpty()) {
                continue;
            }
            ItemStack original = this.getItem(slot).copy();
            ItemStack candidate = this.removeItem(slot, 1);
            if (this.insertOne(primary, candidate, front.getOpposite())
                    || this.insertOne(overflow, candidate, Direction.UP)) {
                return true;
            }
            this.setItem(slot, original);
        }
        return false;
    }

    private boolean insertOne(Container target, ItemStack candidate, Direction face) {
        if (target == null || target == this) {
            return false;
        }
        for (int slot : getSlots(target, face)) {
            if (!target.canPlaceItem(slot, candidate)
                    || target instanceof WorldlyContainer sided && !sided.canPlaceItemThroughFace(slot, candidate, face)) {
                continue;
            }
            ItemStack stored = target.getItem(slot);
            int limit = target.getMaxStackSize(candidate);
            if (limit < 1 || !stored.isEmpty() && (!ItemStack.isSameItemSameComponents(stored, candidate)
                    || stored.getCount() >= limit)) {
                continue;
            }
            boolean wasEmpty = target.isEmpty();
            if (stored.isEmpty()) {
                target.setItem(slot, candidate);
            } else {
                stored.grow(1);
            }
            if (wasEmpty && target instanceof HopperBlockEntity hopper && hopper.cooldownTime <= MOVE_ITEM_SPEED) {
                hopper.cooldownTime = MOVE_ITEM_SPEED;
            } else if (wasEmpty && target instanceof ModdedHopperBlockEntity hopper && !hopper.isOnCustomCooldown()) {
                hopper.setCooldown(hopper.itemMoveSpeed);
            }
            target.setChanged();
            return true;
        }
        return false;
    }

    @Override
    protected @NotNull Component getDefaultName() {
        return Component.translatable("container.overflow_hopper");
    }
}
