package com.langsystem.network;

import com.langsystem.LangSystemMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Клиент -> Сервер: "запиши этот текст на такой-то стороне таблички по этим координатам". */
public record SaveLanguageSignPayload(BlockPos pos, boolean isFrontText, String text) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SaveLanguageSignPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(LangSystemMod.MOD_ID, "save_language_sign"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SaveLanguageSignPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, SaveLanguageSignPayload::pos,
            ByteBufCodecs.BOOL, SaveLanguageSignPayload::isFrontText,
            ByteBufCodecs.STRING_UTF8, SaveLanguageSignPayload::text,
            SaveLanguageSignPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
