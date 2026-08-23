package com.langsystem.data;

import com.langsystem.LangSystemMod;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.jetbrains.annotations.Nullable;

/**
 * Компонент данных предмета: язык, на котором что-то написано, ОРИГИНАЛЬНЫЙ текст (ещё
 * не искажённый) и прогресс написавшего НА МОМЕНТ письма. Это единственное, что хранится
 * на предмете/блоке — а во что это превращается для конкретного языка/читателя, каждый
 * раз считается заново (клиентом), исходя из этих трёх значений:
 * <ul>
 *   <li>{@link com.langsystem.util.RuneCipher#produce} (язык + прогресс автора) — "фоновый"
 *       вид, одинаковый для всех, не зависит от того, кто смотрит;</li>
 *   <li>{@link com.langsystem.util.RuneCipher#read} (плюс прогресс конкретного читателя) —
 *       что этот КОНКРЕТНЫЙ читатель понимает; учитывает обе грамотности разом, а не
 *       искажает уже искажённый автором текст ещё раз поверх (тогда слоги резались бы
 *       по другим границам и получались бы другие, лишние символы).</li>
 * </ul>
 */
public final class ModDataComponents {

    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENTS =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, LangSystemMod.MOD_ID);

    public record LanguageText(String languageId, String rawText, int writerProgress) {

        public static final Codec<LanguageText> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("language").forGetter(LanguageText::languageId),
                Codec.STRING.fieldOf("text").forGetter(LanguageText::rawText),
                Codec.INT.fieldOf("writerProgress").forGetter(LanguageText::writerProgress)
        ).apply(instance, LanguageText::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, LanguageText> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8, LanguageText::languageId,
                ByteBufCodecs.STRING_UTF8, LanguageText::rawText,
                ByteBufCodecs.VAR_INT, LanguageText::writerProgress,
                LanguageText::new
        );
    }

    /** Текст на лицевой и обратной стороне таблички — независимые друг от друга, как у ванильной. */
    public record TwoSidedText(LanguageText front, LanguageText back) {

        public static final LanguageText EMPTY_SIDE = new LanguageText("", "", 0);
        public static final TwoSidedText EMPTY = new TwoSidedText(EMPTY_SIDE, EMPTY_SIDE);

        public static final Codec<TwoSidedText> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                LanguageText.CODEC.fieldOf("front").forGetter(TwoSidedText::front),
                LanguageText.CODEC.fieldOf("back").forGetter(TwoSidedText::back)
        ).apply(instance, TwoSidedText::new));

        @Nullable
        public LanguageText side(boolean isFrontText) {
            LanguageText side = isFrontText ? front : back;
            return side.languageId().isEmpty() ? null : side;
        }

        public TwoSidedText withSide(boolean isFrontText, LanguageText text) {
            return isFrontText ? new TwoSidedText(text, back) : new TwoSidedText(front, text);
        }
    }

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<LanguageText>> LANGUAGE_BOOK_TEXT =
            DATA_COMPONENTS.register("language_book_text", () -> DataComponentType.<LanguageText>builder()
                    .persistent(LanguageText.CODEC)
                    .networkSynchronized(LanguageText.STREAM_CODEC)
                    .build());

    private ModDataComponents() {
    }
}
