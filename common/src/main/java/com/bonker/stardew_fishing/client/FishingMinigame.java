package com.bonker.stardew_fishing.client;

import com.bonker.stardew_fishing.api.API;

import com.bonker.stardew_fishing.CommonPlatform;
import com.bonker.stardew_fishing.StardewFishing;
import com.bonker.stardew_fishing.Sound;
import com.bonker.stardew_fishing.FishBehavior;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

import java.util.Random;

public class FishingMinigame {
    private static final int POINTS_TO_FINISH = 120;
    private static final int TREASURE_CHEST_TIME = 30;

    private static final float UP_ACCELERATION = 0.7F;
    private static final float GRAVITY = -0.7F;
    private static final int MAX_FISH_HEIGHT = FishBehavior.MAX_HEIGHT;

    private final Random random = new Random();
    private final FishingScreen screen;
    private final FishBehavior behavior;
    private final int maxBobberHeight;

    private boolean hasCorkBobber = false;
    private boolean hasSonarBobber = false;
    private boolean hasTrapBobber = false;
    private boolean hasTreasureBobber = false;

    private double bobberPos = 0;
    private double bobberVelocity = 0;

    private double fishPos = 0;
    private double fishVelocity = 0;
    private int fishTarget = -1;
    private boolean fishIsIdle = false;
    private int fishIdleTicks = 0;

    private boolean bobberOnFish = true;
    private boolean bobberOnChest = false;
    private float points = POINTS_TO_FINISH / 5F;
    private int successTicks = 0;
    private int totalTicks = 0;

    private final API.Chest chest;
    private final int chestPos;
    private int chestAppearTime;
    private float chestTimer = 0;
    private boolean chestVisible = false;

    public FishingMinigame(FishingScreen screen, FishBehavior behavior, API.Chest chest) {
        this.screen = screen;
        this.behavior = behavior;
        this.chest = chest;
        this.chestPos = chest != API.Chest.none ? (int) (5 + 125 * random.nextFloat()) : 0;
        this.chestAppearTime = chest != API.Chest.none ? (int) (20 + 40 * random.nextFloat()) : -1;

        // set bobber flags
        Player player = Minecraft.getInstance().player;
        if (StardewFishing.BOBBER_ITEMS_REGISTERED && player != null) {
            InteractionHand hand = CommonPlatform.getRodHand(player);
            if (hand != null) {
                Item bobberItem = StardewFishing.platform.getBobber(player.getItemInHand(hand)).getItem();
                if (bobberItem == StardewFishing.platform.getItem(com.bonker.stardew_fishing.Item.cork_bobber)) {
                    hasCorkBobber = true;
                } else if (bobberItem == StardewFishing.platform.getItem(com.bonker.stardew_fishing.Item.sonar_bobber)) {
                    hasSonarBobber = true;
                } else if (bobberItem == StardewFishing.platform.getItem(com.bonker.stardew_fishing.Item.trap_bobber)) {
                    hasTrapBobber = true;
                } else if (bobberItem == StardewFishing.platform.getItem(com.bonker.stardew_fishing.Item.treasure_bobber)) {
                    hasTreasureBobber = true;
                }
            }
        }

        // set max bobber height
        maxBobberHeight = hasCorkBobber ? 96 : 106;
    }

    public void tick(boolean mouseDown) {
        // bobber movement
        if (mouseDown) {
            if (bobberVelocity < 0) {
                bobberVelocity *= 0.9;
            }
            bobberVelocity += UP_ACCELERATION;
        } else if (bobberPos > 0) {
            bobberVelocity += GRAVITY;
        }

        bobberPos += bobberVelocity;
        if (bobberPos > maxBobberHeight) {
            bobberVelocity = 0;
            bobberPos = maxBobberHeight;
        } else if (bobberPos <= 0) {
            bobberPos = 0;
            if (bobberVelocity < 2 * GRAVITY) {
                bobberVelocity *= -0.4;
            } else {
                bobberVelocity = 0;
            }
        }

        // fish movement
        if (fishTarget == -1 || behavior.shouldMoveNow(fishIdleTicks, random)) {
            fishTarget = behavior.pickNextTargetPos((int) fishPos, random);
            fishIsIdle = false;
            fishIdleTicks = 0;
        }

        if (fishIsIdle) {
            fishIdleTicks++;
            if (Math.abs(fishVelocity) > 0) {
                boolean up = fishVelocity > 0;
                fishVelocity -= (up ? behavior.upAcceleration() : behavior.downAcceleration()) * Math.signum(fishVelocity);
                if (fishVelocity == 0 || up && fishVelocity < 0 || !up && fishVelocity > 0) {
                    fishVelocity = 0;
                }
            }
        } else {
            double distanceLeft = fishTarget - fishPos;
            double acceleration = (distanceLeft > 0 ? behavior.upAcceleration() : behavior.downAcceleration()) * Math.signum(distanceLeft);
            fishVelocity = Mth.clamp(fishVelocity + acceleration, -behavior.topSpeed(), behavior.topSpeed());
        }

        fishPos += fishVelocity;
        if (Math.abs(fishTarget - fishPos) < fishVelocity) {
            fishIsIdle = true;
        } else if (fishPos > MAX_FISH_HEIGHT) {
            fishVelocity = 0;
            fishPos = MAX_FISH_HEIGHT;
            fishIsIdle = true;
        } else if (fishPos < 0) {
            fishVelocity = 0;
            fishPos = 0;
            fishIsIdle = true;
        }

        // treasure chest timer
        if (chestAppearTime > 0 && --chestAppearTime == 0) {
            chestVisible = true;
            screen.playSound(Sound.dwop);
        }

        // game logic
        int min = Mth.floor(bobberPos) - 2;
        int max = Mth.ceil(bobberPos) + (hasCorkBobber ? 34 : 24);
        boolean wasOnFish = bobberOnFish;
        boolean wasOnChest = bobberOnChest;
        bobberOnFish = fishPos >= min && fishPos <= max;
        bobberOnChest = chestVisible && chestPos >= min && chestPos <= max;

        totalTicks++;
        if (bobberOnFish || (hasTreasureBobber && bobberOnChest)) {
            successTicks++;
        }

        if (wasOnFish != bobberOnFish) {
            screen.stopReelingSounds();
            screen.playSound(Sound.dwop);
        }

        if (wasOnChest != bobberOnChest) {
            screen.playSound(Sound.dwop);
        }

        if (!bobberOnChest && chestTimer > 0 && chestTimer < TREASURE_CHEST_TIME) {
            chestTimer -= 0.25F;
        }

        if (bobberOnChest && chestTimer < TREASURE_CHEST_TIME && ++chestTimer >= TREASURE_CHEST_TIME) {
            chestVisible = false;
        }

        if (bobberOnFish) {
            points += 1;
            if (points >= POINTS_TO_FINISH) {
                screen.setResult(true, (double) successTicks / totalTicks, gotChest(), isGoldenChest());
            }
        } else if (!hasTreasureBobber || !bobberOnChest) {
            points -= hasTrapBobber ? 0.666F : 1;
            if (points <= 0) {
                screen.setResult(false, 0, false, false);
            }
        }
    }

    public float getBobberPos() {
        return (float) bobberPos;
    }

    public float getFishPos() {
        return (float) fishPos;
    }

    public boolean isBobberOnFish() {
        return bobberOnFish;
    }

    public boolean isBobberOnChest() {
        return bobberOnChest;
    }

    public float getProgress() {
        return points / POINTS_TO_FINISH;
    }

    public int getChestPos() {
        return chestPos;
    }

    public boolean isChestVisible() {
        return chestVisible;
    }

    public boolean isGoldenChest() {
        return chest == API.Chest.golden;
    }

    public float getChestProgress() {
        return chestTimer / TREASURE_CHEST_TIME;
    }

    public boolean gotChest() {
        return chestTimer >= TREASURE_CHEST_TIME;
    }

    public boolean hasCorkBobber() {
        return hasCorkBobber;
    }

    public boolean hasSonarBobber() {
        return hasSonarBobber;
    }
}
