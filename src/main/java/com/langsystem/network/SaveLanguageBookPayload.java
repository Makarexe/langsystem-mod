package com.langsystem.network;

import com.langsystem.LangSystemMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Клиент -> Сервер: "запиши этот текст в языковую книгу, которую я сейчас держу". */
public record SaveLanguageBookPayload(boolean offhand, String text) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SaveLanguageBookPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(LangSystemMod.MOD_ID, "save_language_book"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SaveLanguageBookPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, SaveLanguageBookPayload::offhand,
            ByteBufCodecs.STRING_UTF8, SaveLanguageBookPayload::text,
            SaveLanguageBookPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
