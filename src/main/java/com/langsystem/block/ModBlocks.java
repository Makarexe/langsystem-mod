package com.langsystem.block;

import com.langsystem.LangSystemMod;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Раньше здесь была табличка-блок {@code language_sign} — заменена перехватом ванильных
 * табличек через миксины (см. {@code mixin.LocalPlayerMixin}/{@code mixin.SignBlockMixin}),
 * так что свой блок больше не нужен. Регистр оставлен пустым про запас.
 */
public final class ModBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(LangSystemMod.MOD_ID);

    private ModBlocks() {
    }
}
