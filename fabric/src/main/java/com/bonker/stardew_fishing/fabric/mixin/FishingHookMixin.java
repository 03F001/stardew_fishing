package com.bonker.stardew_fishing.fabric.mixin;

import com.bonker.stardew_fishing.api.StardewFishingAPI;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(value = FishingHook.class)
public abstract class FishingHookMixin extends Entity implements FishingHookAccessor {
    private FishingHookMixin(EntityType<? extends Projectile> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    @Inject(method = "catchingFish", at = @At(value = "HEAD"), cancellable = true)
    private void cancel_catchingFish(BlockPos pPos, CallbackInfo ci) {
        FishingHook hook = (FishingHook) (Object) this;

        if (getNibble() <= 0 && getTimeUntilHooked() <= 0 && getTimeUntilLured() <= 0) {
            // replicate vanilla
            int time = Mth.nextInt(random, 100, 600);
            time -= getLureSpeed() * 20 * 5;

            // apply configurable reduction
            time = Math.max(1, (int) (time * StardewFishingAPI.getBiteTimeMultiplier()));

            setTimeUntilLured(time);
        }

        if (!StardewFishingAPI.getFishingHookExt(hook).rewards.isEmpty()) {
            ci.cancel();
        }
    }

    @Inject(
        method = "retrieve",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/storage/loot/LootParams$Builder;<init>(Lnet/minecraft/server/level/ServerLevel;)V"),
        locals = LocalCapture.CAPTURE_FAILSOFT,
        cancellable = true)
    public void retrieve(ItemStack pStack, CallbackInfoReturnable<Integer> cir,
                         net.minecraft.world.entity.player.Player player,
                         int i)
    {
        FishingHook hook = (FishingHook) (Object) this;
        StardewFishingAPI.detour_FishingHook$retrieve(pStack, hook);
        cir.cancel();
    }
}