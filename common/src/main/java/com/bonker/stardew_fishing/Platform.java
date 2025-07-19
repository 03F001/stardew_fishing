package com.bonker.stardew_fishing;

import com.bonker.stardew_fishing.api.API;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryType;

import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public interface Platform {
    FishingHookExt getFishingHookExt(FishingHook hook);

    SoundEvent getSoundEvent(Sound sound);

    LootPoolEntryType getLootPoolEntryType();

    void startMinigame(ServerPlayer player, ItemStack fish, API.Chest chest);
    void completeMinigame(boolean success, double accuracy, boolean gotChest);

    void modifyRewards(List<ItemStack> rewards, double accuracy, @Nullable ItemStack fishingRod);
    boolean eventItemFished(List<ItemStack> rewards, int rodDamage, FishingHook hook);

    boolean isFakePlayer(ServerPlayer player);
    FishBehavior getFishBehavior(@Nullable ItemStack stack);
    int getQuality(double accuracy);
    double getMultiplier(double accuracy, boolean qualityBobber);
    double getBiteTimeMultiplier();
    double getTreasureChestChance();
    double getGoldenChestChance();

    net.minecraft.world.item.Item getItem(Item item);
    net.minecraft.world.item.Item getItem(ResourceLocation item);
    ItemStack getBobber(ItemStack fishingRod);

    void renderTooltip(GuiGraphics pGuiGraphics, Font font, List<Component> textComponents, Optional<TooltipComponent> tooltipComponent, ItemStack stack, int mouseX, int mouseY);
}