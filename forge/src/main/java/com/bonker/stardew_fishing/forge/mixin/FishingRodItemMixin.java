package com.bonker.stardew_fishing.forge.mixin;

import com.bonker.stardew_fishing.Sound;
import com.bonker.stardew_fishing.StardewFishing;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.level.Level;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(FishingRodItem.class)
public class FishingRodItemMixin {
    @Redirect(
        method = "use",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;playSound(Lnet/minecraft/world/entity/player/Player;DDDLnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FF)V",
            ordinal = 0))
    private void redirectPlaySoundFirst(Level level, Player player, double x, double y, double z, SoundEvent sound, SoundSource category, float volume, float pitch) {
        var p = level.getNearestPlayer(x, y, z, 1.0, false);
        boolean isBiting = p.fishing != null && p.fishing.getEntityData().get(FishingHookAccessor.getDataBiting());
        if (!isBiting) {
            level.playSound(player, x, y, z, sound, category, volume, pitch);
        }
    }

    @Redirect(
        method = "use",
        at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;playSound(Lnet/minecraft/world/entity/player/Player;DDDLnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FF)V",
            ordinal = 1))
    private void redirectPlaySoundSecond(Level level, Player player, double x, double y, double z, SoundEvent sound, SoundSource category, float volume, float pitch) {
        level.playSound(player, x, y, z, StardewFishing.platform.getSoundEvent(Sound.cast), SoundSource.NEUTRAL, 1.0F, 1.0F);
    }
}