package com.bonker.stardew_fishing;

import com.mojang.logging.LogUtils;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import org.slf4j.Logger;

public class StardewFishing {
    public static final String MODID = Version.id;

    public static final Logger LOGGER = LogUtils.getLogger();

    public static boolean QUALITY_FOOD_INSTALLED;
    public static boolean AQUACULTURE_INSTALLED;
    public static boolean TIDE_INSTALLED;
    public static boolean BOBBER_ITEMS_REGISTERED;

    public static TagKey<Item> STARTS_MINIGAME;

    public static final ResourceLocation TREASURE_CHEST_LOOT = mkResLoc("treasure_chest");
    public static final ResourceLocation TREASURE_CHEST_NETHER_LOOT = mkResLoc("treasure_chest_nether");

    public static String MOD_NAME;

    public static Platform platform;

    public static ResourceLocation mkResLoc(String name)
    {
        return new ResourceLocation(MODID, name);
    }
}
