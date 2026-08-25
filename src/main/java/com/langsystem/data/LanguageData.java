package com.langsystem.data;

import com.langsystem.Language;
import com.langsystem.SpeechDefect;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Изменяемый контейнер языковых данных игрока: прогресс изучения (0-100) по каждому
 * известному языку, текущий язык речи и время последнего "занятия" по книге на каждом
 * языке (чтобы нельзя было спамить книгу ради мгновенного изучения). Хранится как
 * Data Attachment и сериализуется вместе с игроком.
 */
public final class LanguageData {

    public static final Codec<LanguageData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.unboundedMap(Codec.STRING, Codec.INT).fieldOf("progress").forGetter(d ->
                    d.progress.entrySet().stream().collect(Collectors.toMap(e -> e.getKey().id(), Map.Entry::getValue))),
            Codec.STRING.fieldOf("current").forGetter(d -> d.current.id()),
            Codec.unboundedMap(Codec.STRING, Codec.LONG).fieldOf("studyCooldowns").forGetter(d ->
                    d.lastStudyTick.entrySet().stream().collect(Collectors.toMap(e -> e.getKey().id(), Map.Entry::getValue))),
            Codec.STRING.listOf().optionalFieldOf("defects", List.of()).forGetter(d ->
                    d.defects.stream().map(SpeechDefect::id).collect(Collectors.toList()))
    ).apply(instance, (progressMap, currentId, cooldownMap, defectIds) -> {
        LanguageData data = new LanguageData();
        data.progress.clear();
        progressMap.forEach((id, percent) -> Language.byId(id).ifPresent(l -> data.progress.put(l, clamp(percent))));
        data.progress.putIfAbsent(Language.COMMON, 100);
        data.current = Language.byId(currentId).filter(data::knows).orElse(Language.COMMON);
        cooldownMap.forEach((id, tick) -> Language.byId(id).ifPresent(l -> data.lastStudyTick.put(l, tick)));
        defectIds.forEach(id -> SpeechDefect.byId(id).ifPresent(data.defects::add));
        return data;
    }));

    private final Map<Language, Integer> progress = new EnumMap<>(Language.class);
    private final Map<Language, Long> lastStudyTick = new EnumMap<>(Language.class);
    private final Set<SpeechDefect> defects = EnumSet.noneOf(SpeechDefect.class);
    private Language current = Language.COMMON;

    public LanguageData() {
        progress.put(Language.COMMON, 100);
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }

    public boolean knows(Language language) {
        return progress(language) > 0;
    }

    public int progress(Language language) {
        return progress.getOrDefault(language, 0);
    }

    public Map<Language, Integer> allProgress() {
        return progress;
    }

    /** Устанавливает прогресс изучения языка. 0 — язык забыт (Всеобщий забыть нельзя). */
    public boolean setProgress(Language language, int percent) {
        int clamped = clamp(percent);
        if (language == Language.COMMON && clamped <= 0) {
            return false;
        }
        if (clamped <= 0) {
            boolean removed = progress.remove(language) != null;
            if (removed && current == language) {
                current = Language.COMMON;
            }
            return removed;
        }
        Integer previous = progress.put(language, clamped);
        return previous == null || !previous.equals(clamped);
    }

    /** +delta% к прогрессу (используется пассивным изучением), не опускается ниже текущего значения. */
    public int addProgress(Language language, int delta) {
        int updated = clamp(progress(language) + delta);
        progress.put(language, updated);
        return updated;
    }

    public Language current() {
        return current;
    }

    public boolean setCurrent(Language language) {
        if (!knows(language)) {
            return false;
        }
        current = language;
        return true;
    }

    /** Можно ли сейчас "позаниматься" по книге этого языка (кулдаун в тиках игрового времени). */
    public boolean canStudy(Language language, long currentGameTime, long cooldownTicks) {
        Long last = lastStudyTick.get(language);
        return last == null || currentGameTime - last >= cooldownTicks;
    }

    public void markStudied(Language language, long currentGameTime) {
        lastStudyTick.put(language, currentGameTime);
    }

    public Set<SpeechDefect> defects() {
        return defects;
    }

    public boolean hasDefect(SpeechDefect defect) {
        return defects.contains(defect);
    }

    public boolean addDefect(SpeechDefect defect) {
        return defects.add(defect);
    }

    public boolean removeDefect(SpeechDefect defect) {
        return defects.remove(defect);
    }

    public LanguageData copy() {
        LanguageData copy = new LanguageData();
        copy.progress.clear();
        copy.progress.putAll(this.progress);
        copy.lastStudyTick.putAll(this.lastStudyTick);
        copy.defects.addAll(this.defects);
        copy.current = this.current;
        return copy;
    }
}
