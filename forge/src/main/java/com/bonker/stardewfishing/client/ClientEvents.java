package com.bonker.stardewfishing.client;

import com.bonker.stardewfishing.StardewFishing;
import com.bonker.stardewfishing.common.init.SFSoundEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.sound.PlaySoundSourceEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

public class ClientEvents {
    @Mod.EventBusSubscriber(modid = StardewFishing.MODID, value = Dist.CLIENT)
    public static class ForgeBus {
        @SubscribeEvent
        public static void onSoundPlayed(final PlaySoundSourceEvent event) {
            try {
                if (event.getSound() instanceof SimpleSoundInstance instance) {
                    if (event.getSound().getLocation().getNamespace().equals("minecraft")) {
                        SoundEvent newEvent = switch (event.getSound().getLocation().getPath()) {
                            case "entity.fishing_bobber.throw" -> SFSoundEvents.CAST.get();
                            case "entity.fishing_bobber.retrieve" -> {
                                if (Minecraft.getInstance().level == null) yield null;
                                Player player = Minecraft.getInstance().level.getNearestPlayer(event.getSound().getX(), event.getSound().getY(), event.getSound().getZ(), 1, false);
                                yield player == null || player.fishing == null ? SFSoundEvents.PULL_ITEM.get() : SFSoundEvents.FISH_HIT.get();
                            }
                            case "entity.fishing_bobber.splash" -> SFSoundEvents.FISH_BITE.get();
                            default -> null;
                        };

                        if (newEvent != null) {
                            event.getEngine().stop(instance);
                            event.getEngine().play(new SimpleSoundInstance(
                                    newEvent,
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
