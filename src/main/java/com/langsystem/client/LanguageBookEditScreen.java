package com.langsystem.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.font.TextFieldHelper;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Arrays;
import java.util.function.Consumer;

/**
 * Экран записи языковой книги — настоящий ванильный фон книги с пером
 * ({@code textures/gui/book.png}, те же числа позиционирования, что и у ванильного
 * {@code BookEditScreen}/{@code BookViewScreen}), но по вертикали сцентрирован в окне
 * (как табличка через {@code signCenterY()}), а не прибит к самому верху, как в ваниле.
 *
 * <p>Строк 8, каждая не шире видимой колонки страницы (114 px, как у ванильной книги).
 * При наборе текста, если очередной символ уже не помещается в строку, курсор САМ
 * переходит на следующую строку (перенос, как в обычном текстовом редакторе); если и
 * последняя из 8 строк уже заполнена — дальнейший ввод просто блокируется, страница
 * закончилась.</p>
 */
public final class LanguageBookEditScreen extends Screen {

    private static final ResourceLocation BOOK_TEXTURE = ResourceLocation.withDefaultNamespace("textures/gui/book.png");
    private static final int IMAGE_WIDTH = 192;
    private static final int IMAGE_HEIGHT = 192;
    private static final int TEXT_WIDTH = 114;
    private static final int PAGE_TEXT_X_OFFSET = 36;
    private static final int PAGE_TEXT_Y_OFFSET = 30;
    private static final int LINES = 8;
    private static final int LINE_HEIGHT = 10;
    private static final int TEXT_COLOR = 0x000000;

    private final Consumer<String> onSave;
    private final String[] messages = new String[LINES];
    private int line;
    private int frame;
    private TextFieldHelper field;

    public LanguageBookEditScreen(Consumer<String> onSave) {
        super(Component.translatable("langsystem.gui.book_edit.title"));
        this.onSave = onSave;
        Arrays.fill(messages, "");
    }

    private int backgroundX() {
        return (width - IMAGE_WIDTH) / 2;
    }

    private int backgroundY() {
        return (height - IMAGE_HEIGHT) / 2;
    }

    @Override
    protected void init() {
        field = new TextFieldHelper(
                () -> messages[line],
                s -> messages[line] = s,
                TextFieldHelper.createClipboardGetter(minecraft),
                TextFieldHelper.createClipboardSetter(minecraft),
                s -> font.width(s) <= TEXT_WIDTH
        );

        int buttonY = backgroundY() + IMAGE_HEIGHT + 2;
        addRenderableWidget(Button.builder(Component.translatable("langsystem.gui.book_edit.save"), b -> save())
                .bounds(width / 2 - 100, buttonY, 98, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("langsystem.gui.book_edit.cancel"), b -> onClose())
                .bounds(width / 2 + 2, buttonY, 98, 20).build());
    }

    @Override
    public void tick() {
        frame++;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        String current = messages[line];
        int cursorPos = field.getCursorPos();
        String candidate = current.substring(0, cursorPos) + codePoint + current.substring(cursorPos);
        if (font.width(candidate) <= TEXT_WIDTH) {
            field.charTyped(codePoint);
            return true;
        }
        if (cursorPos != current.length()) {
            return true; // не помещается посреди строки — просто игнорируем символ
        }
        if (line + 1 >= LINES) {
            return true; // страница закончилась — дальше не пишем
        }
        line++;
        field.setCursorToStart();
        if (font.width(String.valueOf(codePoint)) <= TEXT_WIDTH) {
            field.charTyped(codePoint);
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 259 && field.getCursorPos() == 0 && line > 0) { // BACKSPACE в начале строки
            line--;
            field.setCursorToEnd();
            return true;
        }
        if (keyCode == 265 && line > 0) { // UP
            line--;
            field.setCursorToEnd();
            return true;
        }
        if ((keyCode == 264 || keyCode == 257 || keyCode == 335) && line + 1 < LINES) { // DOWN / ENTER
            line++;
            field.setCursorToStart();
            return true;
        }
        return field.keyPressed(keyCode) || super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);

        boolean cursorVisible = frame / 6 % 2 == 0;
        int cursorPos = field.getCursorPos();
        int selectionPos = field.getSelectionPos();
        int x = backgroundX() + PAGE_TEXT_X_OFFSET;
        int y = backgroundY() + PAGE_TEXT_Y_OFFSET;

        for (int i = 0; i < LINES; i++) {
            String s = messages[i];
            int ly = y + i * LINE_HEIGHT;
            graphics.drawString(font, s, x, ly, TEXT_COLOR, false);

            if (i != line) {
                continue;
            }
            if (selectionPos != cursorPos) {
                int lo = Math.min(cursorPos, selectionPos);
                int hi = Math.max(cursorPos, selectionPos);
                int hx1 = x + font.width(s.substring(0, lo));
                int hx2 = x + font.width(s.substring(0, hi));
                graphics.fill(hx1, ly - 1, hx2, ly + font.lineHeight, 0x8057A3FF);
            }
            if (cursorVisible && cursorPos >= 0) {
                int cx = x + font.width(s.substring(0, Math.max(Math.min(cursorPos, s.length()), 0)));
                if (cursorPos >= s.length()) {
                    graphics.drawString(font, "_", cx, ly, TEXT_COLOR, false);
                } else {
                    graphics.fill(cx, ly - 1, cx + 1, ly + font.lineHeight, 0xFF000000);
                }
            }
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderTransparentBackground(graphics);
        graphics.blit(BOOK_TEXTURE, backgroundX(), backgroundY(), 0, 0, IMAGE_WIDTH, IMAGE_HEIGHT);
    }

    private void save() {
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
        onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
