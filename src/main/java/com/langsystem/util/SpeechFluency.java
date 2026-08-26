package com.langsystem.util;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/**
 * Пороги владения языком, которые определяют, может ли игрок вообще ГОВОРИТЬ на
 * нём (в отличие от {@link com.langsystem.data.LanguageData#progress}, который
 * также используется для ПОНИМАНИЯ чужой речи — эти два аспекта симметричны и
 * используют одну и ту же шкалу 0-100, но по разным правилам применения).
 *
 * <ul>
 *   <li>ниже {@link #CANNOT_SPEAK_BELOW}% — язык знаком слишком слабо, чтобы на нём
 *       вообще связно говорить; сообщение не отправляется, автор получает пояснение;</li>
 *   <li>от {@link #CANNOT_SPEAK_BELOW}% до {@link #FLUENT_AT}% (не включительно) —
 *       "ломаная" речь: сообщение перед отправкой само проходит через
 *       {@link RuneCipher#encipher}, используя собственный прогресс говорящего —
 *       то есть часть слов/корней говорящий сам произносит неправильно, и даже
 *       получатель, знающий язык на 100%, увидит эти прорехи как есть;</li>
 *   <li>от {@link #FLUENT_AT}% — бегло, сообщение уходит без искажений со стороны
 *       говорящего (дальше на него всё равно влияет то, насколько язык понимает
 *       уже получатель).</li>
 * </ul>
 */
public final class SpeechFluency {

    /** Ниже этого прогресса связно говорить на языке невозможно вообще. */
    public static final int CANNOT_SPEAK_BELOW = 15;

    /** С этого прогресса речь уже беглая, без собственных "ломаных" мест. */
    public static final int FLUENT_AT = 75;

    private SpeechFluency() {
    }

    public static boolean canSpeak(int progress) {
        return progress >= CANNOT_SPEAK_BELOW;
    }

    public static boolean isFluent(int progress) {
        return progress >= FLUENT_AT;
    }

    /**
     * Короткая подпись состояния — используется в GUI выбора языка. См.
     * {@link com.langsystem.Language#translatable()} — переводится по языку игры
     * каждого клиента отдельно, а не захардкожена по-русски.
     */
    public static MutableComponent translatableLabel(int progress) {
        if (progress >= 100) {
            return Component.translatable("langsystem.fluency.free");
        }
        if (isFluent(progress)) {
            return Component.translatable("langsystem.fluency.fluent");
        }
        if (canSpeak(progress)) {
            return Component.translatable("langsystem.fluency.broken");
        }
        return Component.translatable("langsystem.fluency.cannot_speak");
    }
}
