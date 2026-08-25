package com.langsystem.network;

import com.langsystem.client.ClientLanguageState;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class ClientPacketHandler {

    public static void handleSync(SyncLanguagePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientLanguageState.update(payload.languageIds(), payload.progress(), payload.current()));
    }

    public static void handleSyncSpeakerLanguage(SyncSpeakerLanguagePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> ClientLanguageState.updateSpeakerLanguage(payload.playerId(), payload.languageId()));
    }

    private ClientPacketHandler() {
    }
}
