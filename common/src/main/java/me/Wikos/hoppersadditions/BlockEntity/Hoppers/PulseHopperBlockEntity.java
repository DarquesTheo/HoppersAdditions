package me.Wikos.hoppersadditions.BlockEntity.Hoppers;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.HopperMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.Hopper;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class PulseHopperBlockEntity extends RandomizableContainerBlockEntity implements Hopper {
    public static final int INPUT_COOLDOWN = 8;
    private NonNullList<ItemStack> items = NonNullList.withSize(5, ItemStack.EMPTY);
    private boolean powerInitialized;
    private boolean lastPowered;
    private long lastPulseGameTime = Long.MIN_VALUE;
    private long nextInputGameTime = Long.MIN_VALUE;

    public PulseHopperBlockEntity(BlockPos pos, BlockState state) {
        super(me.Wikos.hoppersadditions.Registration.PULSE_HOPPER_ENTITY.get(), pos, state);
    }

    public void initializePower(boolean powered) {
        this.powerInitialized = true;
        this.lastPowered = powered;
        this.setChanged();
    }

    public void powerChanged(boolean powered) {
        if (this.level == null || this.level.isClientSide()) {
            return;
        }
        if (!this.powerInitialized) {
            this.initializePower(powered);
            return;
        }
        if (this.lastPowered == powered) {
            return;
        }
        boolean pulse = powered && this.lastPulseGameTime != this.level.getGameTime();
        this.lastPowered = powered;
        if (pulse) {
            this.lastPulseGameTime = this.level.getGameTime();
        }
        this.setChanged();
        if (pulse) {
            this.ejectOneItem();
        }
    }

    private void ejectOneItem() {
        Direction facing = this.getBlockState().getValue(HopperBlock.FACING);
        Container target = HopperBlockEntity.getContainerAt(this.level, this.worldPosition.relative(facing));
        if (target == null || target == this) {
            return;
        }
        for (int slot = 0; slot < this.getContainerSize(); slot++) {
            ItemStack stored = this.getItem(slot);
            if (stored.isEmpty()) {
                continue;
            }
            ItemStack original = stored.copy();
            ItemStack remainder = HopperBlockEntity.addItem(this, target, this.removeItem(slot, 1), facing.getOpposite());
            if (remainder.isEmpty()) {
                target.setChanged();
                this.setChanged();
                return;
            }
            this.items.set(slot, original);
        }
    }

    public static void tick(Level level, BlockPos pos, BlockState state, PulseHopperBlockEntity hopper) {
        if (!hopper.canCollect()) {
            return;
        }
        if (HopperBlockEntity.getContainerAt(level, pos.above()) != null) {
            hopper.finishCollection(HopperBlockEntity.suckInItems(level, hopper));
        } else {
            for (ItemEntity itemEntity : HopperBlockEntity.getItemsAtAndAbove(level, hopper)) {
                hopper.collectItem(itemEntity);
                if (!hopper.canCollect()) {
                    break;
                }
            }
        }
    }

    public void collectItem(ItemEntity itemEntity) {
        if (!itemEntity.isAlive() || !this.canCollect()) {
            return;
        }
        int previousCount = itemEntity.getItem().getCount();
        HopperBlockEntity.addItem(this, itemEntity);
        this.finishCollection(itemEntity.getItem().getCount() < previousCount);
    }

    private boolean canCollect() {
        if (this.level == null || this.level.isClientSide()) {
            return false;
        }
        boolean powered = this.level.hasNeighborSignal(this.worldPosition);
        if (!this.powerInitialized) {
            this.initializePower(powered);
        }
        return !powered && this.level.getGameTime() >= this.nextInputGameTime;
    }

    private void finishCollection(boolean collected) {
        if (collected) {
            this.nextInputGameTime = this.level.getGameTime() + INPUT_COOLDOWN;
            this.setChanged();
        }
    }

    @Override
    protected void loadAdditional(ValueInput tag) {
        super.loadAdditional(tag);
        this.items = NonNullList.withSize(5, ItemStack.EMPTY);
        if (!this.tryLoadLootTable(tag)) {
            ContainerHelper.loadAllItems(tag, this.items);
        }
        this.powerInitialized = false;
        this.lastPowered = tag.getBooleanOr("PulseLastPowered", false);
        this.lastPulseGameTime = tag.getLongOr("PulseLastGameTime", Long.MIN_VALUE);
        this.nextInputGameTime = tag.getLongOr("PulseNextInputGameTime", Long.MIN_VALUE);
    }

    @Override
    protected void saveAdditional(ValueOutput tag) {
        super.saveAdditional(tag);
        if (!this.trySaveLootTable(tag)) {
            ContainerHelper.saveAllItems(tag, this.items);
        }
        tag.putBoolean("PulsePowerInitialized", this.powerInitialized);
        tag.putBoolean("PulseLastPowered", this.lastPowered);
        tag.putLong("PulseLastGameTime", this.lastPulseGameTime);
        tag.putLong("PulseNextInputGameTime", this.nextInputGameTime);
    }

    @Override
    public int getContainerSize() {
        return 5;
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return this.items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.pulse_hopper");
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new HopperMenu(containerId, inventory, this);
    }

    @Override
    public double getLevelX() {
        return this.worldPosition.getX() + 0.5;
    }

    @Override
    public double getLevelY() {
        return this.worldPosition.getY() + 0.5;
    }

    @Override
    public double getLevelZ() {
        return this.worldPosition.getZ() + 0.5;
    }

    @Override
    public boolean isGridAligned() {
        return true;
    }
}
