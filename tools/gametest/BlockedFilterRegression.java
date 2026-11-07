import java.lang.reflect.Method;
import me.Wikos.hoppersadditions.BlockEntity.Hoppers.FilterHopperBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
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
        ItemStack original = new ItemStack(Items.STONE, 3);
        original.set(DataComponents.CUSTOM_NAME, Component.literal("blocked source"));
        source.setItem(0, original.copy());
        source.setItem(4, original.copyWithCount(1));
        Method eject = FilterHopperBlockEntity.class.getDeclaredMethod("ejectFiltered", Level.class, BlockPos.class, FilterHopperBlockEntity.class);
        eject.setAccessible(true);
        for (int attempt = 0; attempt < 3; attempt++) {
            assertBlocked(source, target, original, (boolean) eject.invoke(null, level, BlockPos.ZERO, source), 3);
        }
        source.setItem(0, original.copyWithCount(1));
        assertBlocked(source, target, original, (boolean) eject.invoke(null, level, BlockPos.ZERO, source), 1);
        System.out.println("PASS: repeated blocked Filter rollback preserves counts, components and prototype (isolated regression only).");
    }

    private static void assertBlocked(FilterHopperBlockEntity source, Container target, ItemStack original, boolean moved, int expected) {
        int actual = count(source) + count(target);
        if (moved || actual != expected + 1 || source.getItem(0).getCount() != expected
                || source.getItem(4).getCount() != 1 || count(target) != 0
                || !ItemStack.isSameItemSameComponents(source.getItem(0), original)
                || !ItemStack.isSameItemSameComponents(source.getItem(4), original)) {
            throw new AssertionError("Blocked Filter conservation: expected source=" + expected + " + prototype=1, target=0, moved=false; actual source="
                    + source.getItem(0).getCount() + ", prototype=" + source.getItem(4).getCount()
                    + ", target=" + count(target) + ", moved=" + moved
                    + ". Production method, fake level/real containers; NOT an in-world GameTest.");
        }
    }

    private static int count(Container inventory) {
        int amount = 0;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) { amount += inventory.getItem(slot).getCount(); }
        return amount;
    }
}
