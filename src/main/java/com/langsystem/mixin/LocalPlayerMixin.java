package com.langsystem.mixin;

import com.langsystem.Language;
import com.langsystem.client.ClientLanguageState;
import com.langsystem.client.LanguageBookReadScreen;
import com.langsystem.client.LanguageSignEditScreen;
import com.langsystem.data.ModAttachments;
import com.langsystem.data.ModDataComponents;
import com.langsystem.network.SaveVanillaSignPayload;
import com.langsystem.util.RuneCipher;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.neoforged.neoforge.network.PacketDistributor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * И автооткрытие редактора таблички сразу после установки, и открытие по правому
 * клику по стороне ванильной таблички — оба идут через один и тот же клиентский метод,
 * {@code LocalPlayer#openTextEdit} (параметр {@code isFrontText} — какая именно сторона).
 * Перехватываем его целиком: если у этой стороны ещё нет нашего attachment'а — открываем
 * свой редактор ({@link LanguageSignEditScreen}), результат уходит на сервер отдельным
 * пакетом ({@link SaveVanillaSignPayload}); если сторона уже подписана — открываем
 * читалку, персонально посчитанную под текущего игрока (а не редактор — текст, который
 * уже написан, больше не редактируется).
 */
@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {

    @Inject(method = "openTextEdit", at = @At("HEAD"), cancellable = true)
    private void langsystem$openTextEdit(SignBlockEntity signEntity, boolean isFrontText, CallbackInfo ci) {
        ci.cancel();
        BlockPos pos = signEntity.getBlockPos();
        var twoSided = signEntity.getData(ModAttachments.VANILLA_SIGN_TEXT.get());
        ModDataComponents.LanguageText content = twoSided.side(isFrontText);

        if (content == null) {
            Minecraft.getInstance().setScreen(new LanguageSignEditScreen(
                    text -> PacketDistributor.sendToServer(new SaveVanillaSignPayload(pos, isFrontText, text))
            ));
            return;
        }

        Language language = Language.byId(content.languageId()).orElse(Language.COMMON);
        int myProgress = ClientLanguageState.progressOf(language.id());
        String shown = RuneCipher.read(content.rawText(), language, content.writerProgress(), myProgress);
        Minecraft.getInstance().setScreen(new LanguageBookReadScreen(language.displayName(), shown));
    }
}
