package com.langsystem.util;

import com.langsystem.Language;
import com.langsystem.data.CompactSyllables;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Превращает произвольный текст в частично понятную речь в зависимости от уровня
 * прогресса изучения языка (0-100). Слово режется на слоги (грубо, по гласным), и для
 * каждого слога отдельно решается, узнан он или нет; неузнанные слоги превращаются в
 * символы из "письменности" языка — либо в заранее заданный компактный "иероглиф" для
 * частых слогов (см. {@link com.langsystem.data.CompactSyllables}), либо, если такого
 * нет, в случайный набор символов по одному на букву.
 *
 * <p>Шифр детерминирован не по позиции в тексте, а по содержимому слога — один и тот
 * же слог при одном и том же уровне владения всегда превращается в один и тот же набор
 * символов, независимо от слова или сообщения, в котором он встретился.</p>
 *
 * <p><b>Важно:</b> оба прохода (говорящего/пишущего и слушающего/читающего) всегда
 * выполняются НАД ОРИГИНАЛЬНЫМ текстом, а не последовательно один поверх результата
 * другого — иначе для уже подставленных символов "письменности" слоги резались бы
 * по-другому (символы чужого алфавита не распознаются как гласные) и на каждый повторный
 * проход получался бы другой, ещё более искажённый результат. Слог виден читателю "как
 * есть" только если его знают ОБА — и написавший, и читающий; поскольку проверка обоих
 * условий использует одно и то же зерно {@link #seedFor}, это математически совпадает
 * с одним проходом при пороге {@code min(грамотность автора, знание читателя)} —
 * см. {@link #read}.</p>
 */
public final class RuneCipher {

    private static final String LATIN_VOWELS = "aeiouyAEIOUY";
    private static final String CYRILLIC_VOWELS = "аеёиоуыэюяАЕЁИОУЫЭЮЯ";

    private RuneCipher() {
    }

    /**
     * Что ПИШУЩИЙ/ГОВОРЯЩИЙ фактически производит на этом языке — искажается в
     * зависимости от его собственного прогресса ("ломаная речь"). Используется как для
     * итогового "фонового" вида (без учёта того, кто именно смотрит), так и как первая
     * из двух частей критерия в {@link #read}.
     *
     * @param writerProgress 0..100 — прогресс самого пишущего/говорящего в этом языке
     */
    public static String produce(String text, Language language, int writerProgress) {
        return process(text, language, writerProgress);
    }

    /**
     * Что понимает КОНКРЕТНЫЙ читатель/слушатель в оригинальном тексте — с учётом ОБЕИХ
     * грамотностей разом: слог виден как есть только если его знает и написавший
     * (иначе он и не мог его верно написать), и сам читающий.
     *
     * @param rawText        оригинальный (ещё не искажённый) текст
     * @param writerProgress 0..100 — прогресс написавшего/сказавшего в этом языке
     * @param readerProgress 0..100 — прогресс читающего/слушающего в этом языке
     */
    public static String read(String rawText, Language language, int writerProgress, int readerProgress) {
        return process(rawText, language, Math.min(writerProgress, readerProgress));
    }

    private static String process(String text, Language language, int understandingPercent) {
        int understanding = Math.max(0, Math.min(100, understandingPercent));
        StringBuilder out = new StringBuilder(text.length());
        int i = 0;
        int n = text.length();
        while (i < n) {
            char c = text.charAt(i);
            if (!Character.isLetterOrDigit(c)) {
                // пробелы и пунктуацию оставляем как есть, чтобы сохранить ритм фразы
                out.append(c);
                i++;
                continue;
            }
            int start = i;
            while (i < n && Character.isLetterOrDigit(text.charAt(i))) {
                i++;
            }
            out.append(encipherWord(text.substring(start, i), language, understanding));
        }
        return out.toString();
    }

    private static String encipherWord(String word, Language language, int understanding) {
        List<String> syllables = splitSyllables(word);
        StringBuilder wordOut = new StringBuilder(word.length());
        for (String syllable : syllables) {
            int seed = seedFor(syllable, language);
            boolean known = Math.floorMod(seed, 100) < understanding;
            if (known) {
                wordOut.append(syllable);
                continue;
            }
            // Частые слоги для отдельных языков (пока — только hellborn) заменяются
            // одним заранее заданным коротким "иероглифом" вместо случайного набора
            // символов по букве — см. CompactSyllables.
            String compact = CompactSyllables.lookup(language, syllable.toLowerCase());
            wordOut.append(compact != null ? compact : glyphFor(syllable, seed, language));
        }
        return wordOut.toString();
    }

    /** Семя завязано на само содержимое слога и язык — не на позицию в тексте. */
    private static int seedFor(String syllable, Language language) {
        return (language.id() + ":" + syllable.toLowerCase()).hashCode();
    }

    /** Один и тот же слог всегда превращается в одну и ту же "непонятную" последовательность. */
    private static String glyphFor(String syllable, int seed, Language language) {
        String alphabet = language.glyphAlphabet();
        Random random = new Random(seed);
        StringBuilder glyph = new StringBuilder(syllable.length());
        for (int k = 0; k < syllable.length(); k++) {
            glyph.append(alphabet.charAt(random.nextInt(alphabet.length())));
        }
        return glyph.toString();
    }

    /**
     * Грубое разбиение слова на слоги: каждый слог тянется до ближайшей гласной
     * включительно, "хвост" без гласной (например, согласный кластер в конце) —
     * приклеивается к последнему слогу. Для слов без гласных (аббревиатуры, цифры)
     * всё слово считается одним "слогом".
     */
    private static List<String> splitSyllables(String word) {
        List<String> result = new ArrayList<>();
        int start = 0;
        for (int i = 0; i < word.length(); i++) {
            char c = word.charAt(i);
            if (isVowel(c)) {
                result.add(word.substring(start, i + 1));
                start = i + 1;
            }
        }
        if (start < word.length()) {
            if (result.isEmpty()) {
                result.add(word.substring(start));
            } else {
                int lastIndex = result.size() - 1;
                result.set(lastIndex, result.get(lastIndex) + word.substring(start));
            }
        }
        return result;
    }

    private static boolean isVowel(char c) {
        return LATIN_VOWELS.indexOf(c) >= 0 || CYRILLIC_VOWELS.indexOf(c) >= 0;
    }
}
