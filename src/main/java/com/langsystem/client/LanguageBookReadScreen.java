package com.langsystem.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

import java.util.List;

/**
 * Экран для чтения языковой книги. Показанный текст уже полностью посчитан заранее
 * (см. {@code ClientBookScreens}) — этот экран лишь рисует его поверх настоящего
 * ванильного фона книги ({@code textures/gui/book.png}), теми же числами, что и у
 * ванильного {@code BookViewScreen} (проверено по декомпилированным исходникам).
 */
public final class LanguageBookReadScreen extends Screen {

    private static final ResourceLocation BOOK_TEXTURE = ResourceLocation.withDefaultNamespace("textures/gui/book.png");
    private static final int IMAGE_WIDTH = 192;
    private static final int IMAGE_HEIGHT = 192;
    private static final int TEXT_WIDTH = 114;
    private static final int TEXT_HEIGHT = 128;
    private static final int PAGE_TEXT_X_OFFSET = 36;
    private static final int PAGE_TEXT_Y_OFFSET = 30;
    private static final int LINE_HEIGHT = 9;
    private static final int TEXT_COLOR = 0x000000;

    private final String bodyText;
    private List<FormattedCharSequence> lines = List.of();

    public LanguageBookReadScreen(String languageTitle, String bodyText) {
        super(Component.literal(languageTitle));
        this.bodyText = bodyText;
    }

    private int backgroundX() {
        return (width - IMAGE_WIDTH) / 2;
    }

    private int backgroundY() {
        return (height - IMAGE_HEIGHT) / 2;
    }

    @Override
    protected void init() {
        lines = font.split(FormattedText.of(bodyText), TEXT_WIDTH);
        int buttonY = backgroundY() + IMAGE_HEIGHT + 2;
        addRenderableWidget(Button.builder(Component.literal("Закрыть"), b -> onClose())
                .bounds(width / 2 - 100, buttonY, 200, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        int x = backgroundX() + PAGE_TEXT_X_OFFSET;
        int y = backgroundY() + PAGE_TEXT_Y_OFFSET;
        int maxLines = Math.min(TEXT_HEIGHT / LINE_HEIGHT, lines.size());
        for (int i = 0; i < maxLines; i++) {
            graphics.drawString(font, lines.get(i), x, y + i * LINE_HEIGHT, TEXT_COLOR, false);
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderTransparentBackground(graphics);
        graphics.blit(BOOK_TEXTURE, backgroundX(), backgroundY(), 0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
