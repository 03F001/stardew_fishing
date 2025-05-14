package com.bonker.stardewfishing.forge.client;

import com.bonker.stardewfishing.Sound;
import com.bonker.stardewfishing.StardewFishing;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.world.entity.player.Player;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.sound.PlaySoundSourceEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Optional;

public class ClientEvents {
    @Mod.EventBusSubscriber(modid = StardewFishing.MODID, value = Dist.CLIENT)
    public static class ForgeBus {
        @SubscribeEvent
        public static void onSoundPlayed(final PlaySoundSourceEvent event) {
            try {
                if (event.getSound() instanceof SimpleSoundInstance instance) {
                    if (event.getSound().getLocation().getNamespace().equals("minecraft")) {
                        Optional<Sound> sound = switch (event.getSound().getLocation().getPath()) {
                            case "entity.fishing_bobber.throw" -> Optional.of(Sound.cast);
                            case "entity.fishing_bobber.retrieve" -> {
                                if (Minecraft.getInstance().level == null) yield null;
                                Player player = Minecraft.getInstance().level.getNearestPlayer(event.getSound().getX(), event.getSound().getY(), event.getSound().getZ(), 1, false);
                                yield Optional.of(player == null || player.fishing == null ? Sound.pull_item : Sound.fish_hit);
                            }
                            case "entity.fishing_bobber.splash" -> Optional.of(Sound.fish_bite);
                            default -> Optional.empty();
                        };

                        if (sound.isPresent()) {
                            event.getEngine().stop(instance);
                            event.getEngine().play(new SimpleSoundInstance(
                                StardewFishing.platform.getSoundEvent(sound.get()),
                                instance.getSource(),
                                1.0F,
                                1.0F,
                                SoundInstance.createUnseededRandom(),
                                instance.getX(),
                                instance.getY(),
                                instance.getZ()));
                        }
                    }
                }
            } catch (Exception e) {
                StardewFishing.LOGGER.error("An exception occurred while trying to replace a sound event. I think this happens when you try to use a fishing rod in extremely laggy conditions.", e);
            }
        }
    }
}
