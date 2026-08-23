package com.langsystem.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Экран для чтения языковой книги/таблички. Показанный текст уже полностью посчитан
 * заранее (см. {@code ClientBookScreens}/{@code ClientSignScreens}) — этот экран лишь
 * рисует его, никакой логики понимания языка здесь нет.
 */
public final class LanguageBookReadScreen extends Screen {

    private static final int TEXT_WIDTH = 280;

    private final String bodyText;
    private MultiLineLabel body = MultiLineLabel.EMPTY;
    private int textStartY;

    public LanguageBookReadScreen(String languageTitle, String bodyText) {
        super(Component.literal(languageTitle));
        this.bodyText = bodyText;
    }

    @Override
    protected void init() {
        body = MultiLineLabel.create(font, Component.literal(bodyText), TEXT_WIDTH);
        int totalHeight = body.getLineCount() * font.lineHeight;
        textStartY = Math.max(40, (height - totalHeight) / 2);

        addRenderableWidget(Button.builder(Component.literal("Закрыть"), b -> onClose())
                .bounds((width - 100) / 2, height - 30, 100, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, Math.max(10, textStartY - 24), 0xFFFFFF);
        body.renderCentered(graphics, width / 2, textStartY);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
