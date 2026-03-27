package me.Wikos.hoppersadditions.fabric;

import me.Wikos.hoppersadditions.Hoppersadditions;
import net.fabricmc.api.ModInitializer;

public final class HoppersadditionsFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        // However, some things (like resources) may still be uninitialized.
        // Proceed with mild caution.

        // Run our common setup.
        Hoppersadditions.init();
    }
}
