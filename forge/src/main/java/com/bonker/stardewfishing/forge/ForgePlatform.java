package com.bonker.stardewfishing.forge;

import com.bonker.stardewfishing.client.FishingScreen;
import com.bonker.stardewfishing.common.OptionalLootItem;
import com.bonker.stardewfishing.Platform;
import com.bonker.stardewfishing.Sound;
import com.bonker.stardewfishing.StardewFishing;

import com.bonker.stardewfishing.common.networking.C2SCompleteMinigamePacket;
import com.bonker.stardewfishing.common.networking.S2CStartMinigamePacket;
import com.bonker.stardewfishing.server.FishBehaviorReloadListener;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryType;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

public class ForgePlatform implements Platform {
    private final DeferredRegister<SoundEvent> soundsRegistry = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, StardewFishing.MODID);
    private final DeferredRegister<LootPoolEntryType> lootPoolEntryTypeRegistry = DeferredRegister.create(Registries.LOOT_POOL_ENTRY_TYPE, StardewFishing.MODID);

    private final ArrayList<RegistryObject<SoundEvent>> sounds = Arrays.stream(Sound.values())
        .map(s -> soundsRegistry.register(s.name(), () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(StardewFishing.MODID, s.name()))))
        .collect(Collectors.toCollection(ArrayList::new));

    private final RegistryObject<LootPoolEntryType> lootPoolEntryType = lootPoolEntryTypeRegistry.register("optional",
            () -> new LootPoolEntryType(new OptionalLootItem.Serializer()));

    private static final String PROTOCOL_VERSION = "1";
    private SimpleChannel channel;

    enum Message {
        StartMinigame,
        CompleteMinigame,
    }

    public ForgePlatform(IEventBus bus) {
        soundsRegistry.register(bus);
        lootPoolEntryTypeRegistry.register(bus);

        channel = NetworkRegistry.ChannelBuilder
                .named(new ResourceLocation(StardewFishing.MODID, "packets"))
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
                new S2CStartMinigamePacket(FishBehaviorReloadListener.getBehavior(fish), fish, treasureChest, goldenChest));
    }

    @Override
    public void completeMinigame(boolean success, double accuracy, boolean gotChest) {
        channel.send(PacketDistributor.SERVER.noArg(), new C2SCompleteMinigamePacket(success, accuracy, gotChest));
    }
}