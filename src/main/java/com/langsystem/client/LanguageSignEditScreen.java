package com.langsystem.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.font.TextFieldHelper;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.Arrays;
import java.util.function.Consumer;

/**
 * Экран записи таблички, устроенный по образцу ванильного {@code SignEditScreen}:
 * прозрачный фон вместо панели, текст с настоящим курсором/выделением редактируется
 * прямо "на весу" (через {@link TextFieldHelper}, тот же класс, что использует
 * ванильная табличка), стрелки/Enter переключают строку, одна кнопка "Готово" внизу.
 * В отличие от ванильной таблички (жёстко 4 строки) здесь тоже 4 — специально для
 * единообразия с оригиналом, хотя сам движок табличек мода поддерживает больше строк
 * (см. {@link LanguageBookEditScreen}, где для книги строк 8).
 */
public final class LanguageSignEditScreen extends Screen {

    private static final int LINES = 4;
    private static final int MAX_LINE_WIDTH = 200;

    private final Consumer<String> onSave;
    private final String[] messages = new String[LINES];
    private int line;
    private int frame;
    private TextFieldHelper field;

    public LanguageSignEditScreen(Consumer<String> onSave) {
        super(Component.literal("Запись таблички"));
        this.onSave = onSave;
        Arrays.fill(messages, "");
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onDone())
                .bounds(width / 2 - 100, height / 4 + 144, 200, 20).build());
        field = new TextFieldHelper(
                () -> messages[line],
                s -> messages[line] = s,
                TextFieldHelper.createClipboardGetter(minecraft),
                TextFieldHelper.createClipboardSetter(minecraft),
                s -> font.width(s) <= MAX_LINE_WIDTH
        );
    }

    @Override
    public void tick() {
        frame++;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 265) { // UP
            line = Math.floorMod(line - 1, LINES);
            field.setCursorToEnd();
            return true;
        } else if (keyCode == 264 || keyCode == 257 || keyCode == 335) { // DOWN / ENTER / NUMPAD_ENTER
            line = Math.floorMod(line + 1, LINES);
            field.setCursorToEnd();
            return true;
        }
        return field.keyPressed(keyCode) || super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        field.charTyped(codePoint);
        return true;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, 40, 0xFFFFFF);
        renderLines(graphics);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderTransparentBackground(graphics);
    }

    private void renderLines(GuiGraphics graphics) {
        int lineHeight = font.lineHeight + 2;
        int startY = (height - LINES * lineHeight) / 2;
        boolean cursorVisible = frame / 6 % 2 == 0;
        int cursorPos = field.getCursorPos();
        int selectionPos = field.getSelectionPos();

        for (int i = 0; i < LINES; i++) {
            String s = messages[i];
            int x = width / 2 - font.width(s) / 2;
            int y = startY + i * lineHeight;
            graphics.drawString(font, s, x, y, 0xFFFFFF, false);

            if (i != line) {
                continue;
            }
            if (selectionPos != cursorPos) {
                int lo = Math.min(cursorPos, selectionPos);
                int hi = Math.max(cursorPos, selectionPos);
                int hx1 = x + font.width(s.substring(0, lo));
                int hx2 = x + font.width(s.substring(0, hi));
                graphics.fill(hx1, y - 1, hx2, y + font.lineHeight, 0x8057A3FF);
            }
            if (cursorVisible && cursorPos >= 0) {
                int cx = x + font.width(s.substring(0, Math.max(Math.min(cursorPos, s.length()), 0)));
                if (cursorPos >= s.length()) {
                    graphics.drawString(font, "_", cx, y, 0xFFFFFF, false);
                } else {
                    graphics.fill(cx, y - 1, cx + 1, y + font.lineHeight, 0xFFD0D0D0);
                }
            }
        }
    }

    private void onDone() {
        minecraft.setScreen(null);
    }

    @Override
    public void removed() {
        StringBuilder sb = new StringBuilder();
        for (String s : messages) {
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append(s);
        }
        String text = sb.toString().strip();
        if (!text.isEmpty()) {
            onSave.accept(text);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
