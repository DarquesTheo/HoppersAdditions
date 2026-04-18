package me.Wikos.hoppersadditions.Block.Hoppers;

import me.Wikos.hoppersadditions.Block.ModdedHopperBlock;
import me.Wikos.hoppersadditions.BlockEntity.Hoppers.BatchHopperBlockEntity;
import me.Wikos.hoppersadditions.Registration;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class BatchHopperBlock extends ModdedHopperBlock {
    private final int batchAmount;

    public BatchHopperBlock(Properties properties, int batchAmount) {
        super(properties);
        this.batchAmount = batchAmount;

        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.DOWN)
                .setValue(ENABLED, Boolean.TRUE));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new me.Wikos.hoppersadditions.BlockEntity.Hoppers.BatchHopperBlockEntity(pos, state, this.batchAmount);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, Registration.BATCH_HOPPER_ENTITY.get(), me.Wikos.hoppersadditions.BlockEntity.Hoppers.BatchHopperBlockEntity::tick);
    }

    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        String key = this.batchAmount == 8 ? "tooltip.hoppersadditions.small_batch_hopper.description" : "tooltip.hoppersadditions.batch_hopper.description";
        tooltipComponents.add(Component.translatable(key)
                .withStyle(ChatFormatting.GRAY));
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
}
