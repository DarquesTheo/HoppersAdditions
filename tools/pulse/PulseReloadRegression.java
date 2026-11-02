import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import me.Wikos.hoppersadditions.BlockEntity.Hoppers.PulseHopperBlockEntity;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.Bootstrap;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.LockCode;
import net.minecraft.world.TickRateManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkSource;
import net.minecraft.world.level.entity.LevelEntityGetter;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.ticks.LevelTickAccess;
import sun.misc.Unsafe;

public class PulseReloadRegression {
    private static Unsafe allocator;
    private static HolderLookup.Provider registries;
    private static Method load;
    private static Method save;
    private static Method canCollect;
    private static Method finishCollection;
    private static int checks;

    public static class TestHopper extends PulseHopperBlockEntity {
        public TestHopper() { super(BlockPos.ZERO, Blocks.HOPPER.defaultBlockState()); }
        @Override public void setChanged() { }
    }

    public static class TestLevel extends Level {
        private long time;
        private boolean powered;
        private BarrelBlockEntity target;

        private TestLevel() { super(null, null, null, null, null, false, false, 0L, 0); }
        @Override public long getGameTime() { return this.time; }
        @Override public boolean hasNeighborSignal(BlockPos pos) { return this.powered; }
        @Override public BlockState getBlockState(BlockPos pos) { return Blocks.BARREL.defaultBlockState(); }
        @Override public BlockEntity getBlockEntity(BlockPos pos) { return this.target; }
        @Override public void sendBlockUpdated(BlockPos pos, BlockState before, BlockState after, int flags) { throw new UnsupportedOperationException(); }
        @Override public void playSeededSound(Player player, double x, double y, double z, Holder<SoundEvent> sound, SoundSource source, float volume, float pitch, long seed) { throw new UnsupportedOperationException(); }
        @Override public void playSeededSound(Player player, Entity entity, Holder<SoundEvent> sound, SoundSource source, float volume, float pitch, long seed) { throw new UnsupportedOperationException(); }
        @Override public String gatherChunkSourceStats() { throw new UnsupportedOperationException(); }
        @Override public Entity getEntity(int id) { throw new UnsupportedOperationException(); }
        @Override public TickRateManager tickRateManager() { throw new UnsupportedOperationException(); }
        @Override public MapItemSavedData getMapData(MapId id) { throw new UnsupportedOperationException(); }
        @Override public void setMapData(MapId id, MapItemSavedData data) { throw new UnsupportedOperationException(); }
        @Override public MapId getFreeMapId() { throw new UnsupportedOperationException(); }
        @Override public void destroyBlockProgress(int id, BlockPos pos, int progress) { throw new UnsupportedOperationException(); }
        @Override public Scoreboard getScoreboard() { throw new UnsupportedOperationException(); }
        @Override public RecipeManager getRecipeManager() { throw new UnsupportedOperationException(); }
        @Override protected LevelEntityGetter<Entity> getEntities() { throw new UnsupportedOperationException(); }
        @Override public PotionBrewing potionBrewing() { throw new UnsupportedOperationException(); }
        @Override public LevelTickAccess<Block> getBlockTicks() { throw new UnsupportedOperationException(); }
        @Override public LevelTickAccess<net.minecraft.world.level.material.Fluid> getFluidTicks() { throw new UnsupportedOperationException(); }
        @Override public ChunkSource getChunkSource() { throw new UnsupportedOperationException(); }
        @Override public int getSeaLevel() { throw new UnsupportedOperationException(); }
        @Override public List<? extends Player> players() { throw new UnsupportedOperationException(); }
        @Override public Holder<net.minecraft.world.level.biome.Biome> getUncachedNoiseBiome(int x, int y, int z) { throw new UnsupportedOperationException(); }
        @Override public net.minecraft.world.flag.FeatureFlagSet enabledFeatures() { throw new UnsupportedOperationException(); }
        @Override public long nextSubTickCount() { throw new UnsupportedOperationException(); }
        @Override public float getShade(Direction direction, boolean shade) { throw new UnsupportedOperationException(); }
        @Override public void gameEvent(Holder<net.minecraft.world.level.gameevent.GameEvent> event, net.minecraft.world.phys.Vec3 position, net.minecraft.world.level.gameevent.GameEvent.Context context) { throw new UnsupportedOperationException(); }
        @Override public void levelEvent(Player player, int event, BlockPos pos, int data) { throw new UnsupportedOperationException(); }
    }

    private static void setField(Class<?> owner, Object target, String name, Object value) throws Exception {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }

    private static Object field(PulseHopperBlockEntity hopper, String name) throws Exception {
        Field field = PulseHopperBlockEntity.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(hopper);
    }

    private static TestLevel level(boolean powered) throws Exception {
        TestLevel level = (TestLevel) allocator.allocateInstance(TestLevel.class);
        level.time = 100;
        level.powered = powered;
        level.target = new BarrelBlockEntity(BlockPos.ZERO.below(), Blocks.BARREL.defaultBlockState());
        return level;
    }

    private static TestHopper hopper(TestLevel level, int count) throws Exception {
        TestHopper hopper = (TestHopper) allocator.allocateInstance(TestHopper.class);
        setField(BlockEntity.class, hopper, "worldPosition", BlockPos.ZERO);
        setField(BlockEntity.class, hopper, "blockState", Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
        setField(BaseContainerBlockEntity.class, hopper, "lockKey", LockCode.NO_LOCK);
        NonNullList<ItemStack> inventory = NonNullList.withSize(5, ItemStack.EMPTY);
        inventory.set(0, count == 0 ? ItemStack.EMPTY : new ItemStack(Items.STONE, count));
        setField(PulseHopperBlockEntity.class, hopper, "items", inventory);
        setField(PulseHopperBlockEntity.class, hopper, "lastPulseGameTime", Long.MIN_VALUE);
        setField(PulseHopperBlockEntity.class, hopper, "nextInputGameTime", Long.MIN_VALUE);
        hopper.setLevel(level);
        return hopper;
    }

    private static CompoundTag saved(boolean powered, int count) throws Exception {
        TestHopper hopper = hopper(level(powered), count);
        hopper.initializePower(powered);
        CompoundTag tag = new CompoundTag();
        save.invoke(hopper, tag, registries);
        return tag;
    }

    private static TestHopper reload(TestLevel level, CompoundTag tag) throws Exception {
        TestHopper restored = hopper(level, 0);
        load.invoke(restored, tag, registries);
        return restored;
    }

    private static boolean observe(TestHopper hopper) throws Exception { return (boolean) canCollect.invoke(hopper); }

    private static void edge(TestLevel level, TestHopper hopper, boolean powered) {
        level.powered = powered;
        hopper.powerChanged(powered);
    }

    private static void check(boolean condition, String name) {
        checks++;
        if (!condition) throw new AssertionError(name);
    }

    private static int count(BarrelBlockEntity container) {
        int total = 0;
        for (int slot = 0; slot < container.getContainerSize(); slot++) total += container.getItem(slot).getCount();
        return total;
    }

    private static void reloadScenario(boolean savedPower, boolean actualPower, boolean callbackFirst) throws Exception {
        TestLevel level = level(actualPower);
        TestHopper hopper = reload(level, saved(savedPower, 8));
        if (callbackFirst) hopper.powerChanged(actualPower);
        observe(hopper);
        check(count(level.target) == 0 && hopper.getItem(0).getCount() == 8, "no phantom output on first observation");
        check(field(hopper, "lastPowered").equals(actualPower), "first observation resynchronizes saved=" + savedPower + " actual=" + actualPower);
        level.time++;
        hopper.powerChanged(actualPower);
        check(count(level.target) == 0, "delayed unchanged-power callback has no phantom output");
        if (actualPower) edge(level, hopper, false);
        level.time++;
        edge(level, hopper, true);
        check(count(level.target) == 1 && hopper.getItem(0).getCount() == 7, "next real rising edge delivers exactly one");
        hopper.powerChanged(true);
        observe(hopper);
        check(count(level.target) == 1, "constant power has no delayed pulse");
    }

    public static void main(String[] args) throws Exception {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        Field unsafe = Unsafe.class.getDeclaredField("theUnsafe");
        unsafe.setAccessible(true);
        allocator = (Unsafe) unsafe.get(null);
        registries = RegistryAccess.fromRegistryOfRegistries(BuiltInRegistries.REGISTRY);
        load = PulseHopperBlockEntity.class.getDeclaredMethod("loadAdditional", CompoundTag.class, HolderLookup.Provider.class);
        save = PulseHopperBlockEntity.class.getDeclaredMethod("saveAdditional", CompoundTag.class, HolderLookup.Provider.class);
        canCollect = PulseHopperBlockEntity.class.getDeclaredMethod("canCollect");
        finishCollection = PulseHopperBlockEntity.class.getDeclaredMethod("finishCollection", boolean.class);
        for (Method method : new Method[] {load, save, canCollect, finishCollection}) method.setAccessible(true);

        if (args.length == 1 && args[0].equals("--expect-historical-failure")) {
            TestLevel level = level(false);
            TestHopper hopper = reload(level, saved(true, 8));
            observe(hopper);
            check(field(hopper, "lastPowered").equals(true), "historical stale powered latch reproduced");
            level.time++;
            edge(level, hopper, true);
            check(count(level.target) == 0 && hopper.getItem(0).getCount() == 8, "historical next real edge lost");
            TestLevel powered = level(true);
            TestHopper staleUnpowered = reload(powered, saved(false, 8));
            staleUnpowered.powerChanged(true);
            check(count(powered.target) == 1, "historical load callback phantom reproduced");
            System.out.println("REPRODUCED: saved-powered -> loaded-unpowered -> next rising edge delivers 0 instead of 1; saved-unpowered -> powered first callback delivers a phantom item.");
            return;
        }

        for (boolean savedPower : new boolean[] {false, true}) {
            for (boolean actualPower : new boolean[] {false, true}) {
                for (boolean callbackFirst : new boolean[] {false, true}) reloadScenario(savedPower, actualPower, callbackFirst);
            }
        }
        TestLevel level = level(true);
        TestHopper hopper = hopper(level, 8);
        hopper.initializePower(true);
        hopper.powerChanged(true);
        observe(hopper);
        check(count(level.target) == 0, "powered placement has no output");
        for (int cycle = 0; cycle < 3; cycle++) {
            CompoundTag tag = new CompoundTag();
            save.invoke(hopper, tag, registries);
            hopper = reload(level, tag);
            hopper.powerChanged(true);
            observe(hopper);
            level.time++;
            hopper.powerChanged(true);
        }
        check(count(level.target) == 0 && hopper.getItem(0).getCount() == 8, "repeated powered NBT reloads conserve inventory");
        level.powered = false;
        load.invoke(hopper, saved(true, 8), registries);
        observe(hopper);
        level.time++;
        edge(level, hopper, true);
        check(count(level.target) == 1, "loading into an already initialized entity also resets baseline");

        for (String missing : new String[] {"PulsePowerInitialized", "PulseLastPowered", "all"}) {
            CompoundTag tag = saved(true, 8);
            if (missing.equals("all")) {
                for (String key : new String[] {"PulsePowerInitialized", "PulseLastPowered", "PulseLastGameTime", "PulseNextInputGameTime"}) tag.remove(key);
            } else tag.remove(missing);
            level = level(true);
            hopper = reload(level, tag);
            hopper.powerChanged(true);
            check(count(level.target) == 0, "missing NBT baseline: " + missing);
            if (missing.equals("all")) {
                check(field(hopper, "lastPulseGameTime").equals(Long.MIN_VALUE), "missing pulse stamp default");
                check(field(hopper, "nextInputGameTime").equals(Long.MIN_VALUE), "missing input deadline default");
            }
            edge(level, hopper, false);
            level.time++;
            edge(level, hopper, true);
            check(count(level.target) == 1, "missing NBT permits next genuine edge");
        }

        level = level(false);
        hopper = hopper(level, 8);
        observe(hopper);
        edge(level, hopper, true);
        edge(level, hopper, true);
        edge(level, hopper, false);
        edge(level, hopper, true);
        check(count(level.target) == 1 && hopper.getItem(0).getCount() == 7, "duplicates and multiple same-tick edges deliver once");
        CompoundTag sameTick = new CompoundTag();
        save.invoke(hopper, sameTick, registries);
        hopper = reload(level, sameTick);
        observe(hopper);
        edge(level, hopper, false);
        edge(level, hopper, true);
        check(count(level.target) == 1, "same-tick attempt stamp survives reload");
        level.time++;
        edge(level, hopper, false);
        edge(level, hopper, true);
        check(count(level.target) == 2 && hopper.getItem(0).getCount() == 6, "later tick edge delivers");

        for (boolean blocked : new boolean[] {false, true}) {
            level = level(false);
            hopper = hopper(level, blocked ? 8 : 0);
            if (blocked) {
                for (int slot = 0; slot < level.target.getContainerSize(); slot++) level.target.setItem(slot, new ItemStack(Items.DIRT, 64));
            }
            observe(hopper);
            edge(level, hopper, true);
            check(field(hopper, "lastPulseGameTime").equals(level.time), "failed pulse consumed");
            check(hopper.getItem(0).getCount() == (blocked ? 8 : 0), "failure preserves inventory");
            check(count(level.target) == (blocked ? 27 * 64 : 0), "failure preserves destination inventory");
            level.target.clearContent();
            if (!blocked) hopper.setItem(0, new ItemStack(Items.STONE, 8));
            edge(level, hopper, false);
            edge(level, hopper, true);
            check(count(level.target) == 0, "failed same-tick pulse cannot retry");
            level.time++;
            observe(hopper);
            hopper.powerChanged(true);
            check(count(level.target) == 0, "restored capacity/stock does not queue output");
            edge(level, hopper, false);
            edge(level, hopper, true);
            check(count(level.target) == 1 && hopper.getItem(0).getCount() == 7, "next edge after failure conserves all items");
        }

        level = level(false);
        hopper = hopper(level, 8);
        observe(hopper);
        finishCollection.invoke(hopper, true);
        CompoundTag cooldown = new CompoundTag();
        save.invoke(hopper, cooldown, registries);
        hopper = reload(level, cooldown);
        check(!observe(hopper), "input cooldown survives reload");
        check(field(hopper, "lastPowered").equals(false), "baseline established even during input cooldown");
        edge(level, hopper, true);
        check(count(level.target) == 1, "output ignores input cooldown");
        edge(level, hopper, false);
        level.time += 7;
        check(!observe(hopper), "collection blocked before eight ticks");
        level.time++;
        check(observe(hopper), "collection eligible at eight ticks");
        System.out.println("PASS: " + checks + " assertions against production pulse/NBT/transfer methods and real Minecraft containers; fake level, no loader or gameplay session.");
    }
}
