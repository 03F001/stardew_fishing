package com.bonker.stardewfishing.forge;

import com.bonker.stardewfishing.Sound;
import com.bonker.stardewfishing.StardewFishing;
import com.bonker.stardewfishing.compat.QualityFoodProxy;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.ItemFishedEvent;

import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FishingHook extends com.bonker.stardewfishing.FishingHook {
    private final ArrayList<ItemStack> rewards = new ArrayList<>();
    private boolean treasureChest = false, goldenChest = false;

    public static Optional<ArrayList<ItemStack>> getStoredRewards(net.minecraft.world.entity.projectile.FishingHook entity) {
        return entity.getCapability(CapProvider.CAP).map(cap -> cap.rewards);
    }

    public static boolean startMinigame(ServerPlayer player) {
        if (player.fishing == null || player instanceof FakePlayer) return false;

        player.fishing.getCapability(CapProvider.CAP).resolve().ifPresent(cap -> {
            ItemStack fish = cap.rewards.stream()
                    .filter(stack -> stack.is(StardewFishing.STARTS_MINIGAME))
                    .findFirst()
                    .orElseThrow();

            double chestChance = StardewFishing.platform.getTreasureChestChance();
            if (StardewFishing.BOBBER_ITEMS_REGISTERED && chestChance < 1) {
                InteractionHand hand = getRodHand(player);
                if (hand != null) {
                    if (hasBobber(player.getItemInHand(hand), com.bonker.stardewfishing.Item.treasure_bobber)) {
                        chestChance += 0.05;
                    }
                }
            }

            if (player.getRandom().nextFloat() < chestChance) {
                cap.treasureChest = true;
                if (player.getRandom().nextFloat() < StardewFishing.platform.getGoldenChestChance()) {
                    cap.goldenChest = true;
                }
            }

            StardewFishing.platform.startMinigame(player, fish, cap.treasureChest, cap.goldenChest);
        });

        return true;
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
        if (player.fishing == null) return;
        getStoredRewards(player.fishing).ifPresent(rewards -> modifyRewards(rewards, accuracy, fishingRod));
    }

    public static void modifyRewards(List<ItemStack> rewards, double accuracy, @Nullable ItemStack fishingRod) {
        if (StardewFishing.QUALITY_FOOD_INSTALLED) {
            int quality = StardewFishing.platform.getQuality(accuracy);
            if (quality < 3 && hasBobber(fishingRod, com.bonker.stardewfishing.Item.quality_bobber)) {
                quality++;
            }
            for (ItemStack reward : rewards) {
                if (reward.is(StardewFishing.STARTS_MINIGAME)) {
                    if (quality == 0 && reward.hasTag() && reward.getOrCreateTag().contains("quality_food")) {
                        if (reward.getOrCreateTag().size() > 1) {
                            reward.getOrCreateTag().remove("quality_food");
                        } else {
                            reward.setTag(null);
                        }
                    } else if (quality > 0) {
                        QualityFoodProxy.applyQuality(reward, quality);
                    }
                }
            }
        }
    }

    public static void giveRewards(ServerPlayer player, double accuracy, boolean gotChest) {
        if (player.fishing == null) return;

        var hook = player.fishing;

        hook.getCapability(CapProvider.CAP).ifPresent(cap -> {
            if (cap.treasureChest && gotChest) {
                cap.rewards.addAll(getTreasureChestLoot(player.serverLevel(), cap.goldenChest));
            }

            if (cap.rewards.isEmpty()) {
                hook.discard();
            }

            if (MinecraftForge.EVENT_BUS.post(new ItemFishedEvent(cap.rewards, 1, hook))) {
                player.level().playSound(null, player, StardewFishing.platform.getSoundEvent(Sound.pull_item), SoundSource.PLAYERS, 1.0F, 1.0F);
                hook.discard();
                return;
            }

            ServerLevel level = player.serverLevel();
            for (ItemStack reward : cap.rewards) {
                if (reward.is(ItemTags.FISHES)) {
                    player.awardStat(Stats.FISH_CAUGHT);
                }

                ItemEntity itementity;
                if (level.getFluidState(hook.blockPosition()).is(FluidTags.LAVA)) {
                    itementity = new ItemEntity(level, hook.getX(), hook.getY(), hook.getZ(), reward) {
                        public boolean displayFireAnimation() {
                            return false;
                        }

                        public void lavaHurt() {
                        }
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
                boolean qualityBobber = hand != null && hasBobber(handItem, com.bonker.stardewfishing.Item.quality_bobber);
                int exp = (int) ((player.getRandom().nextInt(6) + 1) * StardewFishing.platform.getMultiplier(accuracy, qualityBobber));

                level.addFreshEntity(new ExperienceOrb(level, player.getX(), player.getY() + 0.5, player.getZ() + 0.5, exp));

                CriteriaTriggers.FISHING_ROD_HOOKED.trigger(player, handItem, hook, cap.rewards);
            }

            player.level().playSound(null, player, StardewFishing.platform.getSoundEvent(Sound.pull_item), SoundSource.PLAYERS, 1.0F, 1.0F);
        });
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

    static class CapProvider implements ICapabilityProvider {
        static final Capability<FishingHook> CAP = CapabilityManager.get(new CapabilityToken<>() {});
        static final ResourceLocation NAME = new ResourceLocation(StardewFishing.MODID, "hook");

        private final LazyOptional<FishingHook> optional = LazyOptional.of(FishingHook::new);

        @Override
        public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
            return cap == CAP ? optional.cast() : LazyOptional.empty();
        }
    }

    @Mod.EventBusSubscriber(modid = StardewFishing.MODID)
    public static class ForgeBus {
        @SubscribeEvent
        public static void onAttachCapabilitiesPlayer(final AttachCapabilitiesEvent<Entity> event) {
            if (event.getObject() instanceof net.minecraft.world.entity.projectile.FishingHook) {
                if (!event.getObject().getCapability(FishingHook.CapProvider.CAP).isPresent()) {
                    event.addCapability(FishingHook.CapProvider.NAME, new FishingHook.CapProvider());
                }
            }
        }
    }
}
