package com.bonker.stardewfishing;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public abstract class FishBehaviorReloadListener extends SimplePreparableReloadListener<Map<String, JsonObject>> {
    private static final Gson GSON_INSTANCE = new Gson();
    private static final ResourceLocation LOCATION = StardewFishing.mkResLoc("data.json");
    private static FishBehaviorReloadListener INSTANCE;

    private final Map<net.minecraft.world.item.Item, FishBehavior> fishBehaviors = new HashMap<>();
    private FishBehavior defaultBehavior;

    protected FishBehaviorReloadListener() {
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

    protected abstract Item getItemFromRegistry(ResourceLocation loc);

    @Override
    protected void apply(Map<String, JsonObject> jsonObjects, ResourceManager pResourceManager, ProfilerFiller pProfiler) {
        for (Map.Entry<String, JsonObject> entry : jsonObjects.entrySet()) {
            FishBehaviorList.CODEC.parse(JsonOps.INSTANCE, entry.getValue())
                .resultOrPartial(errorMsg -> StardewFishing.LOGGER.warn("Failed to decode fish behavior list {} in data pack {} - {}", LOCATION, entry.getKey(), errorMsg))
                .ifPresent(behaviorList -> {
                    behaviorList.behaviors.forEach((loc, fishBehavior) -> {
                        Item item = getItemFromRegistry(loc);
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

    private record FishBehaviorList(boolean replace, Map<ResourceLocation, FishBehavior> behaviors, Optional<FishBehavior> defaultBehavior) {
        private static final Codec<FishBehaviorList> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.BOOL.optionalFieldOf("replace", false).forGetter(FishBehaviorList::replace),
            Codec.unboundedMap(ResourceLocation.CODEC, FishBehavior.CODEC).fieldOf("behaviors").forGetter(FishBehaviorList::behaviors),
            FishBehavior.CODEC.optionalFieldOf("defaultBehavior").forGetter(FishBehaviorList::defaultBehavior)
        ).apply(inst, FishBehaviorList::new));
    }

    public FishBehavior getBehavior(@Nullable ItemStack stack) {
        if (stack == null) return defaultBehavior;
        return fishBehaviors.getOrDefault(stack.getItem(), defaultBehavior);
    }
}