package com.langsystem.client;

import com.langsystem.Language;
import com.langsystem.data.ModDataComponents;
import com.langsystem.network.SaveLanguageBookPayload;
import com.langsystem.util.RuneCipher;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Открывает экран письма (если книга ещё пустая) или чтения (если уже подписана) —
 * никогда не вызывается на сервере, поэтому может свободно использовать классы
 * клиента. Понимание текста читателем считается прямо здесь, на клиенте: серверу для
 * этого ничего пересылать не нужно — данные предмета (язык + произведённый текст) уже
 * синхронизированы как обычные данные стака, а собственный прогресс игрока в языке уже
 * закэширован в {@link ClientLanguageState}.
 */
public final class ClientBookScreens {

    public static void open(InteractionHand hand, ItemStack stack) {
        ModDataComponents.LanguageText content = stack.get(ModDataComponents.LANGUAGE_BOOK_TEXT.get());
        if (content == null) {
            openEditor(hand);
        } else {
            openReader(content);
        }
    }

    private static void openEditor(InteractionHand hand) {
        boolean offhand = hand == InteractionHand.OFF_HAND;
        Minecraft.getInstance().setScreen(new LanguageBookEditScreen(
                text -> PacketDistributor.sendToServer(new SaveLanguageBookPayload(offhand, text))
        ));
    }

    private static void openReader(ModDataComponents.LanguageText content) {
        Language language = Language.byId(content.languageId()).orElse(Language.COMMON);
        int myProgress = ClientLanguageState.progressOf(language.id());
        String shown = RuneCipher.read(content.rawText(), language, content.writerProgress(), myProgress);
        Minecraft.getInstance().setScreen(new LanguageBookReadScreen(language.displayName(), shown));
    }

    private ClientBookScreens() {
    }
}
