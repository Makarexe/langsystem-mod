package com.langsystem.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;

public final class ModKeyMappings {

    public static final String CATEGORY = "key.categories.langsystem";

    public static final KeyMapping SWITCH_LANGUAGE = new KeyMapping(
            "key.langsystem.switch_language",
            InputConstants.Type.KEYSYM,
            InputConstants.KEY_L,
            CATEGORY
    );

    private ModKeyMappings() {
    }
}
