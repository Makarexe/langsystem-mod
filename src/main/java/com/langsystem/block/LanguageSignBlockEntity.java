package com.langsystem.block;

import com.langsystem.data.ModDataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Как у настоящей ванильной таблички — лицевая и обратная стороны хранят разный текст. */
public final class LanguageSignBlockEntity extends BlockEntity {

    private static final String KEY_LANGUAGE_FRONT = "LanguageFront";
    private static final String KEY_TEXT_FRONT = "TextFront";
    private static final String KEY_PROGRESS_FRONT = "ProgressFront";
    private static final String KEY_LANGUAGE_BACK = "LanguageBack";
    private static final String KEY_TEXT_BACK = "TextBack";
    private static final String KEY_PROGRESS_BACK = "ProgressBack";

    @Nullable
    private ModDataComponents.LanguageText front;
    @Nullable
    private ModDataComponents.LanguageText back;

    public LanguageSignBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LANGUAGE_SIGN.get(), pos, state);
    }

    @Nullable
    public ModDataComponents.LanguageText content(boolean isFrontText) {
        return isFrontText ? front : back;
    }

    /** Вызывается только на сервере при сохранении текста; сразу же рассылает обновление наблюдателям. */
    public void setContent(boolean isFrontText, ModDataComponents.LanguageText content) {
        if (isFrontText) {
            this.front = content;
        } else {
            this.back = content;
        }
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (front != null) {
            tag.putString(KEY_LANGUAGE_FRONT, front.languageId());
            tag.putString(KEY_TEXT_FRONT, front.rawText());
            tag.putInt(KEY_PROGRESS_FRONT, front.writerProgress());
        }
        if (back != null) {
            tag.putString(KEY_LANGUAGE_BACK, back.languageId());
            tag.putString(KEY_TEXT_BACK, back.rawText());
            tag.putInt(KEY_PROGRESS_BACK, back.writerProgress());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        front = tag.contains(KEY_TEXT_FRONT)
                ? new ModDataComponents.LanguageText(tag.getString(KEY_LANGUAGE_FRONT), tag.getString(KEY_TEXT_FRONT),
                        tag.getInt(KEY_PROGRESS_FRONT))
                : null;
        back = tag.contains(KEY_TEXT_BACK)
                ? new ModDataComponents.LanguageText(tag.getString(KEY_LANGUAGE_BACK), tag.getString(KEY_TEXT_BACK),
                        tag.getInt(KEY_PROGRESS_BACK))
                : null;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
