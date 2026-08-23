package com.langsystem.item;

import com.langsystem.LangSystemMod;
import com.langsystem.Language;
import com.langsystem.block.ModBlocks;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.Map;

public final class ModItems {

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(LangSystemMod.MOD_ID);

    private static final Map<Language, DeferredItem<LanguageTomeItem>> TOMES = new EnumMap<>(Language.class);

    static {
        for (Language language : Language.values()) {
            if (language == Language.COMMON) {
                continue; // всеобщий и так знают все, учить нечего
            }
            String id = language.id() + "_tome";
            DeferredItem<LanguageTomeItem> holder = ITEMS.registerItem(id,
                    properties -> new LanguageTomeItem(language, properties),
                    new Item.Properties().stacksTo(1));
            TOMES.put(language, holder);
        }
    }

    /** Пустая книга, в которую можно записать текст на текущем языке (см. {@link LanguageBookItem}). */
    public static final DeferredItem<LanguageBookItem> LANGUAGE_BOOK = ITEMS.registerItem("language_book",
            LanguageBookItem::new, new Item.Properties().stacksTo(1));

    public static final DeferredItem<BlockItem> LANGUAGE_SIGN_ITEM = ITEMS.registerItem("language_sign",
            properties -> new BlockItem(ModBlocks.LANGUAGE_SIGN.get(), properties), new Item.Properties());

    public static DeferredItem<LanguageTomeItem> tomeFor(Language language) {
        return TOMES.get(language);
    }

    private ModItems() {
    }
}
