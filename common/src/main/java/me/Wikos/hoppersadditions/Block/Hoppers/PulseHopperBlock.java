package me.Wikos.hoppersadditions.Block.Hoppers;

import me.Wikos.hoppersadditions.BlockEntity.Hoppers.PulseHopperBlockEntity;
import me.Wikos.hoppersadditions.Registration;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.List;

public class PulseHopperBlock extends HopperBlock implements me.Wikos.hoppersadditions.Item.HopperTooltip {
    public PulseHopperBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return super.getStateForPlacement(context)
                .setValue(ENABLED, !context.getLevel().hasNeighborSignal(context.getClickedPos()));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PulseHopperBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, Registration.PULSE_HOPPER_ENTITY.get(), PulseHopperBlockEntity::tick);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!oldState.is(this)) {
            updatePower(level, pos, true);
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbor, net.minecraft.world.level.redstone.Orientation orientation, boolean movedByPiston) {
        updatePower(level, pos, false);
    }

    private void updatePower(Level level, BlockPos pos, boolean initialize) {
        if (level.isClientSide()) {
            return;
        }
        boolean powered = level.hasNeighborSignal(pos);
        BlockState state = level.getBlockState(pos);
        if (state.getValue(ENABLED) == powered) {
            level.setBlock(pos, state.setValue(ENABLED, !powered), Block.UPDATE_CLIENTS);
        }
        if (level.getBlockEntity(pos) instanceof PulseHopperBlockEntity hopper) {
            if (initialize) {
                hopper.initializePower(powered);
            } else {
                hopper.powerChanged(powered);
            }
        }
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, net.minecraft.world.entity.InsideBlockEffectApplier effects, boolean applyEffects) {
        if (!level.isClientSide() && entity instanceof ItemEntity itemEntity
                && level.getBlockEntity(pos) instanceof PulseHopperBlockEntity hopper
                && entity.getBoundingBox().move(-pos.getX(), -pos.getY(), -pos.getZ()).intersects(hopper.getSuckAabb())) {
            hopper.collectItem(itemEntity);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof PulseHopperBlockEntity hopper) {
            player.openMenu(hopper);
            player.awardStat(Stats.INSPECT_HOPPER);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void appendHopperTooltip(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.hoppersadditions.pulse_hopper.description").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.hoppersadditions.pulse_hopper.input").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.hoppersadditions.pulse_hopper.failed").withStyle(ChatFormatting.GRAY));

    }
}
