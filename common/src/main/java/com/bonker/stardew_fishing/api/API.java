package com.bonker.stardew_fishing.api;

import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface API {
    boolean tryStart(@NotNull FishingHook hook, @NotNull List<ItemStack> loot);

    double getBiteTimeMultiplier();
    double getTreasureChestChance();
    double getGoldenChestChance();
}