package me.Wikos.hoppersadditions.gametest;

import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;

public final class FabricTests implements ModInitializer {
    @Override
    public void onInitialize() {
        HopperGameTests.scenarios().forEach((name, test) ->
                Registry.register(BuiltInRegistries.TEST_FUNCTION, HopperGameTests.id(name), test));
    }
}
