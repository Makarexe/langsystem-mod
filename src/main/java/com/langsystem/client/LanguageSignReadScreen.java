package com.langsystem.client;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.SignRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.Material;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * Экран чтения таблички — то же самое устройство, что и {@link LanguageSignEditScreen}
 * (тот же реальный 3D-фон ванильной таблички при наличии {@link LanguageSignEditScreen.VanillaBackground}),
 * но без редактирования: текст уже посчитан персонально под открывшего заранее
 * (см. {@link ClientSignScreens} / {@code mixin.LocalPlayerMixin}), эта строка лишь
 * рисует его и даёт закрыть экран.
 */
public final class LanguageSignReadScreen extends Screen {

    private static final int LINES = 4;
    private static final int TEXT_LINE_HEIGHT = 10;
    private static final float SIGN_MODEL_SCALE = 62.500004F;
    private static final float TEXT_SCALE = 0.9765628F;

    @Nullable
    private final LanguageSignEditScreen.VanillaBackground background;
    private final String[] lines;
    @Nullable
    private SignRenderer.SignModel signModel;

    public LanguageSignReadScreen(String languageTitle, String bodyText) {
        this(languageTitle, bodyText, null);
    }

    public LanguageSignReadScreen(String languageTitle, String bodyText,
                                   @Nullable LanguageSignEditScreen.VanillaBackground background) {
        super(Component.literal(languageTitle));
        this.background = background;
        String[] split = bodyText.split("\n", -1);
        this.lines = new String[LINES];
        for (int i = 0; i < LINES; i++) {
            lines[i] = i < split.length ? split[i] : "";
        }
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(Component.literal("Закрыть"), b -> onClose())
                .bounds(width / 2 - 100, (int) (signCenterY() + 90), 200, 20).build());
        if (background != null) {
            signModel = SignRenderer.createSignModel(minecraft.getEntityModels(), background.woodType());
        }
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

    private void renderFlatLines(GuiGraphics graphics) {
        renderLinesAt(graphics, width / 2, (int) signCenterY());
    }

    private void renderLinesAt(GuiGraphics graphics, int originX, int originY) {
        int half = LINES * TEXT_LINE_HEIGHT / 2;
        for (int i = 0; i < LINES; i++) {
            String s = lines[i];
            int x = originX - font.width(s) / 2;
            int y = originY + i * TEXT_LINE_HEIGHT - half;
            graphics.drawString(font, s, x, y, 0xFFFFFF, false);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
