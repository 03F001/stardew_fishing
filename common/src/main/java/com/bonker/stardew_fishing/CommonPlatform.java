package com.bonker.stardew_fishing;

import com.bonker.stardew_fishing.api.API;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

abstract public class CommonPlatform implements API, Platform {
    public CommonPlatform(ListenerFishRetrieveBegin vanillaLootGen) {
        // hack(efool): vanillaLootGen provided by subtypes because mixin is needed to access FishingHook.luck
        this.vanillaLootGen = vanillaLootGen;
        register(this.vanillaLootGen);

        register(stardewFishingGiveReward);
        register(vanillaGiveLoot);
        register(vanillaGiveLootExp);
    }

    @Override
    public void detour_FishingHook$retrieve(ItemStack rod, @NotNull FishingHook hook) {
        var player = (ServerPlayer)hook.getPlayerOwner();
        if (player == null)
            return;

        var evtBegin = emit_EventRetrieveBegin(player, rod, hook, new ArrayList<>(), Chest.none, null);
        if (evtBegin.inout_fish != null) {
            var ext = getFishingHookExt(hook);
            ext.rewards = evtBegin.inout_loot;
            ext.chest = evtBegin.inout_chest;
            startMinigame(player, evtBegin.inout_fish, ext.chest);
            return;
        }

        endMinigame(player, true, 0, false);
    }

    @Override
    public void endMinigame(ServerPlayer player, boolean success, double accuracy, boolean gotChest) {
        player.level().playSound(null, player, StardewFishing.platform.getSoundEvent(Sound.pull_item), SoundSource.PLAYERS, 1.0F, 1.0F);

        var hand = getRodHand(player);
        var hook = player.fishing;
        var ext = getFishingHookExt(hook);
        emit_EventMinigameEnd(
            player,
            hand != null ? player.getItemInHand(hand) : null,
            hook,
            ext.rewards,
            success,
            accuracy,
            !gotChest ? Chest.none : ext.chest);

        if (player.fishing != null) {
            player.fishing.discard();
        }
    }

    @Override
    public boolean isStartMinigame(ItemStack item) {
        return item.is(StardewFishing.STARTS_MINIGAME);
    }

    @Override
    public Chest rollChest(ServerPlayer player) {
        double chance = getTreasureChestChance();
        if (StardewFishing.BOBBER_ITEMS_REGISTERED && chance < 1) {
            InteractionHand hand = getRodHand(player);
            if (hand != null) {
                if (hasBobber(player.getItemInHand(hand), com.bonker.stardew_fishing.Item.treasure_bobber)) {
                    chance += 0.05; // todo(efool): configurable?
                }
            }
        }

        if (player.getRandom().nextFloat() < chance) {
            if (player.getRandom().nextFloat() < getGoldenChestChance()) {
                return Chest.golden;
            }
            return Chest.treasure;
        }

        return Chest.none;
    }

    private final ArrayList<ListenerFishRetrieveBegin> listenersFishRetrieveBegin = new ArrayList<>();

    @Override
    public void register(ListenerFishRetrieveBegin listener) {
        listenersFishRetrieveBegin.add(listener);
    }

    @Override
    public void registerBefore(ListenerFishRetrieveBegin before, ListenerFishRetrieveBegin listener) {
        var i = listenersFishRetrieveBegin.indexOf(before);
        if (i >= 0)
            listenersFishRetrieveBegin.add(i, listener);
        else
            listenersFishRetrieveBegin.add(listener);
    }

    @Override
    public void registerAfter(ListenerFishRetrieveBegin after, ListenerFishRetrieveBegin listener) {
        var i = listenersFishRetrieveBegin.indexOf(after);
        if (i >= 0)
            listenersFishRetrieveBegin.add(i+1, listener);
        else
            listenersFishRetrieveBegin.add(listener);
    }

    @Override
    public boolean unregister(ListenerFishRetrieveBegin listener) {
        return listenersFishRetrieveBegin.remove(listener);
    }

    public EventFishRetrieveBegin emit_EventRetrieveBegin(ServerPlayer player, ItemStack rod, FishingHook hook, ArrayList<ItemStack> inout_loot, Chest inout_chest, ItemStack inout_fish) {
        var evt = new EventFishRetrieveBegin() {
            public boolean cancelled = false;
            public void cancel() {
                cancelled = true;
            }
        };
        evt.player = player;
        evt.rod = rod;
        evt.hook = hook;

        evt.inout_loot = inout_loot;
        evt.inout_chest = inout_chest;
        evt.inout_fish = inout_fish;
        for (var l : listenersFishRetrieveBegin) {
            l.event(evt);
            if (evt.cancelled) {
                break;
            }
        }

        return evt;
    }

    private final ArrayList<API.ListenerMinigameEnd> listenersMinigameEnd = new ArrayList<>();

    @Override
    public void register(API.ListenerMinigameEnd listener) {
        listenersMinigameEnd.add(listener);
    }

    @Override
    public void registerBefore(ListenerMinigameEnd before, ListenerMinigameEnd listener) {
        var i = listenersMinigameEnd.indexOf(before);
        if (i >= 0)
            listenersMinigameEnd.add(i, listener);
        else
            listenersMinigameEnd.add(listener);
    }

    @Override
    public void registerAfter(ListenerMinigameEnd after, ListenerMinigameEnd listener) {
        var i = listenersMinigameEnd.indexOf(after);
        if (i >= 0)
            listenersMinigameEnd.add(i+1, listener);
        else
            listenersMinigameEnd.add(listener);
    }

    @Override
    public boolean unregister(ListenerMinigameEnd listener) {
        return listenersMinigameEnd.remove(listener);
    }

    public EventMinigameEnd emit_EventMinigameEnd(ServerPlayer player, ItemStack rod, FishingHook hook, ArrayList<ItemStack> initialLoot, boolean success, double accuracy, Chest chest) {
        var evt = new EventMinigameEnd() {
            public boolean cancelled = false;
            public void cancel() {
                cancelled = true;
            }
        };
        evt.player = player;
        evt.rod = rod;
        evt.hook = hook;
        evt.initialLoot = initialLoot;
        evt.success = success;
        evt.accuracy = accuracy;
        evt.chest = chest;
        evt.inout_rewards = new ArrayList<>();
        for (var l : listenersMinigameEnd) {
            l.event(evt);
            if (evt.cancelled) {
                break;
            }
        }

        return evt;
    }

    public final ListenerFishRetrieveBegin vanillaLootGen;

    public final ListenerMinigameEnd stardewFishingGiveReward = evt -> {
        if (!evt.success) return;

        evt.inout_rewards.addAll(evt.initialLoot);

        if (evt.chest == API.Chest.none) return;

        final var level = evt.player.serverLevel();
        LootTable lootTable = level.getServer().getLootData().getLootTable(level.dimension() == Level.NETHER
            ? StardewFishing.TREASURE_CHEST_NETHER_LOOT
            : StardewFishing.TREASURE_CHEST_LOOT);

        int rolls = 1;
        if (evt.chest == Chest.golden) {
            rolls = 2; // 100% for at least 2
            if (level.random.nextFloat() < 0.25F) {
                rolls++; // 1 in 4 chance to get 3

                if (level.random.nextFloat() < 0.5F) {
                    rolls++; // 1 in 8 chance to get 4
                }
            }
        }

        while (rolls-- > 0) {
            evt.inout_rewards.addAll(lootTable.getRandomItems((new LootParams.Builder(level)).create(LootContextParamSets.EMPTY)));
        }
    };

    public final ListenerMinigameEnd vanillaGiveLoot = evt -> {
        if (!evt.success || evt.player.level().isClientSide) return;

        InteractionHand hand = getRodHand(evt.player);
        ItemStack handItem = hand != null ? evt.player.getItemInHand(hand) : ItemStack.EMPTY;
        CriteriaTriggers.FISHING_ROD_HOOKED.trigger(evt.player, handItem, evt.hook, evt.inout_rewards);

        var level = evt.hook.level();
        for (ItemStack reward : evt.inout_rewards) {
            ItemEntity itementity = level.getFluidState(evt.hook.blockPosition()).is(FluidTags.LAVA)
                ? new ItemEntity(level, evt.hook.getX(), evt.hook.getY(), evt.hook.getZ(), reward) {
                public boolean fireImmune() { return true; }
            }
                : new ItemEntity(level, evt.hook.getX(), evt.hook.getY(), evt.hook.getZ(), reward);
            double dx = evt.player.getX() - evt.hook.getX();
            double dy = evt.player.getY() - evt.hook.getY();
            double dz = evt.player.getZ() - evt.hook.getZ();
            double scale = 0.1;
            itementity.setDeltaMovement(dx * scale, dy * scale + Math.sqrt(Math.sqrt(dx * dx + dy * dy + dz * dz)) * 0.08, dz * scale);
            level.addFreshEntity(itementity);

            if (reward.is(ItemTags.FISHES)) {
                evt.player.awardStat(Stats.FISH_CAUGHT);
            }
        }
    };

    public final ListenerMinigameEnd vanillaGiveLootExp = evt -> {
        if (!evt.success || evt.player.level().isClientSide) return;

        InteractionHand hand = getRodHand(evt.player);
        ItemStack handItem = hand != null ? evt.player.getItemInHand(hand) : ItemStack.EMPTY;
        boolean qualityBobber = hand != null && hasBobber(handItem, Item.quality_bobber);
        int exp = (int) ((evt.player.getRandom().nextInt(6) + 1) * StardewFishing.platform.getMultiplier(evt.accuracy, qualityBobber));

        var level = evt.hook.level();
        for (ItemStack reward : evt.inout_rewards) {
            evt.player.level().addFreshEntity(new ExperienceOrb(evt.player.level(), evt.player.getX(), evt.player.getY() + 0.5, evt.player.getZ() + 0.5, exp));
        }
    };

    public static InteractionHand getRodHand(Player player) {
        if (player.getMainHandItem().getItem() instanceof FishingRodItem)
            return InteractionHand.MAIN_HAND;
        else if (player.getOffhandItem().getItem() instanceof FishingRodItem)
            return InteractionHand.OFF_HAND;

        return null;
    }

    public static boolean hasBobber(@Nullable ItemStack fishingRod, Item item) {
        if (!StardewFishing.BOBBER_ITEMS_REGISTERED || fishingRod == null) {
            return false;
        }
        return StardewFishing.platform.getBobber(fishingRod).is(StardewFishing.platform.getItem(item));
    }
}