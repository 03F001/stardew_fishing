package com.bonker.stardewfishing.fabric;

import com.bonker.stardewfishing.StardewFishing;

import me.shedaniel.autoconfig.ConfigData;
import me.shedaniel.autoconfig.annotation.ConfigEntry;

@me.shedaniel.autoconfig.annotation.Config(name = StardewFishing.MODID)
public class Config implements ConfigData {
    @ConfigEntry.BoundedDiscrete(min=0, max=1)
    public double QUALITY_1_THRESHOLD = 0.75;

    @ConfigEntry.BoundedDiscrete(min=0, max=1)
    public double QUALITY_2_THRESHOLD = 0.9;

    @ConfigEntry.BoundedDiscrete(min=0, max=1)
    public double QUALITY_3_THRESHOLD = 1.0;

    @ConfigEntry.BoundedDiscrete(min=0, max=10)
    public double QUALITY_1_MULTIPLIER = 1.5;

    @ConfigEntry.BoundedDiscrete(min=0, max=10)
    public double QUALITY_2_MULTIPLIER = 2.5;

    @ConfigEntry.BoundedDiscrete(min=0, max=10)
    public double QUALITY_3_MULTIPLIER = 4.0;

    @ConfigEntry.BoundedDiscrete(min=0, max=10)
    public double QUALITY_BOBBER_MULTIPLIER = 1.4;

    @ConfigEntry.BoundedDiscrete(min=0, max=1)
    public double BITE_TIME_MULTIPLIER = 0.5;

    @ConfigEntry.BoundedDiscrete(min=0, max=1)
    public double TREASURE_CHEST_CHANCE = 0.15;

    @ConfigEntry.BoundedDiscrete(min=0, max=1)
    public double GOLDEN_CHEST_CHANCE = 0.1;

    public int getQuality(double accuracy) {
        if (accuracy >= QUALITY_3_THRESHOLD) {
            return 3;
        } else if (accuracy >= QUALITY_2_THRESHOLD) {
            return 2;
        } else if (accuracy >= QUALITY_1_THRESHOLD) {
            return 1;
        }
        return 0;
    }

    public double getMultiplier(double accuracy, boolean qualityBobber) {
        double multiplier = switch(getQuality(accuracy)) {
            case 3 -> QUALITY_3_MULTIPLIER;
            case 2 -> QUALITY_2_MULTIPLIER;
            case 1 -> QUALITY_1_MULTIPLIER;
            default -> 1;
        };
        return qualityBobber ? multiplier * QUALITY_BOBBER_MULTIPLIER : multiplier;
    }

    public double getBiteTimeMultiplier() {
        return BITE_TIME_MULTIPLIER;
    }

    public double getTreasureChestChance() {
        return TREASURE_CHEST_CHANCE;
    }

    public double getGoldenChestChance() {
        return GOLDEN_CHEST_CHANCE;
    }
}