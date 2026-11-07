import java.lang.reflect.Method;
import me.Wikos.hoppersadditions.BlockEntity.Hoppers.FilterHopperBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;

public final class BlockedFilterRegression {
    private BlockedFilterRegression() {}

    public static void main(String[] arguments) throws Exception {
        HopperMigrationRegression.main(new String[0]);
        Method createLevel = HopperMigrationRegression.class.getDeclaredMethod("level", BarrelBlockEntity.class, BarrelBlockEntity.class);
        createLevel.setAccessible(true);
        BarrelBlockEntity target = new HopperMigrationRegression.RejectingBarrel();
        HopperMigrationRegression.RoutingLevel level = (HopperMigrationRegression.RoutingLevel) createLevel.invoke(null, target, null);
        Method createHopper = HopperMigrationRegression.class.getDeclaredMethod("hopper", Class.class, HopperMigrationRegression.RoutingLevel.class, int.class);
        createHopper.setAccessible(true);
        FilterHopperBlockEntity source = (FilterHopperBlockEntity) createHopper.invoke(null, HopperMigrationRegression.Filter.class, level, 3);
        source.setItem(4, new ItemStack(Items.STONE));
        Method eject = FilterHopperBlockEntity.class.getDeclaredMethod("ejectFiltered", Level.class, BlockPos.class, FilterHopperBlockEntity.class);
        eject.setAccessible(true);
        boolean moved = (boolean) eject.invoke(null, level, BlockPos.ZERO, source);
        int actual = count(source) + count(target);
        if (moved || actual != 4 || source.getItem(0).getCount() != 3) {
            throw new AssertionError("Blocked Filter conservation: expected source=3 + prototype=1, target=0, moved=false; actual source="
                    + source.getItem(0).getCount() + ", prototype=" + source.getItem(4).getCount()
                    + ", target=" + count(target) + ", moved=" + moved
                    + ". Production method, fake level/real containers; NOT an in-world GameTest.");
        }
        System.out.println("PASS: blocked Filter rollback conserves all four items (isolated regression only).");
    }

    private static int count(Container inventory) {
        int amount = 0;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) { amount += inventory.getItem(slot).getCount(); }
        return amount;
    }
}
