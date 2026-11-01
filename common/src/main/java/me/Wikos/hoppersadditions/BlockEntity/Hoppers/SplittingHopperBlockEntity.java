package me.Wikos.hoppersadditions.BlockEntity.Hoppers;

import me.Wikos.hoppersadditions.BlockEntity.ModdedHopperBlockEntity;
import me.Wikos.hoppersadditions.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class SplittingHopperBlockEntity extends ModdedHopperBlockEntity {
    private static final int TRANSFER_COOLDOWN = 8;
    private boolean nextDown;

    public SplittingHopperBlockEntity(BlockPos pos, BlockState state) {
        super(Registration.SPLITTING_HOPPER_ENTITY.get(), pos, state, TRANSFER_COOLDOWN);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.splitting_hopper");
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.loadAdditional(tag, provider);
        nextDown = tag.getBoolean("NextDown");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider provider) {
        super.saveAdditional(tag, provider);
        tag.putBoolean("NextDown", nextDown);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SplittingHopperBlockEntity hopper) {
        hopper.tickedGameTime = level.getGameTime();
        if (hopper.cooldownTime > 0) {
            --hopper.cooldownTime;
            hopper.setChanged();
        }
        if (!level.isClientSide && !hopper.isOnCooldown() && state.getValue(HopperBlock.ENABLED)) {
            boolean moved = hopper.output(level, pos, state);
            moved |= hopper.suckInItems(level);
            if (moved) {
                hopper.setCooldown(TRANSFER_COOLDOWN);
                hopper.setChanged();
            }
        }
    }

    private boolean output(Level level, BlockPos pos, BlockState state) {
        Direction direction = nextDown ? Direction.DOWN : state.getValue(HopperBlock.FACING);
        BlockPos targetPos = pos.relative(direction);
        if (!level.hasChunkAt(targetPos)) {
            return false;
        }
        Container target = HopperBlockEntity.getContainerAt(level, targetPos);
        if (target == null || target == this) {
            return false;
        }
        for (int slot = 0; slot < getContainerSize(); ++slot) {
            if (transferOne(this, slot, target, direction.getOpposite())) {
                nextDown = !nextDown;
                return true;
            }
        }
        return false;
    }

    private static boolean transferOne(Container source, int slot, Container target, Direction face) {
        ItemStack original = source.getItem(slot).copy();
        if (original.isEmpty()) {
            return false;
        }
        ItemStack extracted = source.removeItem(slot, 1);
        if (!extracted.isEmpty() && insert(source, target, extracted, face).isEmpty()) {
            source.setChanged();
            return true;
        }
        source.setItem(slot, original);
        return false;
    }

    private static ItemStack insert(Container source, Container target, ItemStack remaining, Direction face) {
        int[] slots = target instanceof WorldlyContainer sided && face != null
                ? sided.getSlotsForFace(face) : java.util.stream.IntStream.range(0, target.getContainerSize()).toArray();
        for (int slot : slots) {
            if (remaining.isEmpty()) {
                break;
            }
            if (!target.canPlaceItem(slot, remaining)
                    || target instanceof WorldlyContainer sided && face != null
                    && !sided.canPlaceItemThroughFace(slot, remaining, face)) {
                continue;
            }
            ItemStack present = target.getItem(slot);
            if (!present.isEmpty() && !ItemStack.isSameItemSameComponents(present, remaining)) {
                continue;
            }
            int limit = Math.min(target.getMaxStackSize(remaining), remaining.getMaxStackSize());
            int amount = Math.min(remaining.getCount(), limit - present.getCount());
            if (amount <= 0) {
                continue;
            }
            boolean wasEmpty = target.isEmpty();
            if (present.isEmpty()) {
                target.setItem(slot, remaining.split(amount));
            } else {
                present.grow(amount);
                remaining.shrink(amount);
            }
            if (wasEmpty && target instanceof HopperBlockEntity vanilla && vanilla.cooldownTime <= TRANSFER_COOLDOWN) {
                vanilla.cooldownTime = TRANSFER_COOLDOWN;
            } else if (wasEmpty && target instanceof ModdedHopperBlockEntity hopper && !hopper.isOnCustomCooldown()) {
                int adjustment = source instanceof ModdedHopperBlockEntity sender
                        && hopper.tickedGameTime >= sender.tickedGameTime ? 1 : 0;
                hopper.setCooldown(hopper.itemMoveSpeed - adjustment);
            }
            target.setChanged();
        }
        return remaining;
    }

    @Override
    public boolean suckInItems(Level level) {
        BlockPos above = worldPosition.above();
        if (!level.hasChunkAt(above)) {
            return false;
        }
        Container source = HopperBlockEntity.getContainerAt(level, above);
        if (source != null) {
            for (int slot : getSlots(source, Direction.DOWN)) {
                ItemStack stack = source.getItem(slot);
                if (!stack.isEmpty() && source.canTakeItem(this, slot, stack)
                        && (!(source instanceof WorldlyContainer sided)
                        || sided.canTakeItemThroughFace(slot, stack, Direction.DOWN))
                        && transferOne(source, slot, this, null)) {
                    return true;
                }
            }
            return false;
        }
        BlockState aboveState = level.getBlockState(above);
        if (aboveState.isCollisionShapeFullBlock(level, above) && !aboveState.is(BlockTags.DOES_NOT_BLOCK_HOPPERS)) {
            return false;
        }
        for (ItemEntity entity : HopperBlockEntity.getItemsAtAndAbove(level, this)) {
            if (collectItem(entity)) {
                return true;
            }
        }
        return false;
    }

    private boolean collectItem(ItemEntity entity) {
        if (!entity.isAlive() || entity.getItem().isEmpty()) {
            return false;
        }
        ItemStack remaining = entity.getItem().copy();
        int before = remaining.getCount();
        insert(null, this, remaining, null);
        if (remaining.getCount() == before) {
            return false;
        }
        entity.setItem(remaining);
        if (remaining.isEmpty()) {
            entity.discard();
        }
        return true;
    }

    public void collectEntity(Level level, BlockState state, Entity entity) {
        if (!level.isClientSide && !isOnCooldown() && state.getValue(HopperBlock.ENABLED)
                && entity instanceof ItemEntity item
                && entity.getBoundingBox().move(-worldPosition.getX(), -worldPosition.getY(), -worldPosition.getZ())
                .intersects(getSuckAabb()) && collectItem(item)) {
            setCooldown(TRANSFER_COOLDOWN);
            setChanged();
        }
    }
}
