package com.langsystem.network;

import com.langsystem.Language;
import com.langsystem.block.LanguageSignBlockEntity;
import com.langsystem.data.LanguageData;
import com.langsystem.data.ModAttachments;
import com.langsystem.data.ModDataComponents;
import com.langsystem.item.LanguageBookItem;
import com.langsystem.util.RuneCipher;
import com.langsystem.util.SpeechFluency;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class NetworkHandler {

    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        registrar.playToServer(
                SetLanguagePayload.TYPE,
                SetLanguagePayload.STREAM_CODEC,
                NetworkHandler::handleSetLanguage
        );

        registrar.playToClient(
                SyncLanguagePayload.TYPE,
                SyncLanguagePayload.STREAM_CODEC,
                ClientPacketHandler::handleSync
        );

        registrar.playToServer(
                SaveLanguageBookPayload.TYPE,
                SaveLanguageBookPayload.STREAM_CODEC,
                NetworkHandler::handleSaveBook
        );

        registrar.playToServer(
                SaveLanguageSignPayload.TYPE,
                SaveLanguageSignPayload.STREAM_CODEC,
                NetworkHandler::handleSaveSign
        );

        registrar.playToServer(
                SaveVanillaSignPayload.TYPE,
                SaveVanillaSignPayload.STREAM_CODEC,
                NetworkHandler::handleSaveVanillaSign
        );
    }

    private static void handleSetLanguage(SetLanguagePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer serverPlayer)) {
                return;
            }
            var opt = Language.byId(payload.languageId());
            if (opt.isEmpty()) {
                return;
            }
            LanguageData data = serverPlayer.getData(ModAttachments.LANGUAGE_DATA);
            if (!data.setCurrent(opt.get())) {
                return; // игрок не знает этот язык вообще (прогресс 0) — игнорируем
            }
            serverPlayer.setData(ModAttachments.LANGUAGE_DATA, data);
            serverPlayer.displayClientMessage(
                    Component.literal("Текущий язык: " + opt.get().displayName())
                            .withStyle(style -> style.withColor(opt.get().color())),
                    true
            );
            sendSync(serverPlayer);
        });
    }

    private static void handleSaveBook(SaveLanguageBookPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer serverPlayer)) {
                return;
            }
            InteractionHand hand = payload.offhand() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
            ItemStack stack = serverPlayer.getItemInHand(hand);
            if (!(stack.getItem() instanceof LanguageBookItem)) {
                return;
            }
            if (stack.has(ModDataComponents.LANGUAGE_BOOK_TEXT.get())) {
                return; // уже подписана — повторно не переписываем
            }

            LanguageData data = serverPlayer.getData(ModAttachments.LANGUAGE_DATA);
            Language language = data.current();
            int progress = data.progress(language);
            if (progress < SpeechFluency.CANNOT_SPEAK_BELOW) {
                serverPlayer.displayClientMessage(Component.literal(
                                "[Языки] Вы знаете \"" + language.displayName()
                                        + "\" слишком слабо, чтобы связно записать текст на нём (нужно минимум "
                                        + SpeechFluency.CANNOT_SPEAK_BELOW + "%).")
                                .withStyle(style -> style.withColor(0xFF5555)),
                        true);
                return;
            }

            stack.set(ModDataComponents.LANGUAGE_BOOK_TEXT.get(),
                    new ModDataComponents.LanguageText(language.id(), payload.text(), progress));
        });
    }

    private static void handleSaveSign(SaveLanguageSignPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer serverPlayer)) {
                return;
            }
            if (serverPlayer.blockPosition().distSqr(payload.pos()) > 64.0 * 64.0) {
                return; // слишком далеко — подозрительно, игнорируем
            }
            if (!(serverPlayer.level().getBlockEntity(payload.pos()) instanceof LanguageSignBlockEntity sign)) {
                return;
            }
            if (sign.content(payload.isFrontText()) != null) {
                return; // эта сторона уже подписана
            }

            LanguageData data = serverPlayer.getData(ModAttachments.LANGUAGE_DATA);
            Language language = data.current();
            int progress = data.progress(language);
            if (progress < SpeechFluency.CANNOT_SPEAK_BELOW) {
                serverPlayer.displayClientMessage(Component.literal(
                                "[Языки] Вы знаете \"" + language.displayName()
                                        + "\" слишком слабо, чтобы связно записать текст на нём (нужно минимум "
                                        + SpeechFluency.CANNOT_SPEAK_BELOW + "%).")
                                .withStyle(style -> style.withColor(0xFF5555)),
                        true);
                return;
            }

            sign.setContent(payload.isFrontText(),
                    new ModDataComponents.LanguageText(language.id(), payload.text(), progress));
        });
    }

    private static void handleSaveVanillaSign(SaveVanillaSignPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer serverPlayer)) {
                return;
            }
            if (serverPlayer.blockPosition().distSqr(payload.pos()) > 64.0 * 64.0) {
                return; // слишком далеко — подозрительно, игнорируем
            }
            if (!(serverPlayer.level().getBlockEntity(payload.pos()) instanceof SignBlockEntity sign)) {
                return;
            }
            var existingTwoSided = sign.getData(ModAttachments.VANILLA_SIGN_TEXT.get());
            if (existingTwoSided.side(payload.isFrontText()) != null) {
                return; // эта сторона уже подписана через языковую систему
            }

            LanguageData data = serverPlayer.getData(ModAttachments.LANGUAGE_DATA);
            Language language = data.current();
            int progress = data.progress(language);
            if (progress < SpeechFluency.CANNOT_SPEAK_BELOW) {
                serverPlayer.displayClientMessage(Component.literal(
                                "[Языки] Вы знаете \"" + language.displayName()
                                        + "\" слишком слабо, чтобы связно записать текст на нём (нужно минимум "
                                        + SpeechFluency.CANNOT_SPEAK_BELOW + "%).")
                                .withStyle(style -> style.withColor(0xFF5555)),
                        true);
                return;
            }

            ModDataComponents.LanguageText newSide =
                    new ModDataComponents.LanguageText(language.id(), payload.text(), progress);
            sign.setData(ModAttachments.VANILLA_SIGN_TEXT.get(), existingTwoSided.withSide(payload.isFrontText(), newSide));

            // Настоящий (ванильный) SignText оставляем непустым — заполняем "фоновым"
            // закодированным видом. Это не для чтения (наш рендер его перекрывает), а
            // чтобы удовлетворить SignApplicator.canApplyToSign() -> SignText.hasMessage():
            // без этого краситель/светящиеся чернила вообще не дают себя применить —
            // ванильная проверка требует хотя бы одну непустую строку ДО применения.
            String ambient = RuneCipher.produce(payload.text(), language, 0);
            String[] ambientLines = ambient.split("\n", -1);
            sign.updateText(oldText -> {
                SignText updated = oldText;
                for (int i = 0; i < 4; i++) {
                    String line = i < ambientLines.length ? ambientLines[i] : "";
                    updated = updated.setMessage(i, Component.literal(line));
                }
                return updated;
            }, payload.isFrontText());

            sign.setChanged();
            serverPlayer.level().sendBlockUpdated(payload.pos(), sign.getBlockState(), sign.getBlockState(), 3);
        });
    }

    /** Отправить игроку актуальные данные о его языках (вызывать при входе и после give/take). */
    public static void sendSync(ServerPlayer player) {
        LanguageData data = player.getData(ModAttachments.LANGUAGE_DATA);
        List<String> ids = new ArrayList<>();
        List<Integer> percents = new ArrayList<>();
        for (Map.Entry<Language, Integer> entry : data.allProgress().entrySet()) {
            ids.add(entry.getKey().id());
            percents.add(entry.getValue());
        }
        PacketDistributor.sendToPlayer(player, new SyncLanguagePayload(ids, percents, data.current().id()));
    }

    private NetworkHandler() {
    }
}
