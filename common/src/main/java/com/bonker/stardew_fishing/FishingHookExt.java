package com.bonker.stardew_fishing;

import com.bonker.stardew_fishing.api.API;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class FishingHookExt extends API.FishingHookExt {
    public static ArrayList<ItemStack> getStoredRewards(FishingHook hook) {
        return StardewFishing.platform.getFishingHookExt(hook).rewards;
    }

    public static void endMinigame(Player player, boolean success, double accuracy, boolean gotChest, @Nullable ItemStack fishingRod) {
        if (success && !player.level().isClientSide) {
            modifyRewards((ServerPlayer) player, accuracy, fishingRod);
            giveRewards((ServerPlayer) player, accuracy, gotChest);
        }

        if (player.fishing != null) {
            player.fishing.discard();
        }
    }

    public static void modifyRewards(ServerPlayer player, double accuracy, @Nullable ItemStack fishingRod) {
        if (!StardewFishing.QUALITY_FOOD_INSTALLED) return;
        if (player.fishing == null) return;

        modifyRewards(getStoredRewards(player.fishing), accuracy, fishingRod);
    }

    public static void modifyRewards(List<ItemStack> rewards, double accuracy, @Nullable ItemStack fishingRod) {
        StardewFishing.platform.modifyRewards(rewards, accuracy, fishingRod);
    }

    public static void giveRewards(ServerPlayer player, double accuracy, boolean gotChest) {
        if (player.fishing == null) return;

        var hook = player.fishing;
        StardewFishing.platform.getFishingHookExt(hook).giveRewards(hook, player, accuracy, gotChest);
    }

    public void giveRewards(FishingHook hook, ServerPlayer player, double accuracy, boolean gotChest) {
        if (chest != API.Chest.none && gotChest) {
            rewards.addAll(getTreasureChestLoot(player.serverLevel(), chest == API.Chest.golden));
        }

        if (rewards.isEmpty()) {
            hook.discard();
        }

        if (StardewFishing.platform.eventItemFished(rewards,1, hook)) {
            player.level().playSound(null, player, StardewFishing.platform.getSoundEvent(Sound.pull_item), SoundSource.PLAYERS, 1.0F, 1.0F);
            hook.discard();
            return;
        }

        ServerLevel level = player.serverLevel();
        for (ItemStack reward : rewards) {
            if (reward.is(ItemTags.FISHES)) {
                player.awardStat(Stats.FISH_CAUGHT);
            }

            ItemEntity itementity;
            if (level.getFluidState(hook.blockPosition()).is(FluidTags.LAVA)) {
                itementity = new ItemEntity(level, hook.getX(), hook.getY(), hook.getZ(), reward) {
                    public boolean displayFireAnimation() { return false; }
                    public void lavaHurt() {}
                };
            } else {
                itementity = new ItemEntity(level, hook.getX(), hook.getY(), hook.getZ(), reward);
            }
            double scale = 0.1;
            double dx = player.getX() - hook.getX();
            double dy = player.getY() - hook.getY();
            double dz = player.getZ() - hook.getZ();
            itementity.setDeltaMovement(dx * scale, dy * scale + Math.sqrt(Math.sqrt(dx * dx + dy * dy + dz * dz)) * 0.08, dz * scale);
            level.addFreshEntity(itementity);

            InteractionHand hand = getRodHand(player);
            ItemStack handItem = hand != null ? player.getItemInHand(hand) : ItemStack.EMPTY;
            boolean qualityBobber = hand != null && hasBobber(handItem, Item.quality_bobber);
            int exp = (int) ((player.getRandom().nextInt(6) + 1) * StardewFishing.platform.getMultiplier(accuracy, qualityBobber));

            level.addFreshEntity(new ExperienceOrb(level, player.getX(), player.getY() + 0.5, player.getZ() + 0.5, exp));

            CriteriaTriggers.FISHING_ROD_HOOKED.trigger(player, handItem, hook, rewards);
        }

        player.level().playSound(null, player, StardewFishing.platform.getSoundEvent(Sound.pull_item), SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    private static List<ItemStack> getTreasureChestLoot(ServerLevel level, boolean isGolden) {
        LootTable lootTable = level.getServer().getLootData().getLootTable(level.dimension() == Level.NETHER ? StardewFishing.TREASURE_CHEST_NETHER_LOOT : StardewFishing.TREASURE_CHEST_LOOT);
        List<ItemStack> items = new ArrayList<>();

        int rolls;
        if (isGolden) {
            rolls = 2; // 100% for at least 2
            if (level.random.nextFloat() < 0.25F) {
                rolls++; // 1 in 4 chance to get 3

                if (level.random.nextFloat() < 0.5F) {
                    rolls++; // 1 in 8 chance to get 4
                }
            }
        } else {
            rolls = 1;
        }

        for (int i = 0; i < rolls; i++) {
            items.addAll(lootTable.getRandomItems((new LootParams.Builder(level)).create(LootContextParamSets.EMPTY)));
        }

        return items;
    }

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