package me.Wikos.hoppersadditions.Item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import java.util.List;

public interface HopperTooltip {
    default void appendHopperTooltip(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
    }
}
