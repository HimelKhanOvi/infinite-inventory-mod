package com.robsterad.infiniteinv;

import com.robsterad.infiniteinv.gui.InfiniteInventoryOverlay;
import com.robsterad.infiniteinv.network.SyncInventoryPayload;
import com.robsterad.infiniteinv.network.SyncUiPrefsPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public class InfiniteInvClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        InfiniteInventoryOverlay.register();

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
