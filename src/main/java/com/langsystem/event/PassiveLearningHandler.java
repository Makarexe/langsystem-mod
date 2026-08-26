package com.langsystem.event;

import com.langsystem.LangSystemMod;
import com.langsystem.Language;
import com.langsystem.data.LanguageData;
import com.langsystem.data.ModAttachments;
import com.langsystem.network.NetworkHandler;
import com.langsystem.util.SpeechFluency;
import com.langsystem.util.Visibility;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.List;
import java.util.Random;

/**
 * Раз в некоторое время проверяет, кто из игроков стоит рядом с достаточно бегло
 * говорящим носителем языка, и с небольшим шансом чуть-чуть повышает их прогресс
 * изучения этого языка. Специально сделано редким и медленным — это фон, а не
 * замена команде/книгам, полное владение языком так за разумное время не выучить.
 */
@EventBusSubscriber(modid = LangSystemMod.MOD_ID)
public final class PassiveLearningHandler {

    /** Как часто вообще проверяем (в тиках). 600 тиков = 30 секунд. */
    private static final int CHECK_INTERVAL_TICKS = 600;
    /** Радиус, в котором можно "подслушать" обычную речь. */
    private static final double LISTEN_RADIUS = 12.0;
    /** Радиус, в котором можно разглядеть жесты (языку жестов нужна видимость). */
    private static final double SIGN_RADIUS = 10.0;
    /** Шанс получить +1% за одну проверку (раз в 30 секунд) — умышленно маленький. */
    private static final double GAIN_CHANCE = 0.05;

    private static final Random RANDOM = new Random();
    private static int tickCounter = 0;

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (++tickCounter < CHECK_INTERVAL_TICKS) {
            return;
        }
        tickCounter = 0;

        MinecraftServer server = event.getServer();
        List<ServerPlayer> players = server.getPlayerList().getPlayers();

        for (ServerPlayer learner : players) {
            LanguageData learnerData = learner.getData(ModAttachments.LANGUAGE_DATA);

            for (ServerPlayer speaker : players) {
                if (speaker == learner || speaker.level() != learner.level()) {
                    continue;
                }
                LanguageData speakerData = speaker.getData(ModAttachments.LANGUAGE_DATA);
                Language language = speakerData.current();
                if (language == Language.COMMON) {
                    continue; // всеобщий и так все знают
                }
                if (speakerData.progress(language) <= SpeechFluency.FLUENT_AT) {
                    continue; // сам говорящий владеет языком недостаточно бегло, учиться не у кого
                }
                if (learnerData.progress(language) >= 100) {
                    continue;
                }

                boolean nearEnough;
                if (language == Language.SIGN) {
                    nearEnough = Visibility.canSee(learner, speaker, SIGN_RADIUS);
                } else {
                    nearEnough = speaker.distanceToSqr(learner) <= LISTEN_RADIUS * LISTEN_RADIUS;
                }
                if (!nearEnough) {
                    continue;
                }

                if (RANDOM.nextDouble() < GAIN_CHANCE) {
                    int newProgress = learnerData.addProgress(language, 1);
                    learner.setData(ModAttachments.LANGUAGE_DATA, learnerData);
                    NetworkHandler.sendSync(learner);
                    learner.sendSystemMessage(Component.translatable("langsystem.msg.progress_gain",
                                    language.translatable(), newProgress)
                            .withStyle(style -> style.withColor(language.color())));
                }
            }
        }
    }

    private PassiveLearningHandler() {
    }
}
