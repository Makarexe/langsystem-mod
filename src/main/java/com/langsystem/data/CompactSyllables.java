package com.langsystem.data;

import com.langsystem.Language;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Для отдельных языков самые частые русские слоги (куски слов, из которых состоит
 * почти любой текст) заменяются не случайным набором символов по одному на букву, а
 * одним заранее заданным коротким "иероглифом" — 1-3 буквы алфавита языка, независимо
 * от того, сколько букв было в самом слоге. Так короткие частые слоги вроде "не" или
 * "при" выглядят как единый компактный символ, а не растягиваются по буквам — этим
 * язык визуально больше похож на настоящую письменность с иероглифами/лигатурами, а не
 * на зашифрованную кириллицу.
 *
 * <p>Пока (как тест) заполнено только для {@link Language#HELLBORN}. Сами буквы взяты
 * из иврита, но эти конкретные сочетания — не настоящие ивритские слова, это
 * стилизованная "скоропись" для кусков слов.</p>
 */
public final class CompactSyllables {

    private static final Map<Language, Map<String, String>> BY_LANGUAGE = new EnumMap<>(Language.class);

    static {
        BY_LANGUAGE.put(Language.HELLBORN, buildHellborn());
    }

    /** @return короткий (1-3 буквы) иероглиф для слога, либо {@code null}, если для него нет записи. */
    public static String lookup(Language language, String syllableLowerCase) {
        Map<String, String> table = BY_LANGUAGE.get(language);
        return table == null ? null : table.get(syllableLowerCase);
    }

    private static Map<String, String> buildHellborn() {
        Map<String, String> m = new LinkedHashMap<>();

        // --- самые частые слоги — по одной букве ---
        m.put("по", "א");
        m.put("на", "ב");
        m.put("не", "ג");
        m.put("во", "ד");
        m.put("ве", "ה");
        m.put("ка", "ו");
        m.put("ко", "ז");
        m.put("то", "ח");
        m.put("та", "ט");
        m.put("ли", "י");
        m.put("ла", "ך");
        m.put("ра", "כ");
        m.put("ре", "ל");
        m.put("ни", "ם");
        m.put("да", "מ");
        m.put("же", "ן");
        m.put("си", "נ");
        m.put("го", "ס");
        m.put("де", "ע");
        m.put("ти", "ף");
        m.put("ди", "פ");
        m.put("ро", "ץ");
        m.put("ме", "צ");
        m.put("ми", "ק");
        m.put("но", "ר");
        m.put("ны", "ש");
        m.put("ся", "ת");

        // --- чуть менее частые, но всё ещё распространённые слоги/приставки — по две буквы ---
        m.put("при", "אח");
        m.put("про", "בט");
        m.put("раз", "גי");
        m.put("ста", "דך");
        m.put("что", "הכ");
        m.put("как", "ול");

        return m;
    }

    private CompactSyllables() {
    }
}
