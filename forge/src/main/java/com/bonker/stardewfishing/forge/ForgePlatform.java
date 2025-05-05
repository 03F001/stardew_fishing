package com.bonker.stardewfishing.forge;

import com.bonker.stardewfishing.Platform;
import com.bonker.stardewfishing.Sound;
import com.bonker.stardewfishing.StardewFishing;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.*;
import java.util.stream.*;

public class ForgePlatform implements Platform {
    private final DeferredRegister<SoundEvent> soundsRegistry = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, StardewFishing.MODID);

    private final ArrayList<RegistryObject<SoundEvent>> sounds = Arrays.stream(Sound.values())
        .map(s -> soundsRegistry.register(s.name(), () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(StardewFishing.MODID, s.name()))))
        .collect(Collectors.toCollection(ArrayList::new));

    public ForgePlatform(IEventBus bus) {
        soundsRegistry.register(bus);
    }

    @Override
    public SoundEvent getSoundEvent(Sound sound) {
        return sounds.get(sound.ordinal()).get();
    }
}
