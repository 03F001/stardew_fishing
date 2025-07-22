package com.bonker.stardew_fishing.fabric;

import com.bonker.stardew_fishing.api.API;
import com.bonker.stardew_fishing.api.StardewFishingAPI;

import com.bonker.stardew_fishing.CommonPlatform;
import com.bonker.stardew_fishing.FishBehavior;
import com.bonker.stardew_fishing.FishBehaviorReloadListener;
import com.bonker.stardew_fishing.OptionalLootItem;
import com.bonker.stardew_fishing.Sound;
import com.bonker.stardew_fishing.StardewFishing;
import com.bonker.stardew_fishing.client.FishingScreen;
import com.bonker.stardew_fishing.fabric.mixin.FishingHookAccessor;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.GsonConfigSerializer;

import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryType;

import io.netty.buffer.Unpooled;

import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class FabricPlatform extends CommonPlatform {
    private static Config config;

    private final ArrayList<SoundEvent> sounds = Arrays.stream(Sound.values())
        .map(s -> SoundEvent.createVariableRangeEvent(StardewFishing.mkResLoc(s.name())))
        .collect(Collectors.toCollection(ArrayList::new));

    private final LootPoolEntryType lootPoolEntryType = Registry.register(
        BuiltInRegistries.LOOT_POOL_ENTRY_TYPE,
        StardewFishing.mkResLoc("optional"),
        new LootPoolEntryType(new OptionalLootItem.Serializer())
    );

    private static final ResourceLocation START_MINIGAME_PACKET_ID = StardewFishing.mkResLoc("start_minigame");
    private static final ResourceLocation COMPLETE_MINIGAME_PACKET_ID = StardewFishing.mkResLoc("complete_minigame");

    FabricFishBehaviorReloadListener fishBehaviorReloadListener = new FabricFishBehaviorReloadListener();

    public static final AttachmentType<FishingHookExt> HOOK = AttachmentRegistry.createDefaulted(StardewFishing.mkResLoc("hook"), () -> new FishingHookExt());

    public FabricPlatform() {
        super(evt -> {
            LootParams lootParams = (new LootParams.Builder((ServerLevel) evt.hook.level()))
                .withParameter(LootContextParams.ORIGIN, evt.hook.position())
                .withParameter(LootContextParams.TOOL, evt.rod)
                .withParameter(LootContextParams.THIS_ENTITY, evt.hook)
                .withLuck((float)((FishingHookAccessor)evt.hook).getLuck() + evt.player.getLuck())
                .create(LootContextParamSets.FISHING);
            LootTable lootTable = evt.hook.level().getServer().getLootData().getLootTable(BuiltInLootTables.FISHING);
            lootTable.getRandomItems(lootParams, evt.inout_loot::add);

            for (var item : evt.inout_loot) {
                if (StardewFishingAPI.isStartMinigame(item)) {
                    evt.inout_fish = item;
                    evt.inout_chest = StardewFishingAPI.rollChest(evt.player);
                    break;
                }
            }
        });

        AutoConfig.register(Config.class, GsonConfigSerializer::new);
        config = AutoConfig.getConfigHolder(Config.class).getConfig();

        ClientPlayNetworking.registerGlobalReceiver(
            START_MINIGAME_PACKET_ID,
            (client, handler, buf, responseSender) -> {
                var packet = new S2CStartMinigamePacket(buf);
                client.execute(() -> Minecraft.getInstance().setScreen(new FishingScreen(packet.behavior(), packet.fish(), packet.chest())));
            });

        ServerPlayNetworking.registerGlobalReceiver(
            COMPLETE_MINIGAME_PACKET_ID,
            (server, player, handler, buf, responseSender) -> {
                var packet = new C2SCompleteMinigamePacket(buf);
                var hook = player.fishing;
                server.execute(() -> packet.handle(player));
            });

        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(fishBehaviorReloadListener);

        var registrySoundEvents = FabricRegistryBuilder.createSimple(ResourceKey.createRegistryKey(StardewFishing.mkResLoc("sound_events"))).buildAndRegister();
        for (Sound s : Sound.values()) {
            Registry.register(registrySoundEvents, StardewFishing.mkResLoc(s.name()), sounds.get(s.ordinal()));
        }
    }

    @Override
    public void startMinigame(ServerPlayer player, ItemStack fish, API.Chest chest) {
        var packet = new S2CStartMinigamePacket(getFishBehavior(fish), fish, chest);
        var buf = new FriendlyByteBuf(Unpooled.buffer());
        packet.encode(buf);
        ServerPlayNetworking.send(player, START_MINIGAME_PACKET_ID, buf);
    }

    @Override
    public FishingHookExt getFishingHookExt(net.minecraft.world.entity.projectile.FishingHook hook) {
        return hook.getAttachedOrCreate(HOOK);
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
    public SoundEvent getSoundEvent(Sound sound) {
        return sounds.get(sound.ordinal());
    }

    @Override
    public LootPoolEntryType getLootPoolEntryType() {
        return lootPoolEntryType;
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
    public Item getItem(com.bonker.stardew_fishing.Item item) {
        return switch (item) {
            case trap_bobber -> Items.TRAP_BOBBER;
            case cork_bobber -> Items.CORK_BOBBER;
            case sonar_bobber -> Items.SONAR_BOBBER;
            case treasure_bobber -> Items.TREASURE_BOBBER;
            case quality_bobber -> Items.QUALITY_BOBBER;
        };
    }

    @Override
    public Item getItem(ResourceLocation itemId) {
        return BuiltInRegistries.ITEM.get(itemId);
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

record S2CStartMinigamePacket(FishBehavior behavior, ItemStack fish, API.Chest chest) {
    public S2CStartMinigamePacket(FriendlyByteBuf buf) {
        this(new FishBehavior(buf), buf.readItem(), API.Chest.values()[buf.readByte()]);
    }

    public void encode(FriendlyByteBuf buf) {
        behavior.writeToBuffer(buf);
        buf.writeItem(fish);
        buf.writeByte(chest.ordinal());
    }

    public void handle() {
        Minecraft.getInstance().setScreen(new FishingScreen(behavior, fish, chest));
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
        if (hook == null || StardewFishingAPI.getFishingHookExt(hook).rewards.isEmpty()) {
            StardewFishing.LOGGER.warn("{} tried to complete a fishing minigame that doesn't exist", player.getScoreboardName());
            return;
        }

        InteractionHand hand = FabricPlatform.getRodHand(player);
        if (hand == null) {
            StardewFishingAPI.endMinigame(player, false, 0, gotChest);
            StardewFishing.LOGGER.warn("{} tried to complete a fishing minigame without a fishing rod", player.getScoreboardName());
        } else {
            ItemStack fishingRod = player.getItemInHand(hand);
            StardewFishingAPI.endMinigame(player, success, accuracy, gotChest);
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
        return StardewFishing.mkResLoc("fish_behavior_reload");
    }

    @Override
    protected Item getItemFromRegistry(ResourceLocation loc) {
        return BuiltInRegistries.ITEM.get(loc);
    }
}