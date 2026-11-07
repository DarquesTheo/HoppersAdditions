package me.Wikos.hoppersadditions.gametest;

import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.RegisterEvent;

@Mod("hoppersadditions_gametest")
public final class NeoForgeTests {
    public NeoForgeTests(IEventBus bus) {
        bus.addListener(this::register);
    }

    private void register(RegisterEvent event) {
        event.register(Registries.TEST_FUNCTION, registry ->
                HopperGameTests.scenarios().forEach((name, test) -> registry.register(HopperGameTests.id(name), test)));
    }
}
