package com.bonker.stardewfishing.fabric.mixin;

import com.bonker.stardewfishing.FishingHookExt;
import com.bonker.stardewfishing.Sound;
import com.bonker.stardewfishing.StardewFishing;

import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.List;

@Mixin(value = FishingHook.class)
public abstract class FishingHookMixin extends Entity implements FishingHookAccessor {
    private FishingHookMixin(EntityType<? extends Projectile> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    @Inject(
        method = "retrieve",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/item/ItemEntity;<init>(Lnet/minecraft/world/level/Level;DDDLnet/minecraft/world/item/ItemStack;)V"),
        locals = LocalCapture.CAPTURE_FAILSOFT,
        cancellable = true)
    public void retrieve(ItemStack pStack, CallbackInfoReturnable<Integer> cir,
                         net.minecraft.world.entity.player.Player player,
                         int i,
                         net.minecraft.world.level.storage.loot.LootParams lootparams,
                         net.minecraft.world.level.storage.loot.LootTable loottable,
                         List<ItemStack> list)
    {
        FishingHook hook = (FishingHook) (Object) this;
        ServerPlayer serverPlayer = (ServerPlayer) hook.getPlayerOwner();
        if (serverPlayer == null) return;

        if (list.stream().anyMatch(stack -> stack.is(StardewFishing.STARTS_MINIGAME))) {
            FishingHookExt.getStoredRewards(hook).addAll(list);
            if (FishingHookExt.startMinigame(serverPlayer)) {
                cir.cancel();
            }
        } else {
            FishingHookExt.modifyRewards(list, 0, null);
            serverPlayer.level().playSound(null, serverPlayer, StardewFishing.platform.getSoundEvent(Sound.pull_item), SoundSource.PLAYERS, 1.0F, 1.0F);
        }
    }

    @Inject(method = "catchingFish", at = @At(value = "HEAD"), cancellable = true)
    private void cancel_catchingFish(BlockPos pPos, CallbackInfo ci) {
        FishingHook hook = (FishingHook) (Object) this;

        if (getNibble() <= 0 && getTimeUntilHooked() <= 0 && getTimeUntilLured() <= 0) {
            // replicate vanilla
            int time = Mth.nextInt(random, 100, 600);
            time -= getLureSpeed() * 20 * 5;

            // apply configurable reduction
            time = Math.max(1, (int) (time * StardewFishing.platform.getBiteTimeMultiplier()));

            setTimeUntilLured(time);
        }

        if (!FishingHookExt.getStoredRewards(hook).isEmpty()) {
            ci.cancel();
        }
    }
}

@Mixin(FishingHook.class)
interface FishingHookAccessor {
    @Accessor("DATA_BITING")
    static EntityDataAccessor<Boolean> getDataBiting() {
        throw new AssertionError("Untransformed accessor");
    }

    @Accessor
    int getNibble();

    @Accessor
    int getTimeUntilHooked();

    @Accessor
    int getTimeUntilLured();

    @Accessor
    void setTimeUntilLured(int value);

    @Accessor
    int getLureSpeed();
}