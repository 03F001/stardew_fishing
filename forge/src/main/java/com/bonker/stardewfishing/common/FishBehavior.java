package com.bonker.stardewfishing.common;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.Mth;

import java.util.Random;

public record FishBehavior(int idleTime, float topSpeed, float upAcceleration, float downAcceleration, int avgDistance, int moveVariation) {
    public static final Codec<FishBehavior> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.INT.fieldOf("idle_time").forGetter(FishBehavior::idleTime),
            Codec.FLOAT.fieldOf("top_speed").forGetter(FishBehavior::topSpeed),
            Codec.FLOAT.fieldOf("up_acceleration").forGetter(FishBehavior::upAcceleration),
            Codec.FLOAT.fieldOf("down_acceleration").forGetter(FishBehavior::downAcceleration),
            Codec.INT.fieldOf("avg_distance").forGetter(FishBehavior::avgDistance),
            Codec.INT.fieldOf("move_variation").forGetter(FishBehavior::moveVariation)
    ).apply(inst, FishBehavior::new));

    public static final int MAX_HEIGHT = 127;

    public FishBehavior(FriendlyByteBuf buf) {
        this(buf.readVarInt(), buf.readFloat(), buf.readFloat(), buf.readFloat(), buf.readVarInt(), buf.readVarInt());
    }

    public void writeToBuffer(FriendlyByteBuf buf) {
        buf.writeVarInt(idleTime);
        buf.writeFloat(topSpeed);
        buf.writeFloat(upAcceleration);
        buf.writeFloat(downAcceleration);
        buf.writeVarInt(avgDistance);
        buf.writeVarInt(moveVariation);
    }

    public boolean shouldMoveNow(int idleTicks, Random random) {
        if (idleTime == 0) return true;
        if (idleTime == 1) return idleTicks == 1;

        int variation = idleTicks / 2;
        float chancePerTick = 1F / variation;

        if (idleTicks >= idleTime - variation) {
            return random.nextFloat() <= chancePerTick;
        }

        return false;
    }

    public int pickNextTargetPos(int oldPos, Random random) {
        int shortestDistance = avgDistance - moveVariation;
        int longestDistance = avgDistance + moveVariation;

        int downLowerLimit = oldPos - shortestDistance;
        int upLowerLimit = oldPos + shortestDistance;

        boolean canGoDown = downLowerLimit >= 0;
        boolean canGoUp = upLowerLimit <= MAX_HEIGHT;

        boolean goingUp;
        if (canGoUp && canGoDown) {
            goingUp = random.nextBoolean();
        } else {
            goingUp = canGoUp;
        }

        int distance = random.nextInt(shortestDistance, longestDistance + 1);

        return Mth.clamp(oldPos + distance * (goingUp ? 1 : -1), 0, MAX_HEIGHT);
    }
}
