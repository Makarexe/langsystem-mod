package com.langsystem.voicechat;

import com.langsystem.LangSystemMod;
import com.langsystem.Language;
import com.langsystem.client.ClientLanguageState;
import com.langsystem.client.VoiceDebugState;
import de.maxhenkel.voicechat.api.ForgeVoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.ClientReceiveSoundEvent;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Опциональная интеграция с Simple Voice Chat: если слушатель недостаточно знает
 * язык, на котором сейчас говорит собеседник (прогресс синхронизируется отдельно, см.
 * {@link com.langsystem.network.SyncSpeakerLanguagePayload}), его голос приглушается —
 * тем сильнее, чем ниже прогресс слушателя в этом языке. Свободное владение (100%) —
 * без изменений.
 *
 * <p>Для языков из {@link #CREATURE_SOUNDS} вместо тонкого DSP-фильтра дополнительно
 * проигрывается настоящий ванильный звук существа рядом с говорящим (адорождённые —
 * блэйз, дворфийский — поборник/разбойник, эльфийский — ведьма, зверолюдский — смесь
 * из амбиента большинства обычных зверей, драконий — эндер-дракон, феерождённые —
 * аллай, людской — житель, Бездны — стражи/дельфины/спруты/утопленник, древний —
 * звуки нотных блоков (с укороченным кулдауном — см. {@link #CREATURE_SOUND_COOLDOWN_OVERRIDES}),
 * первородный — варден и эндермен), а сам голос почти
 * полностью приглушается — это гораздо заметнее на слух, чем один только фильтр. У
 * всеобщего своего "звучания" нет — там только приглушение голоса. Язык жестов — особый
 * случай: голоса у него не бывает в принципе, глушится полностью и безусловно.</p>
 *
 * <p>Событие {@code ClientReceiveSoundEvent} у Simple Voice Chat срабатывает на КАЖДОМ
 * слушающем клиенте отдельно, ещё до проигрывания звука — эффект можно сделать разным
 * для разных слушателей одного и того же голоса, как и с текстом в чате. Обработчик
 * этого события вызывается НЕ на основном потоке клиента — само изменение сэмплов
 * (чистая арифметика) остаётся прямо в обработчике, а любое обращение к игровому миру
 * (проигрывание звука, сообщение в чат) откладывается через {@code Minecraft.execute}.</p>
 *
 * <p>Команда {@code /langvoicedebug} ({@link VoiceDebugState}) включает отладочные
 * сообщения в чат — они пишутся НА КАЖДОМ шаге принятия решения (даже когда эффект не
 * нужен), чтобы можно было отличить "перехват вообще не срабатывает" от "срабатывает,
 * но эффект в этот раз не требуется" — иначе по факту "тишины в логе" нельзя понять,
 * где именно остановилась логика.</p>
 *
 * <p>Этот класс — единственное место в моде, которое напрямую ссылается на классы API
 * Simple Voice Chat. Обращается к нему только сам Simple Voice Chat, сканируя мод на
 * аннотацию {@link ForgeVoicechatPlugin} — если он не установлен, никто этот класс не
 * трогает, и мод спокойно работает без него (зависимость в {@code neoforge.mods.toml}
 * помечена {@code optional}).</p>
 */
@ForgeVoicechatPlugin
public final class LangSystemVoicechatPlugin implements VoicechatPlugin {

    private static final Map<Language, List<SoundEvent>> CREATURE_SOUNDS = new EnumMap<>(Language.class);

    static {
        CREATURE_SOUNDS.put(Language.HELLBORN, List.of(
                SoundEvents.BLAZE_AMBIENT, SoundEvents.BLAZE_BURN));
        CREATURE_SOUNDS.put(Language.DWARVEN, List.of(
                SoundEvents.VINDICATOR_AMBIENT, SoundEvents.VINDICATOR_CELEBRATE,
                SoundEvents.PILLAGER_AMBIENT, SoundEvents.PILLAGER_CELEBRATE));
        CREATURE_SOUNDS.put(Language.ELVEN, List.of(
                SoundEvents.WITCH_AMBIENT, SoundEvents.WITCH_CELEBRATE,
                SoundEvents.WITCH_DRINK, SoundEvents.WITCH_THROW));
        CREATURE_SOUNDS.put(Language.BEASTKIN, List.of(
                SoundEvents.WOLF_GROWL, SoundEvents.WOLF_HOWL, SoundEvents.COW_AMBIENT,
                SoundEvents.PIG_AMBIENT, SoundEvents.SHEEP_AMBIENT, SoundEvents.CHICKEN_AMBIENT,
                SoundEvents.CAT_AMBIENT, SoundEvents.HORSE_AMBIENT, SoundEvents.RABBIT_AMBIENT,
                SoundEvents.FOX_AMBIENT, SoundEvents.PANDA_AMBIENT, SoundEvents.LLAMA_AMBIENT,
                SoundEvents.POLAR_BEAR_AMBIENT, SoundEvents.DONKEY_AMBIENT, SoundEvents.GOAT_AMBIENT,
                SoundEvents.DOLPHIN_AMBIENT, SoundEvents.TURTLE_AMBIENT_LAND, SoundEvents.PARROT_AMBIENT,
                SoundEvents.FROG_AMBIENT, SoundEvents.CAMEL_AMBIENT, SoundEvents.BAT_AMBIENT,
                SoundEvents.OCELOT_AMBIENT, SoundEvents.SNIFFER_IDLE));
        CREATURE_SOUNDS.put(Language.DRACONIC, List.of(
                SoundEvents.ENDER_DRAGON_GROWL, SoundEvents.ENDER_DRAGON_AMBIENT));
        CREATURE_SOUNDS.put(Language.FEYBORN, List.of(
                SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM, SoundEvents.ALLAY_ITEM_GIVEN));
        CREATURE_SOUNDS.put(Language.HUMAN, List.of(
                SoundEvents.VILLAGER_AMBIENT, SoundEvents.VILLAGER_YES, SoundEvents.VILLAGER_TRADE));
        // Древний и Первородный поменялись местами относительно того, что было раньше:
        // мелодия нотных блоков лучше подходит "древнему" как отголоски забытой музыки,
        // а варден/эндермен — как раз тем самым "старым богам" из Первородного.
        CREATURE_SOUNDS.put(Language.ANCIENT, List.of(
                SoundEvents.NOTE_BLOCK_HARP.value(), SoundEvents.NOTE_BLOCK_BELL.value(),
                SoundEvents.NOTE_BLOCK_CHIME.value(), SoundEvents.NOTE_BLOCK_XYLOPHONE.value()));
        CREATURE_SOUNDS.put(Language.ABYSS, List.of(
                SoundEvents.GUARDIAN_AMBIENT, SoundEvents.GUARDIAN_ATTACK,
                SoundEvents.ELDER_GUARDIAN_AMBIENT, SoundEvents.ELDER_GUARDIAN_CURSE,
                SoundEvents.DOLPHIN_AMBIENT, SoundEvents.DOLPHIN_AMBIENT_WATER,
                SoundEvents.SQUID_AMBIENT, SoundEvents.GLOW_SQUID_AMBIENT,
                SoundEvents.DROWNED_AMBIENT_WATER));
        // Без ENDERMAN_SCREAM ("рёв", слишком долгий) и ENDERMAN_TELEPORT — оба
        // слишком выделяются/затягивают по сравнению с остальными короткими звуками.
        CREATURE_SOUNDS.put(Language.PRIMORDIAL, List.of(
                SoundEvents.WARDEN_AMBIENT, SoundEvents.WARDEN_HEARTBEAT, SoundEvents.WARDEN_LISTENING,
                SoundEvents.WARDEN_TENDRIL_CLICKS,
                SoundEvents.ENDERMAN_AMBIENT, SoundEvents.ENDERMAN_STARE));
    }

    /**
     * Индивидуальные кулдауны звука существа для отдельных языков — если не указан,
     * используется {@link #CREATURE_SOUND_COOLDOWN_MS}. У Древнего (нотные блоки) звуки
     * короткие и не мешают друг другу, поэтому кулдаун ощутимо меньше — иначе редкая
     * "мелодия" звучит слишком разрежённо.
     */
    private static final Map<Language, Long> CREATURE_SOUND_COOLDOWN_OVERRIDES = new EnumMap<>(Language.class);

    static {
        CREATURE_SOUND_COOLDOWN_OVERRIDES.put(Language.ANCIENT, 200L);
    }

    /** Ниже этого прогресса голос вообще не слышен — только звуки существ. */
    private static final int MUFFLED_ONLY_THRESHOLD = 40;
    /** С этого прогресса звуки существ больше не играют — только (постепенно проясняющийся) голос. */
    private static final int CLEAR_THRESHOLD = 80;

    private static final long CREATURE_SOUND_COOLDOWN_MS = 350;
    private static final float CREATURE_SOUND_VOLUME = 1.8f;
    private static final long DEBUG_MESSAGE_COOLDOWN_MS = 3000;

    private final Map<UUID, Long> lastCreatureSoundAt = new ConcurrentHashMap<>();
    private final Map<String, Long> lastDebugMessageAt = new ConcurrentHashMap<>();
    private final Random random = new Random();

    @Override
    public String getPluginId() {
        return LangSystemMod.MOD_ID;
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        // Simple Voice Chat диспетчерит по ТОЧНОМУ классу через Map.get(Class), без
        // instanceof — регистрация на базовый ClientReceiveSoundEvent.class никогда не
        // совпадёт с реальными вызовами (они всегда идут на конкретные вложенные типы
        // EntitySound/LocationalSound/StaticSound), обработчик просто никогда не вызовется,
        // без единого исключения в логе. Подтверждено разбором байткода настоящего мода.
        registration.registerEvent(ClientReceiveSoundEvent.EntitySound.class, this::onReceiveSound);
    }

    private void onReceiveSound(ClientReceiveSoundEvent.EntitySound entitySound) {
        boolean debug = VoiceDebugState.isEnabled();
        LocalPlayer listener = Minecraft.getInstance().player;
        if (listener == null) {
            return;
        }
        UUID speakerId = entitySound.getEntityId();
        if (speakerId.equals(listener.getUUID())) {
            return; // свой собственный голос не трогаем и не логируем — это ожидаемо на каждом пакете
        }

        String languageId = ClientLanguageState.speakerLanguageOf(speakerId);
        Language language = Language.byId(languageId).orElse(Language.COMMON);

        if (language == Language.SIGN) {
            // У языка жестов никогда нет голоса — глушим полностью и безусловно, вне
            // зависимости от прогресса слушателя (в отличие от остальных языков, где на
            // 100% эффект вообще не применяется).
            short[] signAudio = entitySound.getRawAudio();
            if (signAudio.length > 0) {
                entitySound.setRawAudio(new short[signAudio.length]);
            }
            logDebug(debug, speakerId.toString(), Component.translatable("langsystem.voice.debug.sign", shortId(speakerId)));
            return;
        }

        int myProgress = ClientLanguageState.progressOf(language.id());

        if (myProgress >= 100) {
            logDebug(debug, speakerId.toString(), Component.translatable("langsystem.voice.debug.fluent",
                    shortId(speakerId), language.translatable()));
            return; // язык знаком свободно — без изменений
        }

        short[] audio = entitySound.getRawAudio();
        if (audio.length == 0) {
            return; // пустой массив = конец потока, API просит его не трогать
        }

        float strength = 1f - myProgress / 100f;
        List<SoundEvent> creatureSounds = CREATURE_SOUNDS.get(language);

        // Три полосы прогресса (только для языков со "звучанием" существа):
        // < 40% — только звуки существ, голос полностью выключен;
        // 40-80% — голос появляется (слегка приглушённый) и постепенно набирает силу,
        //          звуки существ тем временем стихают, пока не пропадут совсем к 80%;
        // >= 80% — звуков существ больше нет, голос доигрывает обычное приглушение
        //          (strength), плавно проясняясь до 100%.
        float creatureIntensity;
        float voiceVolume;
        if (creatureSounds == null) {
            creatureIntensity = 0f;
            voiceVolume = 1f;
        } else if (myProgress < MUFFLED_ONLY_THRESHOLD) {
            creatureIntensity = 1f;
            voiceVolume = 0f;
        } else if (myProgress < CLEAR_THRESHOLD) {
            float t = (myProgress - MUFFLED_ONLY_THRESHOLD) / (float) (CLEAR_THRESHOLD - MUFFLED_ONLY_THRESHOLD);
            creatureIntensity = 1f - t;
            voiceVolume = 0.2f + 0.8f * t;
        } else {
            creatureIntensity = 0f;
            voiceVolume = 1f;
        }

        boolean playCreatureSound = creatureIntensity > 0f && isCreatureSoundDue(speakerId, language)
                && random.nextFloat() < creatureIntensity;
        entitySound.setRawAudio(muffle(audio, strength, voiceVolume));

        SoundEvent chosenSound = playCreatureSound ? creatureSounds.get(random.nextInt(creatureSounds.size())) : null;
        // Широкий разброс высоты тона — чтобы наложенные друг на друга звуки не сливались
        // в один и тот же монотонный вопль, а звучали как несколько разных существ.
        float pitch = 0.8f + random.nextFloat() * 0.5f;
        boolean shouldLog = debug && isDue(speakerId.toString());

        if (chosenSound == null && !shouldLog) {
            return;
        }

        Minecraft.getInstance().execute(() -> {
            ClientLevel level = Minecraft.getInstance().level;
            if (level == null) {
                return;
            }
            AbstractClientPlayer speaker = findPlayer(level, speakerId);
            if (chosenSound != null && speaker != null) {
                level.playLocalSound(speaker, chosenSound, SoundSource.PLAYERS, CREATURE_SOUND_VOLUME, pitch);
            }
            if (shouldLog) {
                String name = speaker != null ? speaker.getGameProfile().getName() : shortId(speakerId);
                String key = chosenSound != null ? "langsystem.voice.debug.creature_sound" : "langsystem.voice.debug.muffled";
                Component message = Component.translatable(key, name, language.translatable(),
                        myProgress, Math.round(strength * 100));
                listener.displayClientMessage(message, false);
            }
        });
    }

    private static String shortId(UUID id) {
        return id.toString().substring(0, 8);
    }

    @Nullable
    private static AbstractClientPlayer findPlayer(ClientLevel level, UUID id) {
        for (AbstractClientPlayer p : level.players()) {
            if (p.getUUID().equals(id)) {
                return p;
            }
        }
        return null;
    }

    private boolean isCreatureSoundDue(UUID speakerId, Language language) {
        long cooldown = CREATURE_SOUND_COOLDOWN_OVERRIDES.getOrDefault(language, CREATURE_SOUND_COOLDOWN_MS);
        long now = System.currentTimeMillis();
        Long last = lastCreatureSoundAt.get(speakerId);
        if (last != null && now - last < cooldown) {
            return false;
        }
        lastCreatureSoundAt.put(speakerId, now);
        return true;
    }

    private boolean isDue(String key) {
        long now = System.currentTimeMillis();
        Long last = lastDebugMessageAt.get(key);
        if (last != null && now - last < DEBUG_MESSAGE_COOLDOWN_MS) {
            return false;
        }
        lastDebugMessageAt.put(key, now);
        return true;
    }

    private void logDebug(boolean enabled, String throttleKey, Component message) {
        if (!enabled || !isDue(throttleKey)) {
            return;
        }
        Minecraft.getInstance().execute(() -> {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null) {
                player.displayClientMessage(message, false);
            }
        });
    }

    /**
     * Однополюсный фильтр нижних частот (экспоненциальное сглаживание) — чем сильнее
     * {@code strength}, тем сильнее "смазан" сигнал (звучит глухо/невнятно, как через
     * стену). {@code voiceVolume} — дополнительное общее снижение громкости (используется,
     * чтобы почти заглушить голос там, где вместо него играет звук существа).
     */
    private static short[] muffle(short[] input, float strength, float voiceVolume) {
        float alpha = 1f - strength * 0.9f;
        float volume = (1f - strength * 0.3f) * voiceVolume;
        short[] out = new short[input.length];
        float state = input[0];
        for (int i = 0; i < input.length; i++) {
            state = alpha * input[i] + (1f - alpha) * state;
            int sample = Math.round(state * volume);
            out[i] = (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, sample));
        }
        return out;
    }
}
