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
 * <p>Для языков из {@link #CREATURE_SOUNDS} (пока только Адорождённых) вместо тонкого
 * DSP-фильтра дополнительно проигрывается настоящий ванильный звук существа рядом с
 * говорящим (для Адорождённых — блэйз/гаст, "звуки ифрита"), а сам голос почти
 * полностью приглушается — это гораздо заметнее на слух, чем один только фильтр.</p>
 *
 * <p>Событие {@code ClientReceiveSoundEvent} у Simple Voice Chat срабатывает на КАЖДОМ
 * слушающем клиенте отдельно, ещё до проигрывания звука — эффект можно сделать разным
 * для разных слушателей одного и того же голоса, как и с текстом в чате. Обработчик
 * этого события вызывается НЕ на основном потоке клиента — само изменение сэмплов
 * (чистая арифметика) остаётся прямо в обработчике, а любое обращение к игровому миру
 * (проигрывание звука, сообщение в чат) откладывается через {@code Minecraft.execute}.</p>
 *
 * <p>Включить отладочные сообщения в чат — команда {@code /langvoicedebug}
 * ({@link VoiceDebugState}) — полезно, чтобы увидеть, что перехват вообще срабатывает,
 * раз сам эффект на слух проверить не получается без живого теста.</p>
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
                SoundEvents.BLAZE_AMBIENT, SoundEvents.BLAZE_BURN, SoundEvents.GHAST_AMBIENT));
    }

    private static final long CREATURE_SOUND_COOLDOWN_MS = 1500;
    private static final long DEBUG_MESSAGE_COOLDOWN_MS = 2000;

    private final Map<UUID, Long> lastCreatureSoundAt = new ConcurrentHashMap<>();
    private final Map<UUID, Long> lastDebugMessageAt = new ConcurrentHashMap<>();
    private final Random random = new Random();

    @Override
    public String getPluginId() {
        return LangSystemMod.MOD_ID;
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(ClientReceiveSoundEvent.class, this::onReceiveSound);
    }

    private void onReceiveSound(ClientReceiveSoundEvent event) {
        if (!(event instanceof ClientReceiveSoundEvent.EntitySound entitySound)) {
            return; // не голос игрока рядом (позиционный/статичный звук) — не трогаем
        }
        LocalPlayer listener = Minecraft.getInstance().player;
        if (listener == null) {
            return;
        }
        UUID speakerId = entitySound.getEntityId();
        if (speakerId.equals(listener.getUUID())) {
            return; // свой собственный голос не трогаем
        }

        String languageId = ClientLanguageState.speakerLanguageOf(speakerId);
        Language language = Language.byId(languageId).orElse(Language.COMMON);
        int myProgress = ClientLanguageState.progressOf(language.id());
        if (myProgress >= 100) {
            return; // язык знаком свободно — без изменений
        }

        short[] audio = event.getRawAudio();
        if (audio.length == 0) {
            return; // пустой массив = конец потока, API просит его не трогать
        }

        float strength = 1f - myProgress / 100f;
        List<SoundEvent> creatureSounds = CREATURE_SOUNDS.get(language);
        boolean playCreatureSound = creatureSounds != null && isCreatureSoundDue(speakerId);
        // Если для языка есть "звучание" существа — голос под ним почти не должен
        // быть слышен, иначе он мешает и смазывает эффект.
        float voiceVolume = creatureSounds != null ? 0.1f : 1f;
        event.setRawAudio(muffle(audio, strength, voiceVolume));

        boolean shouldLog = VoiceDebugState.isEnabled() && isDebugMessageDue(speakerId);
        if (!playCreatureSound && !shouldLog) {
            return;
        }

        SoundEvent chosenSound = playCreatureSound ? creatureSounds.get(random.nextInt(creatureSounds.size())) : null;
        float pitch = 0.9f + random.nextFloat() * 0.2f;

        Minecraft.getInstance().execute(() -> {
            ClientLevel level = Minecraft.getInstance().level;
            if (level == null) {
                return;
            }
            AbstractClientPlayer speaker = findPlayer(level, speakerId);
            if (chosenSound != null && speaker != null) {
                level.playLocalSound(speaker, chosenSound, SoundSource.PLAYERS, 1.0f, pitch);
            }
            if (shouldLog) {
                String name = speaker != null ? speaker.getGameProfile().getName() : speakerId.toString().substring(0, 8);
                String message = "[LangSystem/voice] " + name + " -> \"" + language.displayName()
                        + "\", ваш прогресс " + myProgress + "%, сила эффекта " + Math.round(strength * 100) + "%"
                        + (chosenSound != null ? " — играю звук существа" : " — приглушаю голос");
                listener.displayClientMessage(Component.literal(message), false);
            }
        });
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

    private boolean isCreatureSoundDue(UUID speakerId) {
        long now = System.currentTimeMillis();
        Long last = lastCreatureSoundAt.get(speakerId);
        if (last != null && now - last < CREATURE_SOUND_COOLDOWN_MS) {
            return false;
        }
        lastCreatureSoundAt.put(speakerId, now);
        return true;
    }

    private boolean isDebugMessageDue(UUID speakerId) {
        long now = System.currentTimeMillis();
        Long last = lastDebugMessageAt.get(speakerId);
        if (last != null && now - last < DEBUG_MESSAGE_COOLDOWN_MS) {
            return false;
        }
        lastDebugMessageAt.put(speakerId, now);
        return true;
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
