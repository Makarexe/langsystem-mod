package com.langsystem.item;

import com.langsystem.Language;
import com.langsystem.data.LanguageData;
import com.langsystem.data.ModAttachments;
import com.langsystem.network.NetworkHandler;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Книга для самостоятельного, очень медленного изучения одного конкретного языка.
 * Раз в {@link #STUDY_COOLDOWN_TICKS} читатель получает +1% прогресса.
 */
public final class LanguageTomeItem extends Item {

    /** Кулдаун между "занятиями" по книге — 5 игровых минут (в тиках, 20 тиков/сек). */
    public static final long STUDY_COOLDOWN_TICKS = 20L * 60L * 5L;

    private final Language language;

    public LanguageTomeItem(Language language, Properties properties) {
        super(properties);
        this.language = language;
    }

    public Language language() {
        return language;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) {
            return InteractionResultHolder.success(stack);
        }

        LanguageData data = player.getData(ModAttachments.LANGUAGE_DATA);
        if (data.progress(language) >= 100) {
            player.displayClientMessage(Component.translatable("langsystem.msg.tome_already_fluent",
                    language.translatable()), true);
            return InteractionResultHolder.pass(stack);
        }

        long gameTime = level.getGameTime();
        if (!data.canStudy(language, gameTime, STUDY_COOLDOWN_TICKS)) {
            player.displayClientMessage(Component.translatable("langsystem.msg.tome_cooldown"), true);
            return InteractionResultHolder.pass(stack);
        }

        data.markStudied(language, gameTime);
        int newProgress = data.addProgress(language, 1);
        player.setData(ModAttachments.LANGUAGE_DATA, data);
        if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
            NetworkHandler.sendSync(serverPlayer);
        }

        player.sendSystemMessage(Component.translatable("langsystem.msg.tome_study_gain",
                        language.translatable(), newProgress)
                .withStyle(style -> style.withColor(language.color())));

        return InteractionResultHolder.consume(stack);
    }
}
