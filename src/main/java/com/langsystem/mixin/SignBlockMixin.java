package com.langsystem.mixin;

import com.langsystem.data.ModAttachments;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.SignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Ванильный {@code SignBlock#openTextEdit()} перед показом ЛЮБОГО экрана игроку сначала
 * ставит блокировку {@code playerWhoMayEdit} на его UUID — защита от одновременного
 * редактирования одной и той же пустой таблички двумя игроками. Но когда сторона уже
 * подписана через нашу систему (см. {@code mixin.LocalPlayerMixin}) — открывается не
 * редактор, а читалка, и эта блокировка не нужна: пока читающий не отойдёт от таблички
 * на 4+ блока (см. {@code SignBlockEntity#clearInvalidPlayerWhoMayEdit}), НИКТО другой не
 * может по ней даже кликнуть — {@code SignBlock#useWithoutItem()} тихо вернёт
 * {@code PASS}. Пропускаем установку блокировки именно в этом случае.
 */
@Mixin(SignBlock.class)
public abstract class SignBlockMixin {

    @Inject(method = "openTextEdit", at = @At("HEAD"), cancellable = true)
    private void langsystem$skipLockForRead(Player player, SignBlockEntity signEntity, boolean isFrontText, CallbackInfo ci) {
        var twoSided = signEntity.getData(ModAttachments.VANILLA_SIGN_TEXT.get());
        if (twoSided.side(isFrontText) != null) {
            player.openTextEdit(signEntity, isFrontText);
            ci.cancel();
        }
    }
}
