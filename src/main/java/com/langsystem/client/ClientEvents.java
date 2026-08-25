package com.langsystem.client;

import com.langsystem.LangSystemMod;
import com.langsystem.block.ModBlockEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;

@EventBusSubscriber(modid = LangSystemMod.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class ClientEvents {

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(ModKeyMappings.SWITCH_LANGUAGE);
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModBlockEntities.LANGUAGE_SIGN.get(), LanguageSignRenderer::new);
    }

    private ClientEvents() {
    }

    @EventBusSubscriber(modid = LangSystemMod.MOD_ID, value = Dist.CLIENT)
    public static final class GameBus {
        @SubscribeEvent
        public static void onClientTick(ClientTickEvent.Post event) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.screen != null) {
                return;
            }
            while (ModKeyMappings.SWITCH_LANGUAGE.consumeClick()) {
                mc.setScreen(new LanguageSelectScreen());
            }
        }

        /**
         * Переключатель отладочных сообщений в чат для приглушения голоса в Simple
         * Voice Chat (см. {@code voicechat.LangSystemVoicechatPlugin}) — без реального
         * теста звука сложно понять, срабатывает ли перехват вообще.
         */
        @SubscribeEvent
        public static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
            event.getDispatcher().register(Commands.literal("langvoicedebug")
                    .executes(ctx -> {
                        boolean enabled = !VoiceDebugState.isEnabled();
                        VoiceDebugState.setEnabled(enabled);
                        ctx.getSource().sendSystemMessage(Component.literal(
                                "[LangSystem] Отладка голоса: " + (enabled ? "включена" : "выключена")));
                        return 1;
                    }));
        }
    }
}
