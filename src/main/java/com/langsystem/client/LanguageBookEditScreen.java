package com.langsystem.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Экран записи языковой книги — вместо голого списка текстовых полей рисует настоящий
 * ванильный фон книги с пером ({@code textures/gui/book.png}), теми же числами
 * позиционирования, что и у ванильного {@code BookEditScreen}/{@code BookViewScreen}
 * (проверено по декомпилированным исходникам): фон 192x192 в {@code ((width-192)/2, 2)},
 * текстовая колонка шириной 114 начинается на 36 правее и 30 ниже угла фона. Полей 8,
 * каждое высотой 16 — ровно заполняют текстовую область (128 px) страницы.
 */
public final class LanguageBookEditScreen extends Screen {

    private static final ResourceLocation BOOK_TEXTURE = ResourceLocation.withDefaultNamespace("textures/gui/book.png");
    private static final int IMAGE_WIDTH = 192;
    private static final int IMAGE_HEIGHT = 192;
    private static final int TEXT_WIDTH = 114;
    private static final int PAGE_TEXT_X_OFFSET = 36;
    private static final int PAGE_TEXT_Y_OFFSET = 30;
    private static final int LINES = 8;
    private static final int LINE_HEIGHT = 16;
    private static final int TEXT_COLOR = 0x000000;

    private final Consumer<String> onSave;
    private final List<EditBox> fields = new ArrayList<>();

    public LanguageBookEditScreen(Consumer<String> onSave) {
        super(Component.literal("Письмо на текущем языке"));
        this.onSave = onSave;
    }

    private int backgroundX() {
        return (width - IMAGE_WIDTH) / 2;
    }

    @Override
    protected void init() {
        fields.clear();
        int x = backgroundX() + PAGE_TEXT_X_OFFSET;
        int y = 2 + PAGE_TEXT_Y_OFFSET;

        for (int i = 0; i < LINES; i++) {
            EditBox field = new EditBox(font, x, y + i * LINE_HEIGHT, TEXT_WIDTH, LINE_HEIGHT,
                    Component.literal("Строка " + (i + 1)));
            field.setBordered(false);
            field.setTextColor(TEXT_COLOR);
            field.setMaxLength(200);
            addRenderableWidget(field);
            fields.add(field);
        }
        setInitialFocus(fields.get(0));

        addRenderableWidget(Button.builder(Component.literal("Записать"), b -> save())
                .bounds(width / 2 - 100, 196, 98, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Отмена"), b -> onClose())
                .bounds(width / 2 + 2, 196, 98, 20).build());
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderTransparentBackground(graphics);
        graphics.blit(BOOK_TEXTURE, backgroundX(), 2, 0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);
    }

    private void save() {
        StringBuilder sb = new StringBuilder();
        for (EditBox field : fields) {
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append(field.getValue());
        }
        String text = sb.toString().strip();
        if (!text.isEmpty()) {
            onSave.accept(text);
        }
        onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
