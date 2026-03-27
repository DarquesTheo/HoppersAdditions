package me.Wikos.hoppersadditions.Block;

import me.Wikos.hoppersadditions.BlockEntity.ReinforcedHopperBlockEntity;
import me.Wikos.hoppersadditions.Registration;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ReinforcedHopperBlock extends HopperBlock {
    public ReinforcedHopperBlock(Properties properties) {
        super(properties);

        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.DOWN)
                .setValue(ENABLED, Boolean.TRUE));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ReinforcedHopperBlockEntity(pos, state);
    }

    protected @NotNull InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        } else {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof ReinforcedHopperBlockEntity hopperEntity) {
                player.openMenu(hopperEntity);
            }
            return InteractionResult.CONSUME;
        }
    }

    @Override
    public <T extends BlockEntity>BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, Registration.REINFORCED_HOPPER_ENTITY.get(), ReinforcedHopperBlockEntity::tick);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction clickedFace = context.getClickedFace();

        Direction targetDirection = clickedFace.getAxis() == Direction.Axis.Y
                ? Direction.DOWN
                : clickedFace.getOpposite();

        return this.defaultBlockState()
                .setValue(FACING, targetDirection)
                .setValue(ENABLED, Boolean.TRUE);
    }

    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("tooltip.hoppersadditions.reinforced_hopper.description")
                .withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("tooltip.hoppersadditions.reinforced_hopper.speed")
                .append(Component.literal(": 2x"))
                .withStyle(ChatFormatting.GOLD));
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
}
