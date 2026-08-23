package com.langsystem.event;

import com.langsystem.LangSystemMod;
import com.langsystem.Language;
import com.langsystem.item.ModItems;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

/** Кладёт книги-самоучители в стандартную вкладку "Ингредиенты", чтобы их было видно и без /give. */
@EventBusSubscriber(modid = LangSystemMod.MOD_ID)
public final class CreativeTabHandler {

    @SubscribeEvent
    public static void onBuildContents(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() != CreativeModeTabs.INGREDIENTS) {
            return;
        }
        for (Language language : Language.values()) {
            if (language == Language.COMMON) {
                continue;
            }
            var tome = ModItems.tomeFor(language);
            if (tome != null) {
                event.accept(tome);
            }
        }
        event.accept(ModItems.LANGUAGE_BOOK);
        event.accept(ModItems.LANGUAGE_SIGN_ITEM);
    }

    private CreativeTabHandler() {
    }
}
