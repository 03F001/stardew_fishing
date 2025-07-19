package com.bonker.stardew_fishing.api;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.List;

final public class StardewFishingAPI {
    private static API impl;

    @ApiStatus.Internal
    public static void __init(@NotNull API impl) {
        StardewFishingAPI.impl = impl;
    }

    public static boolean detour_FishingHook$retrieve(ItemStack rod, @NotNull FishingHook hook, @NotNull List<ItemStack> loot) { return impl.detour_FishingHook$retrieve(rod, hook, loot); }

    public static void startMinigame(ServerPlayer player, ItemStack fish, API.Chest chest) { impl.startMinigame(player, fish, chest); }

    public static boolean isStartMinigame(ItemStack item) { return impl.isStartMinigame(item); }

    public static API.FishingHookExt getFishingHookExt(FishingHook hook) { return impl.getFishingHookExt(hook); }

    public static double getBiteTimeMultiplier() {
        return impl.getBiteTimeMultiplier();
    }
    public static double getTreasureChestChance() {
        return impl.getTreasureChestChance();
    }
    public static double getGoldenChestChance() {
        return impl.getGoldenChestChance();
    }

    public static API.Chest rollChest(ServerPlayer player) { return impl.rollChest(player); }
}