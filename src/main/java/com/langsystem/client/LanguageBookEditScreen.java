package com.langsystem.client;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Простой построчный редактор текста (используется и книгой, и табличкой — источник
 * не важен, экран лишь собирает введённый текст и отдаёт его вызывающему коду).
 * Записывается текст на языке, который у игрока сейчас выбран как текущий (см. GUI
 * выбора языка) — здесь это не показывается отдельно, но само название экрана
 * подсказывает, что писать стоит на нём.
 */
public final class LanguageBookEditScreen extends Screen {

    private static final int LINES = 8;
    private static final int FIELD_WIDTH = 300;
    private static final int FIELD_HEIGHT = 16;
    private static final int SPACING = 4;

    private final Consumer<String> onSave;
    private final List<EditBox> fields = new ArrayList<>();

    public LanguageBookEditScreen(Consumer<String> onSave) {
        super(Component.literal("Письмо на текущем языке"));
        this.onSave = onSave;
    }

    @Override
    protected void init() {
        fields.clear();
        int totalHeight = LINES * (FIELD_HEIGHT + SPACING) + 60;
        int startY = Math.max(24, (height - totalHeight) / 2);
        int x = (width - FIELD_WIDTH) / 2;

        addRenderableWidget(new StringWidget(x, startY - 18, FIELD_WIDTH, 14, title, font).alignCenter());

        int y = startY;
        for (int i = 0; i < LINES; i++) {
            EditBox field = new EditBox(font, x, y, FIELD_WIDTH, FIELD_HEIGHT, Component.literal("Строка " + (i + 1)));
            field.setMaxLength(200);
            addRenderableWidget(field);
            fields.add(field);
            y += FIELD_HEIGHT + SPACING;
        }

        int buttonY = y + 6;
        int buttonWidth = (FIELD_WIDTH - SPACING) / 2;
        addRenderableWidget(Button.builder(Component.literal("Записать"), b -> save())
                .bounds(x, buttonY, buttonWidth, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Отмена"), b -> onClose())
                .bounds(x + buttonWidth + SPACING, buttonY, buttonWidth, 20).build());
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
