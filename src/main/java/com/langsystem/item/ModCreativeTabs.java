package com.langsystem.item;

import com.langsystem.LangSystemMod;
import com.langsystem.Language;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Отдельная вкладка мода в креативном меню — раньше книги были свалены в ванильную "Ингредиенты". */
public final class ModCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, LangSystemMod.MOD_ID);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN = CREATIVE_MODE_TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.langsystem.main"))
                    .icon(() -> ModItems.LANGUAGE_BOOK.toStack())
                    .displayItems((params, output) -> {
                        for (Language language : Language.values()) {
                            if (language == Language.COMMON) {
                                continue; // всеобщий и так знают все, учить нечего
                            }
                            var tome = ModItems.tomeFor(language);
                            if (tome != null) {
                                output.accept(tome);
                            }
                        }
                        output.accept(ModItems.LANGUAGE_BOOK);
                    })
                    .build());

    private ModCreativeTabs() {
    }
}
