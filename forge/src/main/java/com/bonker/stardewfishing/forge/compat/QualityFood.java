package com.bonker.stardewfishing.forge.compat;

import de.cadentem.quality_food.core.Quality;
import de.cadentem.quality_food.util.QualityUtils;

import net.minecraft.world.item.ItemStack;

public class QualityFood {
    public static void applyQuality(ItemStack stack, int quality) {
        QualityUtils.applyQuality(stack, Quality.get(quality));
    }
}