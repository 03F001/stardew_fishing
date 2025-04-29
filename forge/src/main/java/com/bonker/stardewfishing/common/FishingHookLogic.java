package com.bonker.stardewfishing.common;

import com.bonker.stardewfishing.SFConfig;
import com.bonker.stardewfishing.StardewFishing;
import com.bonker.stardewfishing.common.init.SFItems;
import com.bonker.stardewfishing.common.init.SFSoundEvents;
import com.bonker.stardewfishing.common.networking.S2CStartMinigamePacket;
import com.bonker.stardewfishing.common.networking.SFNetworking;
import com.bonker.stardewfishing.proxy.BobberGetter;
import com.bonker.stardewfishing.proxy.QualityFoodProxy;
import com.bonker.stardewfishing.server.FishBehaviorReloadListener;
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
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.ToolActions;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.ItemFishedEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

public class FishingHookLogic {
    private final ArrayList<ItemStack> rewards = new ArrayList<>();
    private boolean treasureChest = false, goldenChest = false;

    public static void attachCap(AttachCapabilitiesEvent<Entity> event) {
        if (!event.getObject().getCapability(FishingHookLogic.CapProvider.CAP).isPresent()) {
            event.addCapability(CapProvider.NAME, new CapProvider());
        }
    }

    public static Optional<ArrayList<ItemStack>> getStoredRewards(FishingHook entity) {
        return entity.getCapability(CapProvider.CAP).map(cap -> cap.rewards);
    }

    /**
     * TODO: remove this, Exists for backwards compatibility with Tide.
     */
    @Deprecated(since = "2.0", forRemoval = true)
    public static void startMinigame(ServerPlayer player) {
        startStardewMinigame(player);
    }

    public static boolean startStardewMinigame(ServerPlayer player) {
        if (player.fishing == null || player instanceof FakePlayer) return false;

        player.fishing.getCapability(CapProvider.CAP).resolve().ifPresent(cap -> {
            ItemStack fish = cap.rewards.stream()
                    .filter(stack -> stack.is(StardewFishing.STARTS_MINIGAME))
                    .findFirst()
                    .orElseThrow();

            double chestChance = SFConfig.getTreasureChestChance();
            if (StardewFishing.BOBBER_ITEMS_REGISTERED && chestChance < 1) {
                InteractionHand hand = FishingHookLogic.getRodHand(player);
                if (hand != null) {
                    if (hasBobber(player.getItemInHand(hand), SFItems.TREASURE_BOBBER)) {
                        chestChance += 0.05;
                    }
                }
            }

            if (player.getRandom().nextFloat() < chestChance) {
                cap.treasureChest = true;
                if (player.getRandom().nextFloat() < SFConfig.getGoldenChestChance()) {
                    cap.goldenChest = true;
                }
            }

            SFNetworking.sendToPlayer(player, new S2CStartMinigamePacket(FishBehaviorReloadListener.getBehavior(fish), fish, cap.treasureChest, cap.goldenChest));
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

    // todo: added to avoid breaking tide support
    @Deprecated(since = "2.0", forRemoval = true)
    public static void modifyRewards(List<ItemStack> rewards, double accuracy) {
        modifyRewards(rewards, accuracy, null);
    }

    public static void modifyRewards(List<ItemStack> rewards, double accuracy, @Nullable ItemStack fishingRod) {
        if (StardewFishing.QUALITY_FOOD_INSTALLED) {
            int quality = SFConfig.getQuality(accuracy);
            if (quality < 3 && hasBobber(fishingRod, SFItems.QUALITY_BOBBER)) {
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

        FishingHook hook = player.fishing;

        hook.getCapability(CapProvider.CAP).ifPresent(cap -> {
            if (cap.treasureChest && gotChest) {
                cap.rewards.addAll(getTreasureChestLoot(player.serverLevel(), cap.goldenChest));
            }

            if (cap.rewards.isEmpty()) {
                hook.discard();
            }

            if (MinecraftForge.EVENT_BUS.post(new ItemFishedEvent(cap.rewards, 1, hook))) {
                player.level().playSound(null, player, SFSoundEvents.PULL_ITEM.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
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

                InteractionHand hand = FishingHookLogic.getRodHand(player);
                ItemStack handItem = hand != null ? player.getItemInHand(hand) : ItemStack.EMPTY;
                boolean qualityBobber = hand != null && hasBobber(handItem, SFItems.QUALITY_BOBBER);
                int exp = (int) ((player.getRandom().nextInt(6) + 1) * SFConfig.getMultiplier(accuracy, qualityBobber));

                level.addFreshEntity(new ExperienceOrb(level, player.getX(), player.getY() + 0.5, player.getZ() + 0.5, exp));

                CriteriaTriggers.FISHING_ROD_HOOKED.trigger(player, handItem, hook, cap.rewards);
            }

            player.level().playSound(null, player, SFSoundEvents.PULL_ITEM.get(), SoundSource.PLAYERS, 1.0F, 1.0F);
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

    public static InteractionHand getRodHand(Player player) {
        boolean mainHand = player.getItemInHand(InteractionHand.MAIN_HAND).canPerformAction(ToolActions.FISHING_ROD_CAST);
        if (mainHand) return InteractionHand.MAIN_HAND;

        boolean offHand = player.getItemInHand(InteractionHand.OFF_HAND).canPerformAction(ToolActions.FISHING_ROD_CAST);
        if (offHand) return InteractionHand.OFF_HAND;

        return null;
    }

    public static boolean hasBobber(@Nullable ItemStack fishingRod, Supplier<Item> itemSupplier) {
        if (!StardewFishing.BOBBER_ITEMS_REGISTERED || fishingRod == null) {
            return false;
        }
        return BobberGetter.getBobber(fishingRod).is(itemSupplier.get());
    }

    private static class CapProvider implements ICapabilityProvider {
        private static final Capability<FishingHookLogic> CAP = CapabilityManager.get(new CapabilityToken<>() {});
        private static final ResourceLocation NAME = new ResourceLocation(StardewFishing.MODID, "hook");

        private final LazyOptional<FishingHookLogic> optional = LazyOptional.of(FishingHookLogic::new);

        @Override
        public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
            return getCapability(cap);
        }

        @Override
        public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap) {
            return cap == CAP ? optional.cast() : LazyOptional.empty();
        }
    }
}
