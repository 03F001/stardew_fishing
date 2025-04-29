package com.bonker.stardewfishing.server;

import com.bonker.stardewfishing.StardewFishing;
import com.bonker.stardewfishing.common.FishBehavior;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

import javax.annotation.Nullable;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class FishBehaviorReloadListener extends SimplePreparableReloadListener<Map<String, JsonObject>> {
    private static final Gson GSON_INSTANCE = new Gson();
    private static final ResourceLocation LOCATION = new ResourceLocation(StardewFishing.MODID, "data.json");
    private static FishBehaviorReloadListener INSTANCE;

    private final Map<Item, FishBehavior> fishBehaviors = new HashMap<>();
    private FishBehavior defaultBehavior;

    private FishBehaviorReloadListener() {
        super();
    }

    @Override
    protected Map<String, JsonObject> prepare(ResourceManager pResourceManager, ProfilerFiller pProfiler) {
        Map<String, JsonObject> objects = new HashMap<>();
        for (Resource resource : pResourceManager.getResourceStack(LOCATION)) {
            try (InputStream inputstream = resource.open();
                 Reader reader = new BufferedReader(new InputStreamReader(inputstream, StandardCharsets.UTF_8));
            ) {
                objects.put(resource.sourcePackId(), GsonHelper.fromJson(GSON_INSTANCE, reader, JsonObject.class));
            } catch (RuntimeException | IOException exception) {
                StardewFishing.LOGGER.error("Invalid json in fish behavior list {} in data pack {}", LOCATION, resource.sourcePackId(), exception);
            }
        }
        return objects;
    }

    @Override
    protected void apply(Map<String, JsonObject> jsonObjects, ResourceManager pResourceManager, ProfilerFiller pProfiler) {
        for (Map.Entry<String, JsonObject> entry : jsonObjects.entrySet()) {
            FishBehaviorList.CODEC.parse(JsonOps.INSTANCE, entry.getValue())
                    .resultOrPartial(errorMsg -> StardewFishing.LOGGER.warn("Failed to decode fish behavior list {} in data pack {} - {}", LOCATION, entry.getKey(), errorMsg))
                    .ifPresent(behaviorList -> {
                        behaviorList.behaviors.forEach((loc, fishBehavior) -> {
                            Item item = ForgeRegistries.ITEMS.getValue(loc);
                            if (item != null) {
                                if (behaviorList.replace || !fishBehaviors.containsKey(item)) {
                                    fishBehaviors.put(item, fishBehavior);
                                }

                                behaviorList.defaultBehavior.ifPresent(behavior -> defaultBehavior = behavior);
                            }
                        });
                    });
        }
    }

    public static FishBehaviorReloadListener create() {
        INSTANCE = new FishBehaviorReloadListener();
        return INSTANCE;
    }

    public static FishBehavior getBehavior(@Nullable ItemStack stack) {
        if (stack == null) return INSTANCE.defaultBehavior;
        return INSTANCE.fishBehaviors.getOrDefault(stack.getItem(), INSTANCE.defaultBehavior);
    }

    private record FishBehaviorList(boolean replace, Map<ResourceLocation, FishBehavior> behaviors, Optional<FishBehavior> defaultBehavior) {
        private static final Codec<FishBehaviorList> CODEC = RecordCodecBuilder.create(inst -> inst.group(
                Codec.BOOL.optionalFieldOf("replace", false).forGetter(FishBehaviorList::replace),
                Codec.unboundedMap(ResourceLocation.CODEC, FishBehavior.CODEC).fieldOf("behaviors").forGetter(FishBehaviorList::behaviors),
                FishBehavior.CODEC.optionalFieldOf("defaultBehavior").forGetter(FishBehaviorList::defaultBehavior)
        ).apply(inst, FishBehaviorList::new));
    }
}
