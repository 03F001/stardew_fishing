package com.bonker.stardew_fishing.fabric;

import com.bonker.stardew_fishing.StardewFishing;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;

public class Entry implements ModInitializer {
    @Override
    public void onInitialize() {
        StardewFishing.QUALITY_FOOD_INSTALLED = false;
        StardewFishing.AQUACULTURE_INSTALLED = false;
        StardewFishing.TIDE_INSTALLED = FabricLoader.getInstance().isModLoaded("tide");
        StardewFishing.BOBBER_ITEMS_REGISTERED = StardewFishing.TIDE_INSTALLED;

        StardewFishing.STARTS_MINIGAME = TagKey.create(BuiltInRegistries.ITEM.key(), StardewFishing.mkResLoc("starts_minigame"));

        FabricLoader.getInstance().getModContainer(StardewFishing.MODID).ifPresent(mod -> {
            StardewFishing.MOD_NAME = mod.getMetadata().getName() + " " + mod.getMetadata().getVersion().getFriendlyString();
        });

        StardewFishing.platform = new FabricPlatform();

        Recipes.register();
        if (StardewFishing.TIDE_INSTALLED) {
            Items.register();
        }
    }
}