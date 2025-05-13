package com.bonker.stardewfishing;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryType;

public interface Platform
{
    SoundEvent getSoundEvent(Sound sound);

    LootPoolEntryType getLootPoolEntryType();

    void startMinigame(ServerPlayer player, ItemStack fish, boolean treasureChest, boolean goldenChest);
    void completeMinigame(boolean success, double accuracy, boolean gotChest);
}