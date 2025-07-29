package com.bonker.stardew_fishing.api;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

final public class StardewFishingAPI {
    private static API impl;

    @ApiStatus.Internal
    public static void __init(@NotNull API impl) {
        StardewFishingAPI.impl = impl;
    }

    public static int detour_FishingHook$retrieve(ItemStack rod, @NotNull FishingHook hook) { return impl.detour_FishingHook$retrieve(rod, hook); }

    public static void startMinigame(ServerPlayer player, ItemStack fish, API.Chest chest) { impl.startMinigame(player, fish, chest); }
    public static void startExtMinigame(ServerPlayer player, ItemStack fish, @Nullable List<ItemStack> loot, API.Chest chest) { impl.startExtMinigame(player, fish, loot, chest); }
    public static API.EventMinigameEnd endMinigame(ServerPlayer player, boolean success, double accuracy, boolean gotChest) { return impl.endMinigame(player, success, accuracy, gotChest); }

    public static boolean isStartMinigame(ItemStack item) { return impl.isStartMinigame(item); }
    public static boolean isMinigameStarted(FishingHook hook) { return impl.isMinigameStarted(hook); }

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

    public static void register(API.ListenerFishRetrieveBegin listener) { impl.register(listener); }
    public static void registerBefore(API.ListenerFishRetrieveBegin before, API.ListenerFishRetrieveBegin listener) { impl.registerBefore(before, listener); }
    public static void registerAfter(API.ListenerFishRetrieveBegin after, API.ListenerFishRetrieveBegin listener) { impl.registerAfter(after, listener); }
    public static boolean unregister(API.ListenerFishRetrieveBegin listener) { return impl.unregister(listener); }

    public static void register(API.ListenerExtMinigameEnd listener) { impl.register(listener); }
    public static void registerBefore(API.ListenerExtMinigameEnd before, API.ListenerExtMinigameEnd listener) { impl.registerBefore(before, listener); }
    public static void registerAfter(API.ListenerExtMinigameEnd after, API.ListenerExtMinigameEnd listener) { impl.registerAfter(after, listener); }
    public static boolean unregister(API.ListenerExtMinigameEnd listener) { return impl.unregister(listener); }
}