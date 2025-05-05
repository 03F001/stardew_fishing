package com.bonker.stardewfishing;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryType;

public interface Platform
{
    SoundEvent getSoundEvent(Sound sound);

    LootPoolEntryType getLootPoolEntryType();
}