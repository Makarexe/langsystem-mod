package com.langsystem.voicechat;

import com.langsystem.LangSystemMod;
import com.langsystem.Language;
import com.langsystem.client.ClientLanguageState;
import de.maxhenkel.voicechat.api.ForgeVoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.events.ClientReceiveSoundEvent;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

import java.util.UUID;

/**
 * Опциональная интеграция с Simple Voice Chat: если слушатель недостаточно знает
 * язык, на котором сейчас говорит собеседник (прогресс синхронизируется отдельно, см.
 * {@link com.langsystem.network.SyncSpeakerLanguagePayload}), его голос приглушается —
 * тем сильнее, чем ниже прогресс слушателя в этом языке. Свободное владение (100%) —
 * без изменений.
 *
 * <p>Событие {@code ClientReceiveSoundEvent} у Simple Voice Chat срабатывает на
 * КАЖДОМ слушающем клиенте отдельно, ещё до проигрывания звука — то есть эффект можно
 * сделать разным для разных слушателей одного и того же голоса, точно так же, как с
 * текстом в чате (там у каждого читателя свой {@code RuneCipher.read}).</p>
 *
 * <p>Этот класс — единственное место в моде, которое напрямую ссылается на классы API
 * Simple Voice Chat. Обращается к нему только сам Simple Voice Chat, сканируя мод на
 * аннотацию {@link ForgeVoicechatPlugin} — если он не установлен, никто этот класс не
 * трогает, и мод спокойно работает без него (зависимость в {@code neoforge.mods.toml}
 * помечена {@code optional}).</p>
 */
@ForgeVoicechatPlugin
public final class LangSystemVoicechatPlugin implements VoicechatPlugin {

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
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        UUID speakerId = entitySound.getEntityId();
        if (speakerId.equals(player.getUUID())) {
            return; // свой собственный голос не приглушаем
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
        event.setRawAudio(muffle(audio, strength));
    }

    /**
     * Однополюсный фильтр нижних частот (экспоненциальное сглаживание) — чем сильнее
     * {@code strength}, тем сильнее "смазан" сигнал (звучит глухо/невнятно, как через
     * стену) плюс небольшое снижение громкости. ПЕРВАЯ ВЕРСИЯ, качество не проверялось
     * вживую (нет способа протестировать звук без реального Simple Voice Chat и двух
     * живых собеседников) — коэффициенты ниже почти наверняка потребуют подстройки по
     * ощущениям в игре.
     */
    private static short[] muffle(short[] input, float strength) {
        float alpha = 1f - strength * 0.9f;
        float volume = 1f - strength * 0.3f;
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
