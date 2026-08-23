package com.langsystem.network;

import com.langsystem.LangSystemMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Сервер -> Клиент: языки игрока и прогресс их изучения (0-100, параллельно с ids),
 * плюс текущий выбранный язык. Используется, чтобы отрисовать GUI выбора языка.
 */
public record SyncLanguagePayload(List<String> languageIds, List<Integer> progress, String current) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncLanguagePayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(LangSystemMod.MOD_ID, "sync_language"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncLanguagePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()), SyncLanguagePayload::languageIds,
            ByteBufCodecs.INT.apply(ByteBufCodecs.list()), SyncLanguagePayload::progress,
            ByteBufCodecs.STRING_UTF8, SyncLanguagePayload::current,
            SyncLanguagePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
