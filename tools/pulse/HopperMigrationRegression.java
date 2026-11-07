import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import me.Wikos.hoppersadditions.BlockEntity.ModdedHopperBlockEntity;
import me.Wikos.hoppersadditions.BlockEntity.Hoppers.*;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.Bootstrap;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.Container;
import net.minecraft.world.LockCode;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import sun.misc.Unsafe;

public class HopperMigrationRegression {
    private static Unsafe allocator;
    private static RegistryAccess registries;
    private static int checks;
    private static final BlockPos FRONT = BlockPos.ZERO.north();
    private static final BlockPos DOWN = BlockPos.ZERO.below();
    private static BlockState state;

    public static class RoutingLevel extends PulseReloadRegression.TestLevel {
        private Map<BlockPos, BlockEntity> targets;
        @Override public java.util.List<net.minecraft.world.entity.Entity> getEntities(net.minecraft.world.entity.Entity except, net.minecraft.world.phys.AABB area, java.util.function.Predicate<? super net.minecraft.world.entity.Entity> predicate) { return java.util.List.of(); }
        @Override public boolean hasChunkAt(BlockPos pos) { return true; }
        @Override public BlockState getBlockState(BlockPos pos) { return targets.containsKey(pos) ? Blocks.BARREL.defaultBlockState() : Blocks.AIR.defaultBlockState(); }
        @Override public BlockEntity getBlockEntity(BlockPos pos) { return targets.get(pos); }
    }

    public static class Splitting extends SplittingHopperBlockEntity {
        public Splitting() { super(BlockPos.ZERO, Blocks.HOPPER.defaultBlockState()); }
        @Override public void setChanged() { }
    }
    public static class Overflow extends OverflowHopperBlockEntity {
        public Overflow() { super(BlockPos.ZERO, Blocks.HOPPER.defaultBlockState()); }
        @Override public void setChanged() { }
    }
    public static class Pickaxe extends PickaxeHopperBlockEntity {
        public Pickaxe() { super(BlockPos.ZERO, Blocks.HOPPER.defaultBlockState()); }
        @Override public void setChanged() { }
    }
    public static class Filter extends FilterHopperBlockEntity {
        public Filter() { super(BlockPos.ZERO, Blocks.HOPPER.defaultBlockState()); }
        @Override public void setChanged() { }
    }
    public static class RejectingBarrel extends BarrelBlockEntity implements WorldlyContainer {
        public RejectingBarrel() { super(BlockPos.ZERO, Blocks.BARREL.defaultBlockState()); }
        @Override public int[] getSlotsForFace(Direction face) { return new int[] {0}; }
        @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction face) { return false; }
        @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction face) { return false; }
    }

    private static void set(Class<?> owner, Object target, String name, Object value) throws Exception {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
    private static Method method(Class<?> owner, String name, Class<?>... parameters) throws Exception {
        Method method = owner.getDeclaredMethod(name, parameters);
        method.setAccessible(true);
        return method;
    }
    private static void check(boolean condition, String label) {
        checks++;
        if (!condition) throw new AssertionError(label);
    }
    private static RoutingLevel level(BarrelBlockEntity front, BarrelBlockEntity down) throws Exception {
        RoutingLevel level = (RoutingLevel) allocator.allocateInstance(RoutingLevel.class);
        level.targets = new java.util.HashMap<>();
        if (front != null) level.targets.put(FRONT, front);
        if (down != null) level.targets.put(DOWN, down);
        return level;
    }
    private static <T extends ModdedHopperBlockEntity> T hopper(Class<T> type, RoutingLevel level, int amount) throws Exception {
        T hopper = (T) allocator.allocateInstance(type);
        set(BlockEntity.class, hopper, "worldPosition", BlockPos.ZERO);
        set(BlockEntity.class, hopper, "blockState", state);
        set(BaseContainerBlockEntity.class, hopper, "lockKey", LockCode.NO_LOCK);
        NonNullList<ItemStack> inventory = NonNullList.withSize(5, ItemStack.EMPTY);
        inventory.set(0, amount == 0 ? ItemStack.EMPTY : new ItemStack(Items.STONE, amount));
        set(ModdedHopperBlockEntity.class, hopper, "items", inventory);
        hopper.facing = Direction.NORTH;
        hopper.itemMoveSpeed = 8;
        hopper.setLevel(level);
        return hopper;
    }
    private static BarrelBlockEntity barrel(boolean full) {
        BarrelBlockEntity barrel = new BarrelBlockEntity(BlockPos.ZERO, Blocks.BARREL.defaultBlockState());
        if (full) for (int slot = 0; slot < barrel.getContainerSize(); slot++) barrel.setItem(slot, new ItemStack(Items.DIRT, 64));
        return barrel;
    }
    private static int count(Container container) {
        int count = 0;
        if (container != null) for (int slot = 0; slot < container.getContainerSize(); slot++) count += container.getItem(slot).getCount();
        return count;
    }
    private static CompoundTag save(ModdedHopperBlockEntity hopper, Class<?> owner) throws Exception {
        TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
        method(owner, "saveAdditional", ValueOutput.class).invoke(hopper, output);
        return output.buildResult();
    }
    private static void load(ModdedHopperBlockEntity hopper, Class<?> owner, CompoundTag tag) throws Exception {
        method(owner, "loadAdditional", ValueInput.class).invoke(hopper, TagValueInput.create(ProblemReporter.DISCARDING, registries, tag));
    }

    public static void main(String[] args) throws Exception {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        Field unsafe = Unsafe.class.getDeclaredField("theUnsafe");
        unsafe.setAccessible(true);
        allocator = (Unsafe) unsafe.get(null);
        registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        state = Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.NORTH);
        Method split = method(SplittingHopperBlockEntity.class, "output", Level.class, BlockPos.class, BlockState.class);
        Method overflow = method(OverflowHopperBlockEntity.class, "pushOneItem", Container.class, Container.class, Direction.class);
        for (int scenario = 0; scenario < 4; scenario++) {
            BarrelBlockEntity front = scenario == 0 ? null : scenario == 3 ? new RejectingBarrel() : barrel(scenario == 1);
            BarrelBlockEntity down = barrel(false);
            RoutingLevel level = level(front, down);
            Splitting hopper = hopper(Splitting.class, level, 8);
            int originalFront = count(front);
            boolean moved = (boolean) split.invoke(hopper, level, BlockPos.ZERO, state);
            boolean open = scenario == 2;
            check(moved == open, "splitting only successful scheduled front advances");
            check(count(down) == 0, "splitting cannot bypass blocked front");
            check(count(front) == originalFront + (open ? 1 : 0), "splitting front conservation");
            check(count(hopper) == (open ? 7 : 8), "splitting source conservation");
            CompoundTag tag = save(hopper, SplittingHopperBlockEntity.class);
            check(tag.getBooleanOr("NextDown", false) == open, "saved strict branch reflects only successful output");
            if (open) {
                Splitting restored = hopper(Splitting.class, level, 0);
                load(restored, SplittingHopperBlockEntity.class, tag);
                level.targets.remove(DOWN);
                check(!(boolean) split.invoke(restored, level, BlockPos.ZERO, state), "restored next-down waits if absent");
                check(count(front) == 1 && count(restored) == 7, "blocked down does not use available front");
                level.targets.put(DOWN, down);
                check((boolean) split.invoke(restored, level, BlockPos.ZERO, state), "restored down succeeds once available");
                check(count(down) == 1 && count(front) == 1 && count(restored) == 6, "strict front/down alternation conserves items");
                check(!save(restored, SplittingHopperBlockEntity.class).getBooleanOr("NextDown", true), "second success schedules front");
            }
            Overflow fallback = hopper(Overflow.class, level, 8);
            int beforeFront = count(front);
            int beforeDown = count(down);
            check((boolean) overflow.invoke(fallback, front, down, Direction.NORTH), "overflow finds front or fallback");
            check(count(front) == beforeFront + (open ? 1 : 0), "overflow always prefers acceptable front");
            check(count(down) == beforeDown + (open ? 0 : 1), "overflow uses down only if front fails");
            check(count(fallback) == 7, "overflow outputs exactly one item");
        }
        RoutingLevel level = level(barrel(true), barrel(true));
        Overflow blocked = hopper(Overflow.class, level, 1);
        check(!(boolean) overflow.invoke(blocked, level.targets.get(FRONT), level.targets.get(DOWN), Direction.NORTH), "both outputs full wait");
        check(count(blocked) == 1, "failed final item restored");
        check(!(boolean) overflow.invoke(blocked, null, null, Direction.NORTH), "both outputs missing wait");
        check(count(blocked) == 1, "missing destinations conserve final item");
        Filter filter = hopper(Filter.class, level, 0);
        ItemStack componentStack = new ItemStack(Items.STONE, 13);
        componentStack.set(DataComponents.CUSTOM_NAME, Component.literal("legacy contents"));
        componentStack.set(DataComponents.MAX_STACK_SIZE, 16);
        filter.setItem(0, componentStack.copy());
        filter.setItem(4, componentStack.copyWithCount(1));
        filter.cooldownTime = 7;
        CompoundTag inventory = save(filter, ModdedHopperBlockEntity.class);
        check(inventory.contains("Items") && inventory.getIntOr("TransferCooldown", -1) == 7, "legacy inventory/cooldown keys retained");
        Filter restoredFilter = hopper(Filter.class, level, 0);
        load(restoredFilter, ModdedHopperBlockEntity.class, inventory);
        check(ItemStack.isSameItemSameComponents(restoredFilter.getItem(0), componentStack) && restoredFilter.getItem(0).getCount() == 13, "inventory count/name/stack-limit components round trip");
        check(restoredFilter.cooldownTime == 7 && restoredFilter.getItem(0).getMaxStackSize() == 16, "cooldown and stack limit round trip");
        check(restoredFilter.canPlaceItem(1, componentStack) && !restoredFilter.canPlaceItem(1, new ItemStack(Items.STONE)), "filter uses exact components after reload");
        check(!restoredFilter.canPlaceItem(4, componentStack), "automation cannot fill filter prototype slot");
        Pickaxe pickaxe = hopper(Pickaxe.class, level, 0);
        CompoundTag legacyState = new CompoundTag();
        legacyState.putFloat("BreakProgress", 0.625F);
        legacyState.putInt("EfficiencyLevel", 3);
        legacyState.putInt("FortuneLevel", 2);
        load(pickaxe, PickaxeHopperBlockEntity.class, legacyState);
        CompoundTag savedPickaxe = save(pickaxe, PickaxeHopperBlockEntity.class);
        check(savedPickaxe.getFloatOr("BreakProgress", -1) == 0.625F, "legacy pickaxe progress retained");
        check(savedPickaxe.getIntOr("EfficiencyLevel", -1) == 3 && savedPickaxe.getIntOr("FortuneLevel", -1) == 2, "legacy pickaxe enchantment levels retained");
        Splitting empty = hopper(Splitting.class, level, 0);
        load(empty, SplittingHopperBlockEntity.class, new CompoundTag());
        check(!save(empty, SplittingHopperBlockEntity.class).getBooleanOr("NextDown", true), "missing legacy branch defaults front");
        System.out.println("PASS: " + checks + " routing, conservation, component and saved-state assertions; production methods with fake level/real containers, not gameplay or a world upgrade.");
    }
}
