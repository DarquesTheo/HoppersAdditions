package me.Wikos.hoppersadditions.BlockEntity.Hoppers;

import me.Wikos.hoppersadditions.Registration;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class MobHopperBlockEntity extends BlockEntity {

    private final boolean isMonsterVariant;
    private static final AABB SUCK_AREA = new AABB(0, 1, 0, 1, 2, 1);

    public MobHopperBlockEntity(BlockPos pos, BlockState state, boolean isMonsterVariant) {
        super(isMonsterVariant ? Registration.MONSTER_HOPPER_ENTITY.get() : Registration.ANIMAL_HOPPER_ENTITY.get(), pos, state);
        this.isMonsterVariant = isMonsterVariant;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MobHopperBlockEntity blockEntity) {
        if (level.isClientSide) return;

        if (state.getValue(HopperBlock.ENABLED)) {
            blockEntity.moveMobs(level, pos, state);
        }
    }

    private void moveMobs(Level level, BlockPos pos, BlockState state) {
        AABB absoluteArea = SUCK_AREA.move(pos);
        //find and filter entities
        List<Entity> entities = level.getEntities((Entity)null, absoluteArea, entity -> {
            if (!(entity instanceof LivingEntity)) return false;
            if (this.isMonsterVariant) return entity instanceof Monster;
            return entity instanceof Animal;
        });

        if (!entities.isEmpty()) {
            Direction facing = state.getValue(HopperBlock.FACING);
            BlockPos targetPos = pos.relative(facing);

            for (Entity entity : entities) {
                double tx = targetPos.getX() + 0.5;
                double ty = targetPos.getY();
                double tz = targetPos.getZ() + 0.5;

                if (entity instanceof LivingEntity living) {
                    living.teleportTo(tx, ty, tz);

                    living.setDeltaMovement(Vec3.ZERO);
                    living.hurtMarked = true;
                }
            }
        }
    }
}
