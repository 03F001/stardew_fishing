package com.bonker.stardewfishing.forge.mixin;

import com.bonker.stardewfishing.Sound;
import com.bonker.stardewfishing.StardewFishing;

import com.llamalad7.mixinextras.sugar.Local;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Pseudo
@Mixin(targets = "com.scouter.netherdepthsupgrade.entity.entities.LavaFishingBobberEntity")
public abstract class LavaFishingBobberEntityMixin extends FishingHook {
    @Shadow private int nibble;

    @Shadow private int timeUntilHooked;

    @Shadow private int timeUntilLured;

    @Shadow @Final private int lureSpeed;

    private LavaFishingBobberEntityMixin(EntityType<? extends FishingHook> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    @Inject(method = "catchingFish(Lnet/minecraft/core/BlockPos;)V", at = @At(value = "HEAD"), cancellable = true, remap = false)
    private void cancel_catchingFish(BlockPos pPos, CallbackInfo ci) {
        if (nibble <= 0 && timeUntilHooked <= 0 && timeUntilLured <= 0) {
            // replicate vanilla
            timeUntilLured = Mth.nextInt(random, 100, 600);
            timeUntilLured -= lureSpeed * 20 * 5;

            // apply configurable reduction
            timeUntilLured = Math.max(1, (int) (timeUntilLured * StardewFishing.platform.getBiteTimeMultiplier()));
        }

        if (com.bonker.stardewfishing.forge.FishingHook.getStoredRewards(this).isEmpty()) {
            ci.cancel();
        }
    }

    @Inject(method = "retrieve(Lnet/minecraft/world/item/ItemStack;)I",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraftforge/eventbus/api/IEventBus;post(Lnet/minecraftforge/eventbus/api/Event;)Z"),
            cancellable = true)
    public void retrieve(ItemStack pStack, CallbackInfoReturnable<Integer> cir, @Local List<ItemStack> items) {
        ServerPlayer player = (ServerPlayer) getPlayerOwner();
        if (player == null) return;

        if (items.stream().anyMatch(stack -> stack.is(StardewFishing.STARTS_MINIGAME))) {
            com.bonker.stardewfishing.forge.FishingHook.getStoredRewards(this).ifPresent(rewards -> rewards.addAll(items));
            if (com.bonker.stardewfishing.forge.FishingHook.startMinigame(player)) {
                cir.cancel();
            }
        } else {
            com.bonker.stardewfishing.forge.FishingHook.modifyRewards(items, 0, null);
            player.level().playSound(null, player, StardewFishing.platform.getSoundEvent(Sound.pull_item), SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }
}
