package com.bonker.stardewfishing.fabric;

import com.bonker.stardewfishing.FishBehavior;
import com.bonker.stardewfishing.FishBehaviorReloadListener;
import com.bonker.stardewfishing.Platform;
import com.bonker.stardewfishing.Sound;
import com.bonker.stardewfishing.StardewFishing;
import com.bonker.stardewfishing.client.FishingScreen;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.registry.FabricRegistryBuilder;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryType;

import io.netty.buffer.Unpooled;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class FabricPlatform implements Platform {
    private static Config config;

    private final ArrayList<SoundEvent> sounds = Arrays.stream(Sound.values())
        .map(s -> SoundEvent.createVariableRangeEvent(new ResourceLocation(StardewFishing.MODID, s.name())))
        .collect(Collectors.toCollection(ArrayList::new));

    private final LootPoolEntryType lootPoolEntryType = Registry.register(
        BuiltInRegistries.LOOT_POOL_ENTRY_TYPE,
        new ResourceLocation(StardewFishing.MODID, "optional"),
        new LootPoolEntryType(new OptionalLootItem.Serializer())
    );

    private static final ResourceLocation START_MINIGAME_PACKET_ID = new ResourceLocation(StardewFishing.MODID, "start_minigame");
    private static final ResourceLocation COMPLETE_MINIGAME_PACKET_ID = new ResourceLocation(StardewFishing.MODID, "complete_minigame");

    FabricFishBehaviorReloadListener fishBehaviorReloadListener = new FabricFishBehaviorReloadListener();

    public FabricPlatform() {
        AutoConfig.register(Config.class, GsonConfigSerializer::new);
        config = AutoConfig.getConfigHolder(Config.class).getConfig();

        ClientPlayNetworking.registerGlobalReceiver(
            START_MINIGAME_PACKET_ID,
            (client, handler, buf, responseSender) -> {
                var packet = new S2CStartMinigamePacket(buf);
                client.execute(() -> Minecraft.getInstance().setScreen(new FishingScreen(packet.behavior(), packet.fish(), packet.treasureChest(), packet.goldenChest())));
            });

        ServerPlayNetworking.registerGlobalReceiver(
            COMPLETE_MINIGAME_PACKET_ID,
            (server, player, handler, buf, responseSender) -> {
                var packet = new C2SCompleteMinigamePacket(buf);
                var hook = player.fishing;
                server.execute(() -> packet.handle(player));
            });

        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(fishBehaviorReloadListener);

        var registrySoundEvents = FabricRegistryBuilder.createSimple(ResourceKey.createRegistryKey(new ResourceLocation(StardewFishing.MODID, "sound_events"))).buildAndRegister();
        for (Sound s : Sound.values()) {
            Registry.register(registrySoundEvents, new ResourceLocation(StardewFishing.MODID, s.name()), sounds.get(s.ordinal()));
        }

        // todo: lootpool
    }

    @Override
    public SoundEvent getSoundEvent(Sound sound) { return sounds.get(sound.ordinal()); }

    @Override
    public LootPoolEntryType getLootPoolEntryType() { return null; }

    @Override
    public void startMinigame(ServerPlayer player, ItemStack fish, boolean treasureChest, boolean goldenChest) {
        var packet = new S2CStartMinigamePacket(getFishBehavior(fish), fish, treasureChest, goldenChest);
        var buf = new FriendlyByteBuf(Unpooled.buffer());
        packet.encode(buf);
        ServerPlayNetworking.send(player, START_MINIGAME_PACKET_ID, buf);
    }

    @Override
    public void completeMinigame(boolean success, double accuracy, boolean gotChest) {
        var packet = new C2SCompleteMinigamePacket(success, accuracy, gotChest);
        var buf = new FriendlyByteBuf(Unpooled.buffer());
        packet.encode(buf);
        ClientPlayNetworking.send(COMPLETE_MINIGAME_PACKET_ID, buf);
    }

    @Override
    public FishBehavior getFishBehavior(@Nullable ItemStack stack) {
        return fishBehaviorReloadListener.getBehavior(stack);
    }

    @Override
    public int getQuality(double accuracy) {
        return config.getQuality(accuracy);
    }

    @Override
    public double getMultiplier(double accuracy, boolean qualityBobber) {
        return config.getMultiplier(accuracy, qualityBobber);
    }

    @Override
    public double getBiteTimeMultiplier() {
        return config.getBiteTimeMultiplier();
    }

    @Override
    public double getTreasureChestChance() {
        return config.getTreasureChestChance();
    }

    @Override
    public double getGoldenChestChance() {
        return config.getGoldenChestChance();
    }

    @Override
    public net.minecraft.world.item.Item getItem(com.bonker.stardewfishing.Item item) {
        return switch (item) {
            case trap_bobber -> Items.TRAP_BOBBER;
            case cork_bobber -> Items.CORK_BOBBER;
            case sonar_bobber -> Items.SONAR_BOBBER;
            case treasure_bobber -> Items.TREASURE_BOBBER;
            case quality_bobber -> Items.QUALITY_BOBBER;
        };
    }

    @Override
    public ItemStack getBobber(ItemStack fishingRod) {
        return Items.getBobber(fishingRod);
    }

    @Override
    public void renderTooltip(GuiGraphics pGuiGraphics, Font font, List<Component> textComponents, Optional<TooltipComponent> tooltipComponent, ItemStack stack, int mouseX, int mouseY) {
        pGuiGraphics.renderTooltip(font, textComponents, tooltipComponent, mouseX, mouseY);
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

    public void handle() {
        Minecraft.getInstance().setScreen(new FishingScreen(behavior, fish, treasureChest, goldenChest));
    }
}

record C2SCompleteMinigamePacket(boolean success, double accuracy, boolean gotChest) {
    private C2SCompleteMinigamePacket(boolean success, FriendlyByteBuf buf) {
        this(success, success ? buf.readDouble() : -1, buf.readBoolean());
    }

    public C2SCompleteMinigamePacket(FriendlyByteBuf buf) {
        this(buf.readBoolean(), buf);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(success);
        if (success) buf.writeDouble(accuracy);
        buf.writeBoolean(gotChest);
    }

    public void handle(ServerPlayer player) {
        if (player == null) {
            return;
        }

        var hook = player.fishing;
        if (hook == null || FishingHook.getStoredRewards(hook).isEmpty()) {
            StardewFishing.LOGGER.warn("{} tried to complete a fishing minigame that doesn't exist", player.getScoreboardName());
            return;
        }

        InteractionHand hand = com.bonker.stardewfishing.FishingHook.getRodHand(player);
        if (hand == null) {
            FishingHook.endMinigame(player, false, 0, gotChest, null);
            StardewFishing.LOGGER.warn("{} tried to complete a fishing minigame without a fishing rod", player.getScoreboardName());
        } else {
            ItemStack fishingRod = player.getItemInHand(hand);
            FishingHook.endMinigame(player, success, accuracy, gotChest, fishingRod);
            fishingRod.hurtAndBreak(1, player, p -> p.broadcastBreakEvent(hand));

//            if (StardewFishing.AQUACULTURE_INSTALLED) {
//                AquacultureProxy.damageEquippedBobber(fishingRod, player);
//            }
        }
    }
}

class FabricFishBehaviorReloadListener extends FishBehaviorReloadListener implements IdentifiableResourceReloadListener {
    FabricFishBehaviorReloadListener() {
        super();
    }

    @Override
    public ResourceLocation getFabricId() {
        return new ResourceLocation(StardewFishing.MODID, "fish_behavior_reload");
    }

    @Override
    protected Item getItemFromRegistry(ResourceLocation loc) {
        return BuiltInRegistries.ITEM.get(loc);
    }
}