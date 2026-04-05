package me.Wikos.hoppersadditions.Block.Hoppers;

import me.Wikos.hoppersadditions.BlockEntity.Hoppers.MobHopperBlockEntity;
import me.Wikos.hoppersadditions.Registration;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
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

import java.util.List;

public class MobHopperBlock extends HopperBlock {

    private final boolean isMonsterVariant;

    public MobHopperBlock(Properties properties, boolean isMonsterVariant) {
        super(properties);

        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.DOWN)
                .setValue(ENABLED, Boolean.TRUE));
        this.isMonsterVariant = isMonsterVariant;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MobHopperBlockEntity(pos, state, isMonsterVariant);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (isMonsterVariant) {
            return level.isClientSide ? null : createTickerHelper(type, Registration.MONSTER_HOPPER_ENTITY.get(), MobHopperBlockEntity::tick);
        } else {
            return level.isClientSide ? null : createTickerHelper(type, Registration.ANIMAL_HOPPER_ENTITY.get(), MobHopperBlockEntity::tick);
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

    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        String keyName = "animal_hopper";
        if (isMonsterVariant) {
            keyName = "monster_hopper";
        }
        tooltipComponents.add(Component.translatable("tooltip.hoppersadditions." + keyName + ".description")
                .withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("tooltip.hoppersadditions.hopper.speed")
                .append(Component.literal(": 1x"))
                .withStyle(ChatFormatting.GOLD));
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
}
