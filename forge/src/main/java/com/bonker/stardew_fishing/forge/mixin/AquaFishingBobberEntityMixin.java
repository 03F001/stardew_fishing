package com.bonker.stardew_fishing.forge.mixin;

import com.bonker.stardew_fishing.api.StardewFishingAPI;
import com.bonker.stardew_fishing.Sound;
import com.bonker.stardew_fishing.StardewFishing;

import com.teammetallurgy.aquaculture.entity.AquaFishingBobberEntity;
import com.teammetallurgy.aquaculture.init.AquaSounds;
import com.teammetallurgy.aquaculture.item.AquaFishingRodItem;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.loot.LootParams;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.ItemFishedEvent;
import net.minecraftforge.items.ItemStackHandler;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.List;

@Pseudo
@Mixin(targets = "com.teammetallurgy.aquaculture.entity.AquaFishingBobberEntity")
public abstract class AquaFishingBobberEntityMixin extends FishingHook implements FishingHookAccessor {
    @Shadow protected abstract List<ItemStack> getLoot(LootParams lootParams, ServerLevel serverLevel);

    @Accessor protected abstract ItemStack getFishingRod();

    private AquaFishingBobberEntityMixin(EntityType<? extends FishingHook> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    @Inject(
        method = "catchingFish(Lnet/minecraft/core/BlockPos;)V",
        at = @At(value = "HEAD"),
        cancellable = true)
    private void cancel_catchingFish(BlockPos pPos, CallbackInfo ci) {
        if (getNibble() <= 0 && getTimeUntilHooked() <= 0 && getTimeUntilLured() <= 0) {
            // replicate vanilla
            int time = Mth.nextInt(random, 100, 600);
            time -= getLureSpeed() * 20 * 5;

            // apply configurable reduction
            time = Math.max(1, (int) (time * StardewFishingAPI.getBiteTimeMultiplier()));

            setTimeUntilLured(time);
        }

        if (StardewFishingAPI.isMinigameStarted(this)) {
            ci.cancel();
        }
    }

    @Inject(
        method = "retrieve(Lnet/minecraft/world/item/ItemStack;)I",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraftforge/eventbus/api/IEventBus;post(Lnet/minecraftforge/eventbus/api/Event;)Z",
            ordinal = 0),
        locals = LocalCapture.CAPTURE_FAILSOFT,
        cancellable = true)
    public void retrieve(ItemStack pStack, CallbackInfoReturnable<Integer> cir, List<ItemStack> items, LootParams lootParams, ServerLevel serverLevel) {
        AquaFishingBobberEntity hook = (AquaFishingBobberEntity) (Object) this;
        ServerPlayer player = (ServerPlayer) hook.getPlayerOwner();
        if (player == null) return;

        for (var item : items) {
            if (!StardewFishingAPI.isStartMinigame(item)) continue;

            var rewards = StardewFishingAPI.getFishingHookExt(hook).loot;
            rewards.addAll(items);
            if (hook.hasHook() && hook.getHook().getDoubleCatchChance() > 0.0 && this.random.nextDouble() <= hook.getHook().getDoubleCatchChance()) {
                List<ItemStack> doubleLoot = getLoot(lootParams, serverLevel);
                if (!doubleLoot.isEmpty()) {
                    MinecraftForge.EVENT_BUS.post(new ItemFishedEvent(doubleLoot, 0, this));
                    rewards.addAll(doubleLoot);
                    playSound(SoundEvents.FISHING_BOBBER_SPLASH, 0.25F, 1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.4F);
                }
            }

            if (!player.isCreative()) {
                ItemStackHandler rodHandler = AquaFishingRodItem.getHandler(getFishingRod());
                ItemStack bait = rodHandler.getStackInSlot(1);
                if (!bait.isEmpty()) {
                    if (bait.hurt(1, hook.level().random, null)) {
                        bait.shrink(1);
                        playSound(AquaSounds.BOBBER_BAIT_BREAK.get(), 0.7F, 0.2F);
                    }

                    rodHandler.setStackInSlot(1, bait);
                }
            }

            StardewFishingAPI.startMinigame(player, item, StardewFishingAPI.rollChest(player));
            cir.setReturnValue(0);
            cir.cancel();
        }

        //FishingHookExt.modifyRewards(items, 0, null);
        player.level().playSound(null, player, StardewFishing.platform.getSoundEvent(Sound.pull_item), SoundSource.PLAYERS, 1.0F, 1.0F);
    }
}
