package com.langsystem.network;

import com.langsystem.Language;
import com.langsystem.data.LanguageData;
import com.langsystem.data.ModAttachments;
import com.langsystem.data.ModDataComponents;
import com.langsystem.item.LanguageBookItem;
import com.langsystem.util.RuneCipher;
import com.langsystem.util.SpeechFluency;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.ItemLore;
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

        registrar.playToClient(
                SyncSpeakerLanguagePayload.TYPE,
                SyncSpeakerLanguagePayload.STREAM_CODEC,
                ClientPacketHandler::handleSyncSpeakerLanguage
        );

        registrar.playToServer(
                SaveLanguageBookPayload.TYPE,
                SaveLanguageBookPayload.STREAM_CODEC,
                NetworkHandler::handleSaveBook
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
            broadcastSpeakerLanguage(serverPlayer);
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
            stack.set(DataComponents.LORE, new ItemLore(List.of(
                    Component.literal(language.displayName()).withStyle(style -> style.withColor(language.color())))));
            // ITEM_NAME, а не CUSTOM_NAME — у последнего ItemStack.getTooltipLines()
            // безусловно добавляет курсив, как только компонент вообще присутствует
            // (проверено по декомпилированным исходникам), и убрать его стилем самого
            // компонента нельзя — стиль переопределяется уже после. ITEM_NAME меняет
            // отображаемое имя точно так же, но под эту принудительную стилизацию не
            // подпадает.
            stack.set(DataComponents.ITEM_NAME, Component.literal("Исписанная бумага"));
            // Переключает модель предмета на language_book_written через overrides в
            // language_book.json (custom_model_data == 1) — та же "бумага", но с парой
            // тёмных пикселей, похожих на текст, вместо чистого листа.
            stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(1));
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

            // Ванильный SignBlock.useWithoutItem() открывает openTextEdit только если
            // otherPlayerIsEditingSign() ложно, а тот блокируется, пока playerWhoMayEdit не
            // сброшен в null — обычно это делает сам ванильный обработчик пакета сохранения
            // (SignBlockEntity:148-150), но мы пишем текст напрямую через updateText() в
            // обход него, так что должны сами снять блокировку. Иначе, пока автор не отойдёт
            // от таблички дальше 4 блоков (см. SignBlockEntity.tick/clearInvalidPlayerWhoMayEdit),
            // ни один другой игрок вообще не может кликнуть по табличке, чтобы её прочитать.
            sign.setAllowedPlayerEditor(null);

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

    /** Разослать всем игрокам, на каком языке сейчас говорит этот игрок (для приглушения голоса). */
    public static void broadcastSpeakerLanguage(ServerPlayer player) {
        LanguageData data = player.getData(ModAttachments.LANGUAGE_DATA);
        PacketDistributor.sendToAllPlayers(new SyncSpeakerLanguagePayload(player.getUUID(), data.current().id()));
    }

    /**
     * Вызывать при входе игрока: сообщить ему текущий язык каждого из уже онлайн
     * игроков и разослать всем остальным его собственный (иначе те, кто уже был в
     * игре, не узнают о новом собеседнике, пока тот не сменит язык явно).
     */
    public static void sendAllSpeakerLanguages(ServerPlayer joiningPlayer) {
        for (ServerPlayer other : joiningPlayer.server.getPlayerList().getPlayers()) {
            LanguageData otherData = other.getData(ModAttachments.LANGUAGE_DATA);
            PacketDistributor.sendToPlayer(joiningPlayer, new SyncSpeakerLanguagePayload(other.getUUID(), otherData.current().id()));
        }
        broadcastSpeakerLanguage(joiningPlayer);
    }

    private NetworkHandler() {
    }
}
