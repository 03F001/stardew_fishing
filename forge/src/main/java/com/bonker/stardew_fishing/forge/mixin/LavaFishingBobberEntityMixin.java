package com.bonker.stardew_fishing.forge.mixin;

import com.bonker.stardew_fishing.api.StardewFishingAPI;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
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
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

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

    @Inject(
        method = "catchingFish(Lnet/minecraft/core/BlockPos;)V",
        at = @At(value = "HEAD"),
        cancellable = true,
        remap = false)
    private void cancel_catchingFish(BlockPos pPos, CallbackInfo ci) {
        if (nibble <= 0 && timeUntilHooked <= 0 && timeUntilLured <= 0) {
            // replicate vanilla
            timeUntilLured = Mth.nextInt(random, 100, 600);
            timeUntilLured -= lureSpeed * 20 * 5;

            // apply configurable reduction
            timeUntilLured = Math.max(1, (int) (timeUntilLured * StardewFishingAPI.getBiteTimeMultiplier()));
        }

        if (StardewFishingAPI.isMinigameStarted(this)) {
            ci.cancel();
        }
    }

    @Inject(
        method = "retrieve(Lnet/minecraft/world/item/ItemStack;)I",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/storage/loot/LootParams$Builder;<init>(Lnet/minecraft/server/level/ServerLevel;)V"),
        locals = LocalCapture.CAPTURE_FAILSOFT,
        cancellable = true)
    public void retrieve(ItemStack pStack, CallbackInfoReturnable<Integer> cir, List<ItemStack> items) {
        ServerPlayer player = (ServerPlayer) getPlayerOwner();
        var rodDamage = StardewFishingAPI.detour_FishingHook$retrieve(pStack, this);
        cir.setReturnValue(rodDamage);
        cir.cancel();
    }
}
