package com.bonker.stardewfishing;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryType;

import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

public interface Platform
{
    SoundEvent getSoundEvent(Sound sound);

    LootPoolEntryType getLootPoolEntryType();

    void startMinigame(ServerPlayer player, ItemStack fish, boolean treasureChest, boolean goldenChest);
    void completeMinigame(boolean success, double accuracy, boolean gotChest);

    FishBehavior getFishBehavior(@Nullable ItemStack stack);
    int getQuality(double accuracy);
    double getMultiplier(double accuracy, boolean qualityBobber);
    double getBiteTimeMultiplier();
    double getTreasureChestChance();
    double getGoldenChestChance();

    net.minecraft.world.item.Item getItem(com.bonker.stardewfishing.Item item);
    ItemStack getBobber(ItemStack fishingRod);

    void renderTooltip(GuiGraphics pGuiGraphics, Font font, List<Component> textComponents, Optional<TooltipComponent> tooltipComponent, ItemStack stack, int mouseX, int mouseY);
}