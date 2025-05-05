package com.bonker.stardewfishing;

import net.minecraft.sounds.SoundEvent;

public interface Platform
{
    SoundEvent getSoundEvent(Sound sound);
}