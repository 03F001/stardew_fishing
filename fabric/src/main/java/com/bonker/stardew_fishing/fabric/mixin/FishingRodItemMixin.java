package com.bonker.stardew_fishing.fabric.mixin;

import com.bonker.stardew_fishing.Sound;
import com.bonker.stardew_fishing.StardewFishing;

import net.minecraft.client.Minecraft;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
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
    private void redirectPlaySoundFirst(Level instance, Player player, double x, double y, double z, SoundEvent sound, SoundSource category, float volume, float pitch) {
        Player nearestPlayer = null;
        if (Minecraft.getInstance().level != null) {
            nearestPlayer = Minecraft.getInstance().level.getNearestPlayer(x, y, z, 1, false);
        }
        if (nearestPlayer != null && nearestPlayer.fishing instanceof FishingHook) {
            FishingHook fishingHook = nearestPlayer.fishing;

            boolean isBiting = fishingHook.getEntityData().get(FishingHookAccessor.getDataBiting());
            instance.playSound(null, x, y, z, StardewFishing.platform.getSoundEvent(isBiting ? Sound.fish_hit : Sound.pull_item), SoundSource.NEUTRAL, 1.0F, 1.0F);
        }
    }

    @Redirect(
        method = "use",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;playSound(Lnet/minecraft/world/entity/player/Player;DDDLnet/minecraft/sounds/SoundEvent;Lnet/minecraft/sounds/SoundSource;FF)V",
            ordinal = 1))
    private void redirectPlaySoundSecond(Level instance, Player player, double x, double y, double z, SoundEvent sound, SoundSource category, float volume, float pitch) {
        instance.playSound(null, x, y, z, StardewFishing.platform.getSoundEvent(Sound.cast), SoundSource.NEUTRAL, 1.0F, 1.0F);
    }
}