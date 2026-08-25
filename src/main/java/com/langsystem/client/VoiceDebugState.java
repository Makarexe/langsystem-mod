package com.langsystem.client;

/** Включается/выключается командой {@code /langvoicedebug} — см. {@link ClientEvents}. */
public final class VoiceDebugState {

    private static volatile boolean enabled = false;

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean value) {
        enabled = value;
    }

    private VoiceDebugState() {
    }
}
