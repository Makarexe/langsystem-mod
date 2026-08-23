package com.langsystem;

import java.util.Arrays;
import java.util.Optional;

/**
 * Языки, существующие в мире. COMMON — всеобщий торговый язык, известен всем игрокам
 * со старта на 100%. Остальные нужно изучать (выдаются командой с уровнем прогресса).
 */
public enum Language {
    COMMON("common", "Всеобщий", 0xFFFFFF, "abcdefghijklmnopqrstuvwxyz"),
    HUMAN("human", "Людской", 0x55FF55, cyrillicRange()),
    DWARVEN("dwarven", "Дворфийский", 0xB08040, runicRange()),
    ELVEN("elven", "Эльфийский", 0x66CCFF, glagoliticRange()),
    BEASTKIN("beastkin", "Зверолюдский", 0xCC8844, cherokeeRange()),
    DRACONIC("draconic", "Драконий", 0xDD3333, brailleRange()),
    FEYBORN("feyborn", "Феерождённых", 0x66DD99, oghamRange()),
    HELLBORN("hellborn", "Адорождённых", 0x992222, hebrewRange()),
    SIGN("sign", "Язык жестов", 0xCCCCCC, dingbatRange());

    private final String id;
    private final String displayName;
    private final int color;
    private final String glyphAlphabet;

    Language(String id, String displayName, int color, String glyphAlphabet) {
        this.id = id;
        this.displayName = displayName;
        this.color = color;
        this.glyphAlphabet = glyphAlphabet;
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public int color() {
        return color;
    }

    /** Набор символов, которым язык "маскирует" непонятную речь. */
    public String glyphAlphabet() {
        return glyphAlphabet;
    }

    public static Optional<Language> byId(String id) {
        return Arrays.stream(values()).filter(l -> l.id.equalsIgnoreCase(id)).findFirst();
    }

    // --- диапазоны символов Unicode для "письменностей" каждого языка ---

    private static String cyrillicRange() {
        return buildRange(0x0430, 0x044F); // строчная кириллица
    }

    private static String runicRange() {
        return buildRange(0x16A0, 0x16F0); // руны
    }

    private static String glagoliticRange() {
        return buildRange(0x2C30, 0x2C5E); // глаголица (строчная)
    }

    private static String cherokeeRange() {
        return buildRange(0x13A0, 0x13D0); // слоговое письмо чероки
    }

    private static String brailleRange() {
        return buildRange(0x2801, 0x2840); // шрифт Брайля — "когтистые" точки дракона
    }

    private static String oghamRange() {
        return buildRange(0x1681, 0x169A); // огам — фэйское/друидическое письмо
    }

    private static String hebrewRange() {
        return buildRange(0x05D0, 0x05EA); // иврит — основа адской письменности
    }

    private static String dingbatRange() {
        return buildRange(0x2701, 0x2720); // символы-пиктограммы для языка жестов
    }

    private static String buildRange(int fromInclusive, int toInclusive) {
        StringBuilder sb = new StringBuilder();
        for (int cp = fromInclusive; cp <= toInclusive; cp++) {
            sb.appendCodePoint(cp);
        }
        return sb.toString();
    }
}
