package me.Wikos.hoppersadditions.Item;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

public class PickaxeHopperItem extends BlockItem {
    public PickaxeHopperItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public boolean isEnchantable(ItemStack stack) {
        return true;
    }

    @Override
    public int getEnchantmentValue() {
        return 10;
    }
}
