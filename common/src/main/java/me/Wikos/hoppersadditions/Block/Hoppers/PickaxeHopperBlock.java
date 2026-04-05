package me.Wikos.hoppersadditions.Block.Hoppers;

import me.Wikos.hoppersadditions.Block.ModdedHopperBlock;
import me.Wikos.hoppersadditions.BlockEntity.Hoppers.PickaxeHopperBlockEntity;
import me.Wikos.hoppersadditions.Registration;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

public class PickaxeHopperBlock extends ModdedHopperBlock {
    public PickaxeHopperBlock(Properties properties) {
        super(properties);

        this.registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.DOWN)
                .setValue(ENABLED, Boolean.TRUE));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PickaxeHopperBlockEntity(pos, state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof PickaxeHopperBlockEntity pickaxeHopper) {
            var enchantmentRegistry = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);

            int eff = stack.getEnchantments().getLevel(enchantmentRegistry.getOrThrow(Enchantments.EFFICIENCY));
            int fort = stack.getEnchantments().getLevel(enchantmentRegistry.getOrThrow(Enchantments.FORTUNE));

            pickaxeHopper.setEnchantments(eff, fort);
        }
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, Registration.PICKAXE_HOPPER_ENTITY.get(), PickaxeHopperBlockEntity::tick);
    }

    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
        tooltipComponents.add(Component.translatable("tooltip.hoppersadditions.pickaxe_hopper.description")
                .withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("tooltip.hoppersadditions.pickaxe_hopper.description_two")
                .withStyle(ChatFormatting.DARK_PURPLE));
        tooltipComponents.add(Component.translatable("tooltip.hoppersadditions.hopper.speed")
                .append(Component.literal(": 0.5x"))
                .withStyle(ChatFormatting.GOLD));
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }
}
