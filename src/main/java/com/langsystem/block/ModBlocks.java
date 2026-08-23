package com.langsystem.block;

import com.langsystem.LangSystemMod;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(LangSystemMod.MOD_ID);

    public static final DeferredBlock<LanguageSignBlock> LANGUAGE_SIGN = BLOCKS.register("language_sign",
            () -> new LanguageSignBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .sound(SoundType.WOOD)
                    .strength(2.0f)
                    .noOcclusion()));

    private ModBlocks() {
    }
}
