package com.langsystem.block;

import com.langsystem.LangSystemMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlockEntities {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, LangSystemMod.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<LanguageSignBlockEntity>> LANGUAGE_SIGN =
            BLOCK_ENTITY_TYPES.register("language_sign", () -> BlockEntityType.Builder.of(
                    LanguageSignBlockEntity::new, ModBlocks.LANGUAGE_SIGN.get()).build(null));

    private ModBlockEntities() {
    }
}
