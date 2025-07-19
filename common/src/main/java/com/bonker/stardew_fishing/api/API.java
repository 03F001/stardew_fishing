package com.bonker.stardew_fishing.api;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public interface API {
    boolean detour_FishingHook$retrieve(ItemStack rod, @NotNull FishingHook hook, @NotNull List<ItemStack> loot);

    enum Chest {
        none,
        treasure,
        golden,
    }

    void startMinigame(ServerPlayer player, ItemStack fish, Chest chest);

    boolean isStartMinigame(ItemStack item);

    class FishingHookExt {
        public ArrayList<ItemStack> rewards = new ArrayList<>();
        public Chest chest = Chest.none;
    }

    FishingHookExt getFishingHookExt(FishingHook hook);

    double getBiteTimeMultiplier();
    double getTreasureChestChance();
    double getGoldenChestChance();

    Chest rollChest(ServerPlayer player);
}