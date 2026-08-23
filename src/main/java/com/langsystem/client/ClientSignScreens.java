package com.langsystem.client;

import com.langsystem.Language;
import com.langsystem.block.LanguageSignBlockEntity;
import com.langsystem.data.ModDataComponents;
import com.langsystem.network.SaveLanguageSignPayload;
import com.langsystem.util.RuneCipher;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Пустая сторона таблички -> экран записи ({@link LanguageSignEditScreen}, как у
 * ванильной таблички). Уже подписанная сторона -> экран чтения ({@link LanguageSignReadScreen}),
 * персонально посчитанный под текущего игрока — прямо в мире (см. {@link LanguageSignRenderer})
 * всегда виден только "сырой" текст, без учёта прогресса читателя.
 */
public final class ClientSignScreens {

    public static void open(BlockPos pos, LanguageSignBlockEntity sign, boolean isFrontText) {
        ModDataComponents.LanguageText content = sign.content(isFrontText);
        if (content == null) {
            Minecraft.getInstance().setScreen(new LanguageSignEditScreen(
                    text -> PacketDistributor.sendToServer(new SaveLanguageSignPayload(pos, isFrontText, text))
            ));
            return;
        }
        Language language = Language.byId(content.languageId()).orElse(Language.COMMON);
        int myProgress = ClientLanguageState.progressOf(language.id());
        String shown = RuneCipher.read(content.rawText(), language, content.writerProgress(), myProgress);
        Minecraft.getInstance().setScreen(new LanguageSignReadScreen(language.displayName(), shown));
    }

    private ClientSignScreens() {
    }
}
