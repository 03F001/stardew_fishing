package com.bonker.stardewfishing.forge;

import com.bonker.stardewfishing.FishBehavior;
import com.bonker.stardewfishing.FishBehaviorReloadListener;
import com.bonker.stardewfishing.FishingHookExt;
import com.bonker.stardewfishing.Platform;
import com.bonker.stardewfishing.OptionalLootItem;
import com.bonker.stardewfishing.Sound;
import com.bonker.stardewfishing.StardewFishing;
import com.bonker.stardewfishing.client.FishingScreen;
import com.bonker.stardewfishing.forge.compat.Aquaculture;
import com.bonker.stardewfishing.forge.compat.QualityFood;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryType;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.ItemFishedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ForgePlatform implements Platform {
    private final DeferredRegister<SoundEvent> soundsRegistry = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, StardewFishing.MODID);
    private final DeferredRegister<LootPoolEntryType> lootPoolEntryTypeRegistry = DeferredRegister.create(Registries.LOOT_POOL_ENTRY_TYPE, StardewFishing.MODID);

    private final ArrayList<RegistryObject<SoundEvent>> sounds = Arrays.stream(Sound.values())
        .map(s -> soundsRegistry.register(s.name(), () -> SoundEvent.createVariableRangeEvent(StardewFishing.mkResLoc(s.name()))))
        .collect(Collectors.toCollection(ArrayList::new));

    private final RegistryObject<LootPoolEntryType> lootPoolEntryType = lootPoolEntryTypeRegistry.register("optional",
        () -> new LootPoolEntryType(new OptionalLootItem.Serializer()));

    private static final String PROTOCOL_VERSION = "1";
    private final SimpleChannel channel;

    enum Message {
        StartMinigame,
        CompleteMinigame,
    }

    ForgeFishBehaviorReloadListener fishBehaviorReloadListener = new ForgeFishBehaviorReloadListener();

    public ForgePlatform(IEventBus bus) {
        soundsRegistry.register(bus);
        lootPoolEntryTypeRegistry.register(bus);

        channel = NetworkRegistry.ChannelBuilder
            .named(StardewFishing.mkResLoc("packets"))
            .networkProtocolVersion(() -> PROTOCOL_VERSION)
            .clientAcceptedVersions(PROTOCOL_VERSION::equals)
            .serverAcceptedVersions(PROTOCOL_VERSION::equals)
            .simpleChannel();

        channel.registerMessage(Message.StartMinigame.ordinal(),
            S2CStartMinigamePacket.class,
            S2CStartMinigamePacket::encode,
            S2CStartMinigamePacket::new,
            S2CStartMinigamePacket::handle);

        channel.registerMessage(Message.CompleteMinigame.ordinal(),
            C2SCompleteMinigamePacket.class,
            C2SCompleteMinigamePacket::encode,
            C2SCompleteMinigamePacket::decode,
            C2SCompleteMinigamePacket::handle);
    }

    static class CapProvider implements ICapabilityProvider {
        static final Capability<FishingHookExt> CAP = CapabilityManager.get(new CapabilityToken<>() {});
        static final ResourceLocation NAME = StardewFishing.mkResLoc("hook");

        private final LazyOptional<FishingHookExt> optional = LazyOptional.of(FishingHookExt::new);

        @Override
        public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
            return cap == CAP ? optional.cast() : LazyOptional.empty();
        }
    }

    @Mod.EventBusSubscriber(modid = StardewFishing.MODID)
    public static class ForgeBus {
        @SubscribeEvent
        public static void onAttachCapabilitiesPlayer(final AttachCapabilitiesEvent<Entity> event) {
            if (event.getObject() instanceof net.minecraft.world.entity.projectile.FishingHook) {
                if (!event.getObject().getCapability(CapProvider.CAP).isPresent()) {
                    event.addCapability(CapProvider.NAME, new CapProvider());
                }
            }
        }
    }

    @Override
    public FishingHookExt getFishingHookExt(FishingHook hook) {
        return hook.getCapability(CapProvider.CAP).resolve().get();
    }

    @Override
    public SoundEvent getSoundEvent(Sound sound) {
        return sounds.get(sound.ordinal()).get();
    }

    @Override
    public LootPoolEntryType getLootPoolEntryType() {
        return lootPoolEntryType.get();
    }

    @Override
    public void startMinigame(ServerPlayer player, ItemStack fish, boolean treasureChest, boolean goldenChest) {
        channel.send(PacketDistributor.PLAYER.with(() -> player),
            new S2CStartMinigamePacket(getFishBehavior(fish), fish, treasureChest, goldenChest));
    }

    @Override
    public void completeMinigame(boolean success, double accuracy, boolean gotChest) {
        channel.send(PacketDistributor.SERVER.noArg(), new C2SCompleteMinigamePacket(success, accuracy, gotChest));
    }

    @Override
    public void modifyRewards(List<ItemStack> rewards, double accuracy, @Nullable ItemStack fishingRod) {
        if (!StardewFishing.QUALITY_FOOD_INSTALLED) return;

        int quality = StardewFishing.platform.getQuality(accuracy);
        if (quality < 3 && FishingHookExt.hasBobber(fishingRod, com.bonker.stardewfishing.Item.quality_bobber)) {
            quality++;
        }
        for (ItemStack reward : rewards) {
            if (reward.is(StardewFishing.STARTS_MINIGAME)) {
                if (quality == 0 && reward.hasTag() && reward.getOrCreateTag().contains("quality_food")) {
                    if (reward.getOrCreateTag().size() > 1) {
                        reward.getOrCreateTag().remove("quality_food");
                    } else {
                        reward.setTag(null);
                    }
                } else if (quality > 0) {
                    QualityFood.applyQuality(reward, quality);
                }
            }
        }
    }

    @Override
    public boolean eventItemFished(List<ItemStack> rewards, int rodDamage, FishingHook hook) {
        return MinecraftForge.EVENT_BUS.post(new ItemFishedEvent(rewards, 1, hook));
    }

    @Override
    public boolean isFakePlayer(ServerPlayer player) {
        return player instanceof FakePlayer;
    }

    @Override
    public FishBehavior getFishBehavior(@Nullable ItemStack stack) {
        return fishBehaviorReloadListener.getBehavior(stack);
    }

    @Override
    public int getQuality(double accuracy) {
        return Config.getQuality(accuracy);
    }

    @Override
    public double getMultiplier(double accuracy, boolean qualityBobber) {
        return Config.getMultiplier(accuracy, qualityBobber);
    }

    @Override
    public double getBiteTimeMultiplier() {
        return Config.getBiteTimeMultiplier();
    }

    @Override
    public double getTreasureChestChance() {
        return Config.getTreasureChestChance();
    }

    @Override
    public double getGoldenChestChance() {
        return Config.getGoldenChestChance();
    }

    @Override
    public Item getItem(com.bonker.stardewfishing.Item item) {
        return switch (item) {
            case trap_bobber -> Items.TRAP_BOBBER.get();
            case cork_bobber -> Items.CORK_BOBBER.get();
            case sonar_bobber -> Items.SONAR_BOBBER.get();
            case treasure_bobber -> Items.TREASURE_BOBBER.get();
            case quality_bobber -> Items.QUALITY_BOBBER.get();
        };
    }

    @Override
    public Item getItem(ResourceLocation itemId) {
        return ForgeRegistries.ITEMS.getValue(itemId);
    }

    @Override
    public ItemStack getBobber(ItemStack fishingRod) {
        return Items.getBobber(fishingRod);
    }

    @Override
    public void renderTooltip(GuiGraphics pGuiGraphics, Font font, List<Component> textComponents, Optional<TooltipComponent> tooltipComponent, ItemStack stack, int mouseX, int mouseY) {
        pGuiGraphics.renderTooltip(font, textComponents, tooltipComponent, stack, mouseX, mouseY);
    }
}

record S2CStartMinigamePacket(FishBehavior behavior, ItemStack fish, boolean treasureChest, boolean goldenChest) {
    public S2CStartMinigamePacket(FriendlyByteBuf buf) {
        this(new FishBehavior(buf), buf.readItem(), buf.readBoolean(), buf.readBoolean());
    }

    public void encode(FriendlyByteBuf buf) {
        behavior.writeToBuffer(buf);
        buf.writeItem(fish);
        buf.writeBoolean(treasureChest);
        buf.writeBoolean(goldenChest);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        contextSupplier.get().enqueueWork(() -> Minecraft.getInstance().setScreen(new FishingScreen(behavior, fish, treasureChest, goldenChest)));
    }
}

record C2SCompleteMinigamePacket(boolean success, double accuracy, boolean gotChest) {
    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(success);
        if (success) buf.writeDouble(accuracy);
        buf.writeBoolean(gotChest);
    }

    public static C2SCompleteMinigamePacket decode(FriendlyByteBuf buf) {
        boolean success = buf.readBoolean();
        return new C2SCompleteMinigamePacket(success, success ? buf.readDouble() : -1, buf.readBoolean());
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        ServerPlayer player = contextSupplier.get().getSender();
        if (player == null) {
            return;
        }

        var hook = player.fishing;
        if (hook == null /*|| FishingHookExt.getStoredRewards(hook).isEmpty()*/) {
            StardewFishing.LOGGER.warn("{} tried to complete a fishing minigame that doesn't exist", player.getScoreboardName());
            return;
        }

        contextSupplier.get().enqueueWork(() -> {
            InteractionHand hand = FishingHookExt.getRodHand(player);
            if (hand == null) {
                FishingHookExt.endMinigame(player, false, 0, gotChest, null);
                StardewFishing.LOGGER.warn("{} tried to complete a fishing minigame without a fishing rod", player.getScoreboardName());
            } else {
                ItemStack fishingRod = player.getItemInHand(hand);
                FishingHookExt.endMinigame(player, success, accuracy, gotChest, fishingRod);
                fishingRod.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));

                if (StardewFishing.AQUACULTURE_INSTALLED) {
                    Aquaculture.damageEquippedBobber(fishingRod, player);
                }
            }
        });
    }
}

class ForgeFishBehaviorReloadListener extends FishBehaviorReloadListener {
    ForgeFishBehaviorReloadListener() {
        super();
    }

    @Mod.EventBusSubscriber(modid = StardewFishing.MODID)
    public static class ForgeBus {
        @SubscribeEvent
        public static void onAddReloadListeners(final AddReloadListenerEvent event) {
            event.addListener(((ForgePlatform)StardewFishing.platform).fishBehaviorReloadListener);
        }
    }

    @Override
    protected Item getItemFromRegistry(ResourceLocation loc) {
        return ForgeRegistries.ITEMS.getValue(loc);
    }
}