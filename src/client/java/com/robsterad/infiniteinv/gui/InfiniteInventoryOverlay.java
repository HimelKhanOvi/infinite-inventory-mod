package com.robsterad.infiniteinv;

import com.mojang.blaze3d.platform.InputConstants;
import com.robsterad.infiniteinv.config.InfiniteInvConfig;
import com.robsterad.infiniteinv.gui.InfiniteInventoryOverlay;
import com.robsterad.infiniteinv.network.SyncInventoryPayload;
import com.robsterad.infiniteinv.network.SyncUiPrefsPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

@Environment(EnvType.CLIENT)
public class InfiniteInvClient implements ClientModInitializer {
    public static KeyMapping toggleKey;

    @Override
    public void onInitializeClient() {
        InfiniteInvConfig.load();
        
        // অত্যন্ত গুরুত্বপূর্ণ: ওভারলে এবং ইভেন্ট রেজিস্টার করার জন্য এটি থাকতেই হবে
        InfiniteInventoryOverlay.register();

        toggleKey = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.infiniteinv.toggle",
                InputConstants.Type.KEYSYM,
                InputConstants.KEY_I,
                "category.infiniteinv"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (toggleKey.consumeClick()) {
                if (Minecraft.getInstance().player != null) {
                    InfiniteInventoryOverlay.panelVisible = !InfiniteInventoryOverlay.panelVisible;
                }
            }
        });

        ClientPlayNetworking.registerGlobalReceiver(SyncInventoryPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                InfiniteInventoryOverlay.cachedItems = payload.items();
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(SyncUiPrefsPayload.ID, (payload, context) -> {
            context.client().execute(() -> {
                InfiniteInventoryOverlay.applyUiPrefs(payload.panelVisible(), payload.sortMode(), payload.showTooltips());
            });
        });
    }
}
