package com.langsystem.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** Простое клиентское хранилище последних синхронизированных языковых данных игрока. */
public final class ClientLanguageState {

    public record Entry(String languageId, int progress) {
    }

    private static volatile List<Entry> entries = List.of(new Entry("common", 100));
    private static volatile String current = "common";

    /**
     * На каком языке сейчас говорит КАЖДЫЙ ИЗ ВИДИМЫХ игроков (не только свой), см.
     * {@link com.langsystem.network.SyncSpeakerLanguagePayload}. Нужно для приглушения
     * чужого голоса в Simple Voice Chat — обращение может происходить не в основном
     * клиентском потоке, поэтому {@link ConcurrentHashMap}.
     */
    private static final Map<UUID, String> speakerLanguages = new ConcurrentHashMap<>();

    public static void update(List<String> ids, List<Integer> percents, String newCurrent) {
        List<Entry> updated = new ArrayList<>();
        for (int i = 0; i < ids.size() && i < percents.size(); i++) {
            updated.add(new Entry(ids.get(i), percents.get(i)));
        }
        entries = updated;
        current = newCurrent;
    }

    public static List<Entry> entries() {
        return entries;
    }

    /** Прогресс локального игрока в конкретном языке (0, если язык вообще не знаком). */
    public static int progressOf(String languageId) {
        for (Entry entry : entries) {
            if (entry.languageId().equals(languageId)) {
                return entry.progress();
            }
        }
        return 0;
    }

    public static String current() {
        return current;
    }

    public static void updateSpeakerLanguage(UUID playerId, String languageId) {
        speakerLanguages.put(playerId, languageId);
    }

    /** Всеобщий по умолчанию, пока не пришла синхронизация — как и на сервере. */
    public static String speakerLanguageOf(UUID playerId) {
        return speakerLanguages.getOrDefault(playerId, "common");
    }

    private ClientLanguageState() {
    }
}
