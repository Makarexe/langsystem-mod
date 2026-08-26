package com.langsystem.client;

import com.langsystem.Language;
import com.langsystem.network.SetLanguagePayload;
import com.langsystem.util.SpeechFluency;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/**
 * Экран выбора текущего языка речи. Показывает только языки, которые игрок хоть
 * немного знает (прогресс > 0), с указанием процента понимания.
 */
public final class LanguageSelectScreen extends Screen {

    private static final int BUTTON_WIDTH = 260;
    private static final int BUTTON_HEIGHT = 20;
    private static final int SPACING = 4;

    public LanguageSelectScreen() {
        super(Component.translatable("langsystem.gui.language_select.title"));
    }

    @Override
    protected void init() {
        List<ClientLanguageState.Entry> entries = ClientLanguageState.entries();
        String current = ClientLanguageState.current();

        int rows = Math.max(entries.size(), 1);
        int totalHeight = rows * (BUTTON_HEIGHT + SPACING) + BUTTON_HEIGHT + 30;
        int startY = Math.max(30, (height - totalHeight) / 2);
        int x = (width - BUTTON_WIDTH) / 2;

        addRenderableWidget(new StringWidget(x, startY - 20, BUTTON_WIDTH, 16, title, font).alignCenter());

        int y = startY;
        if (entries.isEmpty()) {
            addRenderableWidget(new StringWidget(x, y, BUTTON_WIDTH, 16,
                    Component.translatable("langsystem.gui.language_select.none_known"), font).alignCenter());
            y += BUTTON_HEIGHT + SPACING;
        }

        for (ClientLanguageState.Entry entry : entries) {
            Language language = Language.byId(entry.languageId()).orElse(null);
            if (language == null) {
                continue;
            }
            boolean isCurrent = entry.languageId().equals(current);
            Component entryLabel = Component.translatable("langsystem.gui.language_select.entry",
                    language.translatable(), entry.progress(), SpeechFluency.translatableLabel(entry.progress()));
            Component label = isCurrent
                    ? Component.translatable("langsystem.gui.language_select.current_suffix", entryLabel)
                    : entryLabel;
            Button button = Button.builder(label, b -> {
                        PacketDistributor.sendToServer(new SetLanguagePayload(language.id()));
                        onClose();
                    })
                    .bounds(x, y, BUTTON_WIDTH, BUTTON_HEIGHT)
                    .build();
            button.active = !isCurrent;
            addRenderableWidget(button);
            y += BUTTON_HEIGHT + SPACING;
        }

        addRenderableWidget(Button.builder(Component.translatable("langsystem.gui.close"), b -> onClose())
                .bounds(x, y + 6, BUTTON_WIDTH, BUTTON_HEIGHT)
                .build());
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
