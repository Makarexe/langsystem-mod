package com.langsystem.event;

import com.langsystem.LangSystemMod;
import com.langsystem.data.LanguageData;
import com.langsystem.data.ModAttachments;
import com.langsystem.item.LanguageTomeItem;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * Книги-самоучители нельзя скрафтить "с нуля" — нужно уже неплохо владеть языком
 * (минимум {@link #REQUIRED_PROGRESS}%), иначе получившийся "самоучитель" рассыпается,
 * а материалы тратятся впустую (как будто попытка не удалась). Технически рецепт
 * в Minecraft не умеет проверять, кто крафтит, поэтому проверка происходит уже
 * после крафта — событие не отменяет сам крафт, а "гасит" результат.
 */
@EventBusSubscriber(modid = LangSystemMod.MOD_ID)
public final class TomeCraftingHandler {

    public static final int REQUIRED_PROGRESS = 70;

    @SubscribeEvent
    public static void onItemCrafted(PlayerEvent.ItemCraftedEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) {
            return;
        }
        ItemStack crafted = event.getCrafting();
        if (!(crafted.getItem() instanceof LanguageTomeItem tome)) {
            return;
        }

        LanguageData data = serverPlayer.getData(ModAttachments.LANGUAGE_DATA);
        if (data.progress(tome.language()) >= REQUIRED_PROGRESS) {
            return; // всё в порядке, книга остаётся у игрока
        }

        crafted.setCount(0);
        serverPlayer.sendSystemMessage(Component.translatable("langsystem.msg.tome_crumbles",
                        tome.language().translatable(), REQUIRED_PROGRESS)
                .withStyle(style -> style.withColor(0xFF5555)));
    }

    private TomeCraftingHandler() {
    }
}
