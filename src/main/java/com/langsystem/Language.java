package com.langsystem;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.Arrays;
import java.util.Optional;

/**
 * Языки, существующие в мире. COMMON — всеобщий торговый язык, известен всем игрокам
 * со старта на 100%. Остальные нужно изучать (выдаются командой с уровнем прогресса).
 */
public enum Language {
    COMMON("common", 0xFFFFFF, "abcdefghijklmnopqrstuvwxyz"), // Всеобщий
    HUMAN("human", 0x55FF55, cyrillicRange()), // Людской
    DWARVEN("dwarven", 0xB08040, runicRange()), // Дворфийский
    ELVEN("elven", 0x66CCFF, glagoliticRange()), // Эльфийский
    BEASTKIN("beastkin", 0xCC8844, cherokeeRange()), // Зверолюдский
    DRACONIC("draconic", 0xDD3333, brailleRange()), // Драконий
    FEYBORN("feyborn", 0x66DD99, oghamRange()), // Феерождённых
    HELLBORN("hellborn", 0x992222, hebrewRange()), // Адорождённых
    ABYSS("abyss", 0x4B0082, tifinaghRange()), // Бездны
    PRIMORDIAL("primordial", 0xD4AF37, copticRange()), // Первородный
    ANCIENT("ancient", 0x1E9C8B, georgianRange()), // Древний
    SIGN("sign", 0xCCCCCC, dingbatRange()); // Язык жестов

    private final String id;
    private final int color;
    private final String glyphAlphabet;

    Language(String id, int color, String glyphAlphabet) {
        this.id = id;
        this.color = color;
        this.glyphAlphabet = glyphAlphabet;
    }

    public String id() {
        return id;
    }

    public int color() {
        return color;
    }

    /** Набор символов, которым язык "маскирует" непонятную речь. */
    public String glyphAlphabet() {
        return glyphAlphabet;
    }

    /**
     * Локализуемое название языка — единственный источник имени языка теперь лежит в
     * lang-файлах (ключ {@code langsystem.language.<id>}), а не захардкожен в перечислении:
     * такой Component переводится на стороне КАЖДОГО клиента отдельно по его языку игры.
     */
    public MutableComponent translatable() {
        return Component.translatable("langsystem.language." + id);
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

    private static String tifinaghRange() {
        return buildRange(0x2D30, 0x2D65); // тифинаг — угловатое "чужое" письмо для демонов Бездны
    }

    private static String copticRange() {
        return buildRange(0x2C80, 0x2CB1); // коптское письмо для древних письмён старых богов
    }

    private static String georgianRange() {
        return buildRange(0x10A0, 0x10C5); // асомтаврули — древнейшее грузинское письмо, для Древнего языка
    }

    private static String buildRange(int fromInclusive, int toInclusive) {
        StringBuilder sb = new StringBuilder();
        for (int cp = fromInclusive; cp <= toInclusive; cp++) {
            sb.appendCodePoint(cp);
        }
        return sb.toString();
    }
}
