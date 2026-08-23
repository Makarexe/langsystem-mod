package com.langsystem.network;

import com.langsystem.LangSystemMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Клиент -> Сервер: "переключи мой текущий язык на этот" (выбрано в GUI). */
public record SetLanguagePayload(String languageId) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SetLanguagePayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(LangSystemMod.MOD_ID, "set_language"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SetLanguagePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, SetLanguagePayload::languageId,
            SetLanguagePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
