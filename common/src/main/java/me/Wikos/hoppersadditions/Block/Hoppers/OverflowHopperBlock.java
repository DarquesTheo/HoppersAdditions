package me.Wikos.hoppersadditions.Block.Hoppers;

import me.Wikos.hoppersadditions.Block.ModdedHopperBlock;
import me.Wikos.hoppersadditions.BlockEntity.Hoppers.OverflowHopperBlockEntity;
import me.Wikos.hoppersadditions.Registration;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

public class OverflowHopperBlock extends ModdedHopperBlock {
    private static final VoxelShape BOTTOM_SPOUT = Block.box(6, 0, 6, 10, 4, 10);

    public OverflowHopperBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.defaultBlockState().setValue(FACING, Direction.NORTH));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction clickedFace = context.getClickedFace();
        Direction front = clickedFace.getAxis().isHorizontal()
                ? clickedFace.getOpposite()
                : context.getHorizontalDirection();
        return this.defaultBlockState()
                .setValue(FACING, front)
                .setValue(ENABLED, !context.getLevel().hasNeighborSignal(context.getClickedPos()));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new OverflowHopperBlockEntity(pos, state);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.or(super.getShape(state, level, pos, context), BOTTOM_SPOUT);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, Registration.OVERFLOW_HOPPER_ENTITY.get(), OverflowHopperBlockEntity::tick);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, net.minecraft.world.entity.InsideBlockEffectApplier effects, boolean applyEffects) {
        if (level.getBlockEntity(pos) instanceof OverflowHopperBlockEntity hopper) {
            hopper.collectItem(level, pos, state, entity);
        }
    }

    @Override
    public void appendHopperTooltip(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("tooltip.hoppersadditions.overflow_hopper.description")
                .withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("tooltip.hoppersadditions.hopper.speed")
                .append(Component.literal(": 1x"))
                .withStyle(ChatFormatting.GOLD));
        super.appendHopperTooltip(stack, context, tooltipComponents, tooltipFlag);
    }
}
