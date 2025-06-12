package com.bonker.stardewfishing.proxy;

import com.bonker.stardewfishing.client.FishingScreen;
import com.bonker.stardewfishing.common.networking.S2CStartMinigamePacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public class ClientProxy {
    public static void openFishingScreen(S2CStartMinigamePacket packet) {
        Minecraft.getInstance().setScreen(new FishingScreen(packet));
    }

    public static boolean isShiftDown() {
        return Screen.hasShiftDown();
    }
}
