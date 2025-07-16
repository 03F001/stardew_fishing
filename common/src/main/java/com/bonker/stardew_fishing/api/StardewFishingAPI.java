package com.bonker.stardew_fishing.api;

import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class StardewFishingAPI {
    private static API impl;

    @ApiStatus.Internal
    public static void __init(@NotNull API impl) {
        StardewFishingAPI.impl = impl;
    }

    public static boolean tryStart(@NotNull FishingHook hook, @NotNull List<ItemStack> loot) {
        return impl.tryStart(hook, loot);
    }

    double getBiteTimeMultiplier() {
        return impl.getBiteTimeMultiplier();
    }

    double getTreasureChestChance() {
        return impl.getTreasureChestChance();
    }

    double getGoldenChestChance() {
        return impl.getGoldenChestChance();
    }
}