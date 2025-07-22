package com.bonker.stardew_fishing.api;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

interface EventCancellable {
    void cancel();
}

public interface API {
    void detour_FishingHook$retrieve(ItemStack rod, @NotNull FishingHook hook);

    enum Chest {
        none,
        treasure,
        golden,
    }

    void startMinigame(ServerPlayer player, ItemStack fish, Chest chest);
    void endMinigame(ServerPlayer player, boolean success, double accuracy, boolean gotChest);

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

    abstract class EventFishRetrieveBegin implements EventCancellable {
        public ServerPlayer player;
        public ItemStack rod;
        public FishingHook hook;

        public ArrayList<ItemStack> inout_loot;
        public Chest inout_chest;
        public ItemStack inout_fish;
    }
    interface ListenerFishRetrieveBegin {
        void event(EventFishRetrieveBegin evt);
    }
    void register(ListenerFishRetrieveBegin listener);
    void registerBefore(ListenerFishRetrieveBegin before, ListenerFishRetrieveBegin listener);
    void registerAfter(ListenerFishRetrieveBegin after, ListenerFishRetrieveBegin listener);
    boolean unregister(ListenerFishRetrieveBegin listener);

    abstract class EventMinigameEnd implements EventCancellable {
        public ServerPlayer player;
        public ItemStack rod;
        public FishingHook hook;
        public ArrayList<ItemStack> initialLoot;
        public boolean success;
        public double accuracy;
        public Chest chest;

        public ArrayList<ItemStack> inout_rewards;
    }
    interface ListenerMinigameEnd {
        void event(EventMinigameEnd evt);
    }
    void register(ListenerMinigameEnd listener);
    void registerBefore(ListenerMinigameEnd before, ListenerMinigameEnd listener);
    void registerAfter(ListenerMinigameEnd after, ListenerMinigameEnd listener);
    boolean unregister(ListenerMinigameEnd listener);
}