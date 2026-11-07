package me.Wikos.hoppersadditions.Item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.block.Block;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class HopperBlockItem extends BlockItem {
    public HopperBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display,
                                Consumer<Component> tooltip, TooltipFlag flag) {
        if (getBlock() instanceof HopperTooltip hopper) {
            List<Component> lines = new ArrayList<>();
            hopper.appendHopperTooltip(stack, context, lines, flag);
            lines.forEach(tooltip);
        }
        super.appendHoverText(stack, context, display, tooltip, flag);
    }
}
