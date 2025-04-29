package com.bonker.stardewfishing.proxy;

import de.cadentem.quality_food.core.Quality;
import de.cadentem.quality_food.util.QualityUtils;
import net.minecraft.world.item.ItemStack;

public class QualityFoodProxy {
    public static void applyQuality(ItemStack stack, int quality) {
        QualityUtils.applyQuality(stack, Quality.get(quality));
    }
}
