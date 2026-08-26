package com.langsystem.mixin;

import com.langsystem.Language;
import com.langsystem.data.ModAttachments;
import com.langsystem.data.ModDataComponents;
import com.langsystem.util.RuneCipher;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.SignRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Если у ванильной таблички есть языковой attachment на этой стороне ({@link ModAttachments#VANILLA_SIGN_TEXT}
 * — то есть она подписана через нашу систему, см. {@link LocalPlayerMixin}), рисуем
 * вместо обычного текста то, что реально написано (уже с поправкой на грамотность
 * ПИСАВШЕГО, но БЕЗ поправки на прогресс конкретного читателя — та же логика, что и у
 * {@link com.langsystem.client.LanguageSignRenderer}, полная расшифровка "под себя"
 * доступна только через клик) и отменяем ванильный рендер текста — сама деревянная
 * модель таблички (столбик/доска) рисуется как обычно, не трогаем. Цвет и свечение
 * (крашение/светящиеся чернила) по-прежнему берутся из ванильного {@link SignText} —
 * работают как обычно.
 * Смещение и масштаб текста берутся не захардкоженными числами, а через
 * {@code getTextOffset()}/{@code getSignTextRenderScale()} — у {@code HangingSignRenderer}
 * (табличка на цепях) они переопределены под другую геометрию (другой offset, масштаб
 * 0.9 вместо 0.6666667), и благодаря обычному виртуальному вызову эти методы сами
 * возвращают правильные числа для конкретного подкласса рендерера — жёстко фиксированные
 * значения обычной таблички ломали позицию текста на висячей.
 */
@Mixin(SignRenderer.class)
public abstract class SignRendererMixin {

    @Shadow
    @Final
    private Font font;

    // Табличка на цепях (HangingSignRenderer) переопределяет и смещение, и масштаб
    // текста под свою геометрию (другой offset, scale 0.9 вместо 0.6666667) — вызывая
    // эти методы вместо захардкоженных чисел обычной таблички, получаем правильную
    // позицию текста для конкретного подкласса рендерера через обычный виртуальный вызов.
    @Shadow
    abstract Vec3 getTextOffset();

    @Shadow
    public abstract float getSignTextRenderScale();

    @Inject(method = "renderSignText", at = @At("HEAD"), cancellable = true)
    private void langsystem$renderSignText(BlockPos pos, SignText text, PoseStack poseStack, MultiBufferSource buffer,
                                            int packedLight, int lineHeight, int maxWidth, boolean isFrontText,
                                            CallbackInfo ci) {
        Level level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof SignBlockEntity)) {
            return;
        }
        var twoSided = blockEntity.getData(ModAttachments.VANILLA_SIGN_TEXT.get());
        ModDataComponents.LanguageText content = twoSided.side(isFrontText);
        if (content == null) {
            return; // эта сторона не подписана через нашу систему — пусть рисуется как всегда
        }
        Language language = Language.byId(content.languageId()).orElse(null);
        if (language == null) {
            return;
        }

        // Прогресс 0 намеренно — фоновый вид всегда выглядит как чужая письменность,
        // независимо от грамотности автора; расшифровка только через клик (LocalPlayerMixin).
        String ambient = RuneCipher.produce(content.rawText(), language, 0);
        String[] lines = ambient.split("\n", -1);

        int darkColor = SignRenderer.getDarkColor(text);
        int color;
        boolean outline;
        int light;
        if (text.hasGlowingText()) {
            color = text.getColor().getTextColor();
            outline = langsystem$isOutlineVisible(pos, color);
            light = 15728880;
        } else {
            color = darkColor;
            outline = false;
            light = packedLight;
        }

        poseStack.pushPose();
        if (!isFrontText) {
            poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        }
        Vec3 offset = getTextOffset();
        float scale = 0.015625F * getSignTextRenderScale();
        poseStack.translate(offset.x, offset.y, offset.z);
        poseStack.scale(scale, -scale, scale);

        int half = 4 * lineHeight / 2;
        for (int i = 0; i < lines.length && i < 4; i++) {
            String line = lines[i];
            float x = -font.width(line) / 2f;
            float y = (float) (i * lineHeight - half);
            if (outline) {
                font.drawInBatch8xOutline(FormattedCharSequence.forward(line, Style.EMPTY), x, y, color, darkColor,
                        poseStack.last().pose(), buffer, light);
            } else {
                font.drawInBatch(line, x, y, color, false, poseStack.last().pose(), buffer,
                        Font.DisplayMode.POLYGON_OFFSET, 0, light);
            }
        }

        poseStack.popPose();
        ci.cancel();
    }

    /** Копия приватной {@code SignRenderer.isOutlineVisible} — своя, чтобы не шэдоуить приватный метод. */
    private static boolean langsystem$isOutlineVisible(BlockPos pos, int textColor) {
        if (textColor == DyeColor.BLACK.getTextColor()) {
            return true;
        }
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer localPlayer = minecraft.player;
        if (localPlayer != null && minecraft.options.getCameraType().isFirstPerson() && localPlayer.isScoping()) {
            return true;
        }
        Entity camera = minecraft.getCameraEntity();
        return camera != null && camera.distanceToSqr(Vec3.atCenterOf(pos)) < Mth.square(16);
    }
}
