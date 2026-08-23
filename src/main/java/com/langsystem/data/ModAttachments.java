package com.langsystem.data;

import com.langsystem.LangSystemMod;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModAttachments {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, LangSystemMod.MOD_ID);

    public static final DeferredHolder<AttachmentType<?>, AttachmentType<LanguageData>> LANGUAGE_DATA =
            ATTACHMENT_TYPES.register("language_data", () -> AttachmentType.builder(LanguageData::new)
                    .serialize(LanguageData.CODEC)
                    .copyOnDeath()
                    .build());

    /**
     * Языковой текст лицевой/обратной стороны, "приклеенный" к произвольной ванильной
     * табличке (см. {@code mixin.LocalPlayerMixin} / {@code mixin.SignRendererMixin}).
     * Пустой {@code languageId} у стороны — она ещё не подписана через языковую систему.
     * NeoForge сам сохраняет и синхронизирует attachment'ы блок-энтити наравне со всеми
     * остальными данными (через {@code saveAdditional}/{@code getUpdateTag}) — миксин
     * здесь не нужен.
     */
    public static final DeferredHolder<AttachmentType<?>, AttachmentType<ModDataComponents.TwoSidedText>> VANILLA_SIGN_TEXT =
            ATTACHMENT_TYPES.register("vanilla_sign_text", () -> AttachmentType.builder(
                            () -> ModDataComponents.TwoSidedText.EMPTY)
                    .serialize(ModDataComponents.TwoSidedText.CODEC)
                    .build());

    private ModAttachments() {
    }
}
