package com.langsystem;

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.Random;

/**
 * Речевой дефект — постоянная черта игрока (выдаётся/забирается командой оператора,
 * {@code /language defect give|take|list}), не связанная с прогрессом изучения языка.
 * В отличие от {@link com.langsystem.util.RuneCipher} (шифрует НЕЗНАКОМЫЕ слоги под
 * конкретного читателя), дефект речи искажает то, что игрок физически ПРОИЗНОСИТ — те же
 * буквы видят все получатели одинаково, независимо от того, кто и что понимает.
 * Работает только в чате (см. {@code event.ChatLanguageHandler}) — не затрагивает то, что
 * записано в книгах/табличках.
 *
 * <p>Каждая буква, подходящая под условие дефекта, искажается не гарантированно, а с
 * определённым шансом ({@link #chance}) — так дефект выглядит естественно (не "каждая
 * буква без исключений"), и один и тот же текст может каждый раз "прозвучать" немного
 * по-разному. Само искажение буквы определяет {@link #transform} — это не обязательно
 * замена на другую букву (как в {@link #RHOTACISM}), может быть и повтор той же самой
 * (как в {@link #SIGMATISM}, "растянутое" шипение/свист).</p>
 */
public enum SpeechDefect {

    RHOTACISM("rhotacism", "Картавость", 0.85) {
        @Override
        protected String transform(char c, Random random) {
            return switch (c) {
                case 'р' -> "л";
                case 'Р' -> "Л";
                default -> null;
            };
        }
    },

    SIGMATISM("sigmatism", "Сигматизм", 0.7) {
        @Override
        protected String transform(char c, Random random) {
            if (c != 'с' && c != 'С') {
                return null;
            }
            // "растягивает" свистящий звук — на 1-2 лишних повтора той же буквы
            int extra = 1 + random.nextInt(2);
            return String.valueOf(c).repeat(1 + extra);
        }
    };

    private final String id;
    private final String displayName;
    private final double chance;

    SpeechDefect(String id, String displayName, double chance) {
        this.id = id;
        this.displayName = displayName;
        this.chance = chance;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    /** Как исказить конкретную подходящую букву; {@code null} — буква не подходит под условие дефекта. */
    protected abstract String transform(char c, Random random);

    public String apply(String text, Random random) {
        StringBuilder out = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            String replacement = random.nextDouble() < chance ? transform(c, random) : null;
            out.append(replacement != null ? replacement : String.valueOf(c));
        }
        return out.toString();
    }

    /** Прогоняет текст через все переданные дефекты по очереди (один и тот же random — на всё сообщение). */
    public static String applyAll(String text, Collection<SpeechDefect> defects, Random random) {
        String result = text;
        for (SpeechDefect defect : defects) {
            result = defect.apply(result, random);
        }
        return result;
    }

    public static Optional<SpeechDefect> byId(String id) {
        return Arrays.stream(values()).filter(d -> d.id.equalsIgnoreCase(id)).findFirst();
    }
}
