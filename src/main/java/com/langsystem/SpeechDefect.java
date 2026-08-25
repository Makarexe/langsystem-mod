package com.langsystem;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
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
 * <p>Каждая буква, подходящая под условие дефекта, заменяется не гарантированно, а с
 * определённым шансом — так дефект выглядит естественно (не "каждая Р без исключений"),
 * и один и тот же текст может каждый раз "прозвучать" немного по-разному.</p>
 */
public enum SpeechDefect {

    RHOTACISM("rhotacism", "Картавость", 0.85, Map.of(
            'р', "л",
            'Р', "Л"
    ));

    private final String id;
    private final String displayName;
    private final double chance;
    private final Map<Character, String> replacements;

    SpeechDefect(String id, String displayName, double chance, Map<Character, String> replacements) {
        this.id = id;
        this.displayName = displayName;
        this.chance = chance;
        this.replacements = replacements;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public String apply(String text, Random random) {
        StringBuilder out = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            String replacement = replacements.get(c);
            out.append(replacement != null && random.nextDouble() < chance ? replacement : String.valueOf(c));
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
