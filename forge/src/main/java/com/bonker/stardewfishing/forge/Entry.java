package com.bonker.stardewfishing.forge;

import com.bonker.stardewfishing.StardewFishing;

import net.minecraft.tags.TagKey;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.forgespi.language.IModInfo;
import net.minecraftforge.registries.ForgeRegistries;

@Mod(StardewFishing.MODID)
public class Entry {
    public Entry() {
        StardewFishing.QUALITY_FOOD_INSTALLED = ModList.get().isLoaded("quality_food");
        StardewFishing.AQUACULTURE_INSTALLED = ModList.get().isLoaded("aquaculture");
        StardewFishing.TIDE_INSTALLED = ModList.get().isLoaded("tide");
        StardewFishing.BOBBER_ITEMS_REGISTERED = StardewFishing.AQUACULTURE_INSTALLED || StardewFishing.TIDE_INSTALLED;

        StardewFishing.STARTS_MINIGAME = TagKey.create(ForgeRegistries.ITEMS.getRegistryKey(), StardewFishing.mkResLoc("starts_minigame"));

        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();

        IModInfo info = ModLoadingContext.get().getActiveContainer().getModInfo();
        StardewFishing.MOD_NAME = info.getDisplayName() + " " + info.getVersion();

        StardewFishing.platform = new ForgePlatform(bus);

        if (StardewFishing.BOBBER_ITEMS_REGISTERED) {
            Items.ITEMS.register(bus);
            Items.CREATIVE_MODE_TABS.register(bus);
        }

        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, Config.SERVER_SPEC);
    }
}
