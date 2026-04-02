package me.Wikos.hoppersadditions.Block;

import me.Wikos.hoppersadditions.BlockEntity.ModdedHopperBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.NotNull;

public class ModdedHopperBlock extends HopperBlock  {
    public ModdedHopperBlock(Properties properties) {
        super(properties);

        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.DOWN)
                .setValue(ENABLED, Boolean.TRUE));
    }

    protected @NotNull InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        } else {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof ModdedHopperBlockEntity hopperEntity) {
                player.openMenu(hopperEntity);
            }
            return InteractionResult.CONSUME;
        }
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
}
