package me.Wikos.hoppersadditions.BlockEntity;

import me.Wikos.hoppersadditions.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.List;


public class PickaxeHopperBlockEntity extends ModdedHopperBlockEntity {
    public static final int MOVE_ITEM_SPEED = 16;
    private float breakProgress = 0;
    private int miningCooldown = 0;
    private static final float IRON_EFFICIENCY = 6.0f;

    private float lastHardness = -1.0f; //hardness of last hit block

    private int efficiencyLevel = 0;
    private int fortuneLevel = 0;

    public PickaxeHopperBlockEntity(BlockPos pos, BlockState state) {
        super(Registration.PICKAXE_HOPPER_ENTITY.get(), pos, state, MOVE_ITEM_SPEED);
    }

    public static void tick(Level level, BlockPos pos, BlockState state, PickaxeHopperBlockEntity blockEntity) {
        if (blockEntity.miningCooldown > 0) {
            blockEntity.miningCooldown--;
        } else {
            if (state.getValue(HopperBlock.ENABLED)) {
                blockEntity.damageBlockAbove(level, pos);
            }
            blockEntity.miningCooldown = MOVE_ITEM_SPEED;
        }

        ModdedHopperBlockEntity.pushItemsTick(level, pos, state, blockEntity);
    }

    @Override
    protected void saveAdditional(CompoundTag compoundTag, HolderLookup.Provider provider) {
        compoundTag.putFloat("BreakProgress", this.breakProgress);
        compoundTag.putInt("EfficiencyLevel", this.efficiencyLevel);
        compoundTag.putInt("FortuneLevel", this.fortuneLevel);
        super.saveAdditional(compoundTag, provider);
    }

    @Override
    protected void loadAdditional(CompoundTag compoundTag, HolderLookup.Provider provider) {
        super.loadAdditional(compoundTag, provider);
        this.breakProgress = compoundTag.getFloat("BreakProgress");
        this.efficiencyLevel = compoundTag.getInt("EfficiencyLevel");
        this.fortuneLevel = compoundTag.getInt("FortuneLevel");
    }

    public void damageBlockAbove(Level level, BlockPos pos)
    {
        BlockPos abovePos = pos.above();
        BlockState aboveState = level.getBlockState(abovePos);
        float hardness = aboveState.getDestroySpeed(level, abovePos);

        if (this.lastHardness != -1.0f && Float.compare(hardness, this.lastHardness) != 0) { //above block not same hardness has previous hit
            resetBreakProgress(level, abovePos);
        }

        this.lastHardness = hardness;

        if (aboveState.isAir() || hardness < 0) { //unbreakable block
            resetBreakProgress(level, abovePos);
            return;
        }

        float efficiencyMultiplier = IRON_EFFICIENCY;
        if (this.efficiencyLevel > 0) {
            efficiencyMultiplier += (float) (this.efficiencyLevel * this.efficiencyLevel + 1);
        }

        float breakSpeed = (efficiencyMultiplier / hardness) / 30.0f;

        if (breakSpeed <= 0) breakSpeed = 0.01f;

        this.breakProgress += breakSpeed;

        int stage = (int) (this.breakProgress * 10);
        level.destroyBlockProgress(this.hashCode(), abovePos, stage);

        level.levelEvent(2001, abovePos, Block.getId(aboveState));
        level.playSound(null, pos, aboveState.getSoundType().getHitSound(), SoundSource.BLOCKS, 0.5f, 1.0f);

        // Break the block
        if (this.breakProgress >= 1.0f) {
            if (level instanceof ServerLevel serverLevel) {
                ItemStack fakeTool = new ItemStack(Items.IRON_PICKAXE);
                if (this.fortuneLevel > 0) {
                    fakeTool.enchant(serverLevel.holderLookup(Registries.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE), this.fortuneLevel);
                }

                LootParams.Builder builder = new LootParams.Builder(serverLevel)
                        .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(abovePos))
                        .withParameter(LootContextParams.BLOCK_STATE, aboveState)
                        .withOptionalParameter(LootContextParams.BLOCK_ENTITY, serverLevel.getBlockEntity(abovePos))
                        .withOptionalParameter(LootContextParams.TOOL, fakeTool);

                List<ItemStack> drops = aboveState.getDrops(builder);
                serverLevel.destroyBlock(abovePos, false);
                for (ItemStack drop : drops) {
                    Block.popResource(serverLevel, abovePos, drop);
                }
            }
            resetBreakProgress(level, abovePos);
        }
    }

    private void resetBreakProgress(Level level, BlockPos abovePos) {
        this.breakProgress = 0.0f;
        level.destroyBlockProgress(this.hashCode(), abovePos, -1);
    }

    public void setEnchantments(int efficiency, int fortune) {
        this.efficiencyLevel = efficiency;
        this.fortuneLevel = fortune;
        this.setChanged();
    }

    @Override
    protected @NotNull net.minecraft.network.chat.Component getDefaultName() {
        return Component.translatable("container.pickaxe_hopper");
    }
}
