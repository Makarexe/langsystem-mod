package com.langsystem.client;

import com.langsystem.Language;
import com.langsystem.block.LanguageSignBlock;
import com.langsystem.block.LanguageSignBlockEntity;
import com.langsystem.data.ModDataComponents;
import com.langsystem.util.RuneCipher;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Рисует сообщение прямо в мире, как у ванильной таблички — не нужно кликать, чтобы
 * что-то увидеть. Но в отличие от версии, где текст сразу подгонялся под наблюдателя,
 * здесь на весу всегда видно ОДИНАКОВЫЙ для всех "фоновый" вид — {@link RuneCipher#produce},
 * то есть с поправкой на грамотность ПИСАВШЕГО, но БЕЗ поправки на прогресс конкретного
 * читателя. Полная расшифровка "под себя" ({@link RuneCipher#read}) — только через клик
 * (см. {@link ClientSignScreens}). Лицевая и обратная стороны независимы — как у
 * настоящей вывески.
 */
public final class LanguageSignRenderer implements BlockEntityRenderer<LanguageSignBlockEntity> {

    private static final int TEXT_COLOR = 0xFF000000;

    public LanguageSignRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(LanguageSignBlockEntity sign, float partialTick, PoseStack poseStack,
                        MultiBufferSource bufferSource, int packedLight, int packedOverlay) {
        ModDataComponents.LanguageText front = sign.content(true);
        ModDataComponents.LanguageText back = sign.content(false);
        if (front == null && back == null) {
            return;
        }

        Font font = Minecraft.getInstance().font;
        Direction facing = sign.getBlockState().getValue(LanguageSignBlock.FACING);
        float yRot = modelYRotationFor(facing);

        poseStack.pushPose();
        poseStack.translate(0.5, 0.719, 0.5);
        if (front != null) {
            renderSide(poseStack, bufferSource, font, front, packedLight, yRot);
        }
        if (back != null) {
            renderSide(poseStack, bufferSource, font, back, packedLight, yRot + 180);
        }
        poseStack.popPose();
    }

    /** Тот же поворот, что задан в blockstates/language_sign.json для каждого facing. */
    private static float modelYRotationFor(Direction facing) {
        return switch (facing) {
            case NORTH -> 180;
            case WEST -> 270;
            case EAST -> 90;
            default -> 0; // SOUTH
        };
    }

    private static void renderSide(PoseStack poseStack, MultiBufferSource bufferSource, Font font,
                                    ModDataComponents.LanguageText content, int packedLight, float yRotDegrees) {
        Language language = Language.byId(content.languageId()).orElse(null);
        if (language == null) {
            return;
        }
        String ambient = RuneCipher.produce(content.rawText(), language, content.writerProgress());
        String[] lines = ambient.split("\n", -1);

        int maxPixelWidth = 1;
        for (String line : lines) {
            maxPixelWidth = Math.max(maxPixelWidth, font.width(line));
        }
        float maxWidthWorld = 0.85f;
        float maxHeightWorld = 0.34f;
        float lineHeight = font.lineHeight;
        float scaleByWidth = maxWidthWorld / maxPixelWidth;
        float scaleByHeight = maxHeightWorld / (lines.length * lineHeight);
        float scale = Math.min(0.013f, Math.min(scaleByWidth, scaleByHeight));

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(yRotDegrees));
        poseStack.translate(0, 0, -0.13);
        poseStack.scale(scale, -scale, scale);

        float totalHeight = lines.length * lineHeight;
        float y = -totalHeight / 2f;
        for (String line : lines) {
            float w = font.width(line);
            font.drawInBatch(line, -w / 2f, y, TEXT_COLOR, false, poseStack.last().pose(), bufferSource,
                    Font.DisplayMode.NORMAL, 0, packedLight);
            y += lineHeight;
        }

        poseStack.popPose();
    }
}
