package com.langsystem.network;

import com.langsystem.LangSystemMod;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

/**
 * Сервер -> Клиент: "этот игрок сейчас говорит на этом языке". Рассылается всем при
 * входе в игру и при каждой смене текущего языка (см. {@code NetworkHandler}) — нужно
 * клиентам не для чата (там всё считает сервер), а для приглушения голоса в Simple
 * Voice Chat ({@code voicechat.LangSystemVoicechatPlugin}): чтобы применить фильтр к
 * чужому голосу, клиент слушателя должен знать, на каком языке говорит именно ЭТОТ
 * собеседник, и сверить это со своим собственным прогрессом в этом языке.
 */
public record SyncSpeakerLanguagePayload(UUID playerId, String languageId) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncSpeakerLanguagePayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(LangSystemMod.MOD_ID, "sync_speaker_language"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SyncSpeakerLanguagePayload> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, SyncSpeakerLanguagePayload::playerId,
            ByteBufCodecs.STRING_UTF8, SyncSpeakerLanguagePayload::languageId,
            SyncSpeakerLanguagePayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
