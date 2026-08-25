package com.langsystem.client;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.font.TextFieldHelper;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.SignRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.Material;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.properties.WoodType;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Экран записи таблички, устроенный по образцу ванильного {@code SignEditScreen}:
 * прозрачный фон, текст с настоящим курсором/выделением редактируется прямо "на весу"
 * (через {@link TextFieldHelper}, тот же класс, что использует ванильная табличка),
 * стрелки/Enter переключают строку, одна кнопка "Готово" внизу. Строк 4, максимальная
 * ширина строки — 90 пикселей, как у настоящей ванильной таблички
 * ({@code SignBlockEntity.MAX_TEXT_LINE_WIDTH}).
 *
 * <p>Открывается только для перехваченных ванильных табличек (см.
 * {@code mixin.LocalPlayerMixin}/{@code mixin.SignBlockMixin}), поэтому {@link VanillaBackground}
 * передаётся всегда — позади текста рисуется НАСТОЯЩАЯ 3D-модель таблички нужной породы
 * дерева, теми же числами позиционирования, что и у ванильного {@code SignEditScreen}
 * (проверено по декомпилированным исходникам). Параметр остаётся nullable на случай
 * будущего вызова без фона.</p>
 */
public final class LanguageSignEditScreen extends Screen {

    /** Порода дерева ванильной таблички + стоит ли она (значит, виден столбик) или висит на стене. */
    public record VanillaBackground(WoodType woodType, boolean standing) {
    }

    private static final int LINES = 4;
    private static final int MAX_LINE_WIDTH = 90;
    private static final int TEXT_LINE_HEIGHT = 10;
    private static final float SIGN_MODEL_SCALE = 62.500004F;
    private static final float TEXT_SCALE = 0.9765628F;

    /** Породы с достаточно тёмной текстурой, чтобы чёрный текст на ней было не видно. */
    private static final Set<String> DARK_WOODS = Set.of("spruce", "dark_oak", "mangrove", "crimson", "warped");

    /** Цвет текста таблички: белый на тёмных породах, чёрный на светлых (как в ваниле); без фона — белый (тёмный прозрачный фон экрана). */
    static int textColorFor(@Nullable WoodType wood) {
        if (wood == null) {
            return 0xFFFFFF;
        }
        return DARK_WOODS.contains(wood.name()) ? 0xFFFFFF : 0x000000;
    }

    private final Consumer<String> onSave;
    @Nullable
    private final VanillaBackground background;
    private final String[] messages = new String[LINES];
    private final int textColor;
    private int line;
    private int frame;
    private TextFieldHelper field;
    @Nullable
    private SignRenderer.SignModel signModel;

    public LanguageSignEditScreen(Consumer<String> onSave) {
        this(onSave, null);
    }

    public LanguageSignEditScreen(Consumer<String> onSave, @Nullable VanillaBackground background) {
        super(Component.literal("Запись таблички"));
        this.onSave = onSave;
        this.background = background;
        this.textColor = background != null ? textColorFor(background.woodType()) : 0xFFFFFF;
        Arrays.fill(messages, "");
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onDone())
                .bounds(width / 2 - 100, (int) (signCenterY() + 90), 200, 20).build());
        field = new TextFieldHelper(
                () -> messages[line],
                s -> messages[line] = s,
                TextFieldHelper.createClipboardGetter(minecraft),
                TextFieldHelper.createClipboardSetter(minecraft),
                s -> font.width(s) <= MAX_LINE_WIDTH
        );
        if (background != null) {
            signModel = SignRenderer.createSignModel(minecraft.getEntityModels(), background.woodType());
        }
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

    /** Вертикальный центр таблички/текста — чуть выше геометрического центра экрана. */
    private float signCenterY() {
        return height / 2f - 20f;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.drawCenteredString(font, title, width / 2, (int) (signCenterY() - 60), 0xFFFFFF);

        if (background != null && signModel != null) {
            Lighting.setupForFlatItems();
            renderVanillaSign(graphics);
            Lighting.setupFor3DItems();
        } else {
            renderFlatLines(graphics);
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderTransparentBackground(graphics);
    }

    // --- ванильная 3D-табличка (числа взяты из SignEditScreen/AbstractSignEditScreen) ---

    private void renderVanillaSign(GuiGraphics graphics) {
        graphics.pose().pushPose();
        graphics.pose().translate((float) width / 2.0F, signCenterY(), 50.0F);
        if (!background.standing()) {
            graphics.pose().translate(0.0F, 35.0F, 0.0F);
        }

        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 31.0F, 0.0F);
        graphics.pose().scale(SIGN_MODEL_SCALE, SIGN_MODEL_SCALE, -SIGN_MODEL_SCALE);
        Material material = Sheets.getSignMaterial(background.woodType());
        VertexConsumer vertexConsumer = material.buffer(graphics.bufferSource(), signModel::renderType);
        signModel.stick.visible = background.standing();
        signModel.root.render(graphics.pose(), vertexConsumer, 15728880, OverlayTexture.NO_OVERLAY);
        graphics.pose().popPose();

        graphics.pose().translate(0.0F, 0.0F, 4.0F);
        graphics.pose().scale(TEXT_SCALE, TEXT_SCALE, TEXT_SCALE);
        renderLinesAt(graphics, 0, 0);
        graphics.pose().popPose();
    }

    // --- без фона (сейчас не используется, background всегда передан — оставлено для надёжности) ---

    private void renderFlatLines(GuiGraphics graphics) {
        renderLinesAt(graphics, width / 2, (int) signCenterY());
    }

    /** Общий рендер 4 строк с курсором/выделением; (originX, originY) — точка, вокруг которой строки центрируются по вертикали. */
    private void renderLinesAt(GuiGraphics graphics, int originX, int originY) {
        boolean cursorVisible = frame / 6 % 2 == 0;
        int cursorPos = field.getCursorPos();
        int selectionPos = field.getSelectionPos();
        int half = LINES * TEXT_LINE_HEIGHT / 2;

        for (int i = 0; i < LINES; i++) {
            String s = messages[i];
            int x = originX - font.width(s) / 2;
            int y = originY + i * TEXT_LINE_HEIGHT - half;
            graphics.drawString(font, s, x, y, textColor, false);

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
                    graphics.drawString(font, "_", cx, y, textColor, false);
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
