package com.bonker.stardewfishing.compat;

import com.li64.tide.data.rods.CustomRodManager;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class Tide {
    public static final ResourceLocation DEFAULT_BOBBER_TEXTURE = new ResourceLocation("tide", "textures/item/white_fishing_bobber.png");

    public static ItemStack getBobber(ItemStack fishingRod) {
        return CustomRodManager.getBobber(fishingRod);
    }
}
