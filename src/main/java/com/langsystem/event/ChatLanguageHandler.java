package com.langsystem.event;

import com.langsystem.LangSystemMod;
import com.langsystem.Language;
import com.langsystem.SpeechDefect;
import com.langsystem.data.LanguageData;
import com.langsystem.data.ModAttachments;
import com.langsystem.util.RuneCipher;
import com.langsystem.util.SpeechFluency;
import com.langsystem.util.Visibility;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.ServerChatEvent;

import java.util.Random;

@EventBusSubscriber(modid = LangSystemMod.MOD_ID)
public final class ChatLanguageHandler {

    /** Максимальная дистанция, на которой вообще можно разглядеть жесты. */
    private static final double SIGN_VISIBILITY_RANGE = 32.0;

    @SubscribeEvent
    public static void onServerChat(ServerChatEvent event) {
        ServerPlayer sender = event.getPlayer();
        LanguageData senderData = sender.getData(ModAttachments.LANGUAGE_DATA);
        Language spoken = senderData.current();

        String rawMessage = event.getRawText();
        MinecraftServer server = sender.server;

        // Перехватываем стандартную рассылку — сами решаем, что покажет каждый получатель.
        event.setCanceled(true);

        int senderProgress = senderData.progress(spoken);

        if (!SpeechFluency.canSpeak(senderProgress)) {
            // Знания слишком мало, чтобы связно сказать хоть что-то на этом языке —
            // сообщение вообще никуда не уходит, автору лишь объясняем, в чём дело.
            sender.sendSystemMessage(Component.translatable("langsystem.msg.cannot_speak",
                            spoken.translatable(), senderProgress, SpeechFluency.CANNOT_SPEAK_BELOW)
                    .withStyle(style -> style.withColor(0xFF5555)));
            return;
        }

        // Дефекты речи (картавость и т.п.) искажают то, что говорящий физически
        // произносит — те же самые буквы, одинаково для всех получателей, независимо от
        // того, кто что понимает (в отличие от языкового шифра). Работает только здесь,
        // в чате — не затрагивает то, что записывается в книги/таблички.
        String spokenMessage = senderData.defects().isEmpty()
                ? rawMessage
                : SpeechDefect.applyAll(rawMessage, senderData.defects(), new Random());

        for (ServerPlayer recipient : server.getPlayerList().getPlayers()) {
            LanguageData recipientData = recipient.getData(ModAttachments.LANGUAGE_DATA);
            int understanding = recipient == sender ? 100 : recipientData.progress(spoken);

            // Приписку "[Язык]" рядом с ником показываем только тем, кто знает этот
            // язык хотя бы настолько, чтобы вообще опознать его на слух (тот же порог,
            // что и для способности самому на нём говорить) — иначе игрок даже не
            // поймёт, на каком языке к нему обращаются.
            boolean revealsLanguageName = recipient == sender || understanding >= SpeechFluency.CANNOT_SPEAK_BELOW;
            Component senderNameTag = Component.literal("<")
                    .append(sender.getDisplayName().copy())
                    .append(revealsLanguageName
                            ? Component.literal(" [").append(spoken.translatable()).append("]")
                            : Component.empty())
                    .append(Component.literal("> "));

            MutableComponent bodyComponent;

            if (spoken == Language.SIGN && recipient != sender && !Visibility.canSee(recipient, sender, SIGN_VISIBILITY_RANGE)) {
                // Язык жестов чисто визуальный — если получатель не видит говорящего
                // (закрыт стеной, отвернулся, слишком далеко), понять его невозможно
                // вообще, независимо от уровня владения языком.
                bodyComponent = Component.translatable("langsystem.msg.sign_language_no_sight");
            } else {
                bodyComponent = Component.literal(RuneCipher.read(spokenMessage, spoken, senderProgress, understanding));
            }

            // Текст всегда белый, без цвета конкретного языка.
            MutableComponent line = Component.empty()
                    .append(senderNameTag)
                    .append(bodyComponent.withStyle(style -> style.withColor(0xFFFFFF)));

            recipient.sendSystemMessage(line);
        }

        // Лог на сервере всегда содержит исходный (истинный) текст, независимо от того,
        // как ломано он прозвучал в игре — это полезно для модерации/отладки.
        LangSystemMod.LOGGER.info("[Chat/{}] {}: {}", spoken.id(), sender.getGameProfile().getName(), rawMessage);
    }

    private ChatLanguageHandler() {
    }
}
