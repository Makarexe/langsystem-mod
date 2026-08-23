package com.langsystem.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Пустая книга, в которую можно записать текст на своём текущем языке (см. GUI выбора
 * языка) — все дальнейшие читатели видят её либо как настоящий текст, либо как
 * "иностранную" абракадабру, в зависимости от того, насколько ХОРОШО ОНИ САМИ знают
 * этот язык — точно так же, как это работает в чате. Открытие экрана письма/чтения —
 * чисто клиентское действие (см. {@code client.ClientBookScreens}), запись на сервер
 * уходит только при сохранении.
 */
public final class LanguageBookItem extends Item {

    public LanguageBookItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            com.langsystem.client.ClientBookScreens.open(hand, stack);
        }
        return InteractionResultHolder.success(stack);
    }
}
