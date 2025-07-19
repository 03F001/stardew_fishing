package com.bonker.stardew_fishing.forge;

import com.bonker.stardew_fishing.Item;
import com.bonker.stardew_fishing.Sound;
import com.bonker.stardew_fishing.api.StardewFishingAPI;
import com.bonker.stardew_fishing.StardewFishing;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.forgespi.language.IModInfo;
import net.minecraftforge.registries.ForgeRegistries;

import org.jetbrains.annotations.NotNull;

import java.util.List;

import static com.bonker.stardew_fishing.FishingHookExt.hasBobber;

@Mod(StardewFishing.MODID)
public class Entry {
    public Entry() {
        StardewFishing.QUALITY_FOOD_INSTALLED = ModList.get().isLoaded("quality_food");
        StardewFishing.AQUACULTURE_INSTALLED = ModList.get().isLoaded("aquaculture");
        StardewFishing.TIDE_INSTALLED = ModList.get().isLoaded("tide");
        StardewFishing.BOBBER_ITEMS_REGISTERED = StardewFishing.AQUACULTURE_INSTALLED || StardewFishing.TIDE_INSTALLED;

        StardewFishing.STARTS_MINIGAME = TagKey.create(ForgeRegistries.ITEMS.getRegistryKey(), StardewFishing.mkResLoc("starts_minigame"));

        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();

        IModInfo info = ModLoadingContext.get().getActiveContainer().getModInfo();
        StardewFishing.MOD_NAME = info.getDisplayName() + " " + info.getVersion();

        StardewFishing.platform = new ForgePlatform(bus);
        StardewFishingAPI.__init(new API());

        if (StardewFishing.BOBBER_ITEMS_REGISTERED) {
            Items.ITEMS.register(bus);
            Items.CREATIVE_MODE_TABS.register(bus);
        }

        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, Config.SERVER_SPEC);
    }
}

final class API implements com.bonker.stardew_fishing.api.API {
    public boolean detour_FishingHook$retrieve(ItemStack rod, @NotNull FishingHook hook, @NotNull List<ItemStack> loot) {
        var player = (ServerPlayer)hook.getPlayerOwner();
        if (player == null)
            return false;

        var ext = getFishingHookExt(hook);
        for (var item : loot) {
            if (isStartMinigame(item)) {
                ext.rewards.addAll(loot);
                startMinigame(player, item, rollChest(player));
                return true;
            }
        }

        com.bonker.stardew_fishing.FishingHookExt.modifyRewards(loot, 0, rod); // todo
        player.level().playSound(null, player, StardewFishing.platform.getSoundEvent(Sound.pull_item), SoundSource.PLAYERS, 1.0F, 1.0F);
        return false;
    }

    @Override
    public void startMinigame(ServerPlayer player, ItemStack fish, Chest chest) {
        StardewFishing.platform.startMinigame(player, fish, chest);
    }

    @Override
    public boolean isStartMinigame(ItemStack item) {
        return item.is(StardewFishing.STARTS_MINIGAME);
    }

    @Override
    public FishingHookExt getFishingHookExt(FishingHook hook) { return StardewFishing.platform.getFishingHookExt(hook); }

    @Override
    public double getBiteTimeMultiplier() {
        return StardewFishing.platform.getBiteTimeMultiplier();
    }

    @Override
    public double getTreasureChestChance() {
        return StardewFishing.platform.getTreasureChestChance();
    }

    @Override
    public double getGoldenChestChance() {
        return StardewFishing.platform.getGoldenChestChance();
    }

    @Override
    public Chest rollChest(ServerPlayer player) {
        double chance = getTreasureChestChance();
        if (StardewFishing.BOBBER_ITEMS_REGISTERED && chance < 1) {
            InteractionHand hand = com.bonker.stardew_fishing.FishingHookExt.getRodHand(player);
            if (hand != null) {
                if (hasBobber(player.getItemInHand(hand), Item.treasure_bobber)) {
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
}