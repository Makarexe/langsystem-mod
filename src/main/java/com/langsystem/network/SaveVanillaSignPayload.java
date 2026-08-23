package com.langsystem.network;

import com.langsystem.LangSystemMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Клиент -> Сервер: "запиши этот текст на такой-то стороне ОБЫЧНОЙ ванильной таблички"
 * (перехват {@code LocalPlayer.openTextEdit}, см. {@code mixin.LocalPlayerMixin}).
 * Аналог {@link SaveLanguageSignPayload}, но для чужого блока — данные хранятся не в
 * своём блок-энтити, а в attachment'е поверх ванильного {@code SignBlockEntity}.
 */
public record SaveVanillaSignPayload(BlockPos pos, boolean isFrontText, String text) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SaveVanillaSignPayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(LangSystemMod.MOD_ID, "save_vanilla_sign"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SaveVanillaSignPayload> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, SaveVanillaSignPayload::pos,
            ByteBufCodecs.BOOL, SaveVanillaSignPayload::isFrontText,
            ByteBufCodecs.STRING_UTF8, SaveVanillaSignPayload::text,
            SaveVanillaSignPayload::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
