package com.langsystem;

import com.langsystem.block.ModBlockEntities;
import com.langsystem.block.ModBlocks;
import com.langsystem.command.LanguageCommand;
import com.langsystem.data.ModAttachments;
import com.langsystem.data.ModDataComponents;
import com.langsystem.item.ModCreativeTabs;
import com.langsystem.item.ModItems;
import com.langsystem.network.NetworkHandler;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(LangSystemMod.MOD_ID)
public final class LangSystemMod {

    public static final String MOD_ID = "langsystem";
    public static final Logger LOGGER = LoggerFactory.getLogger("LangSystem");

    public LangSystemMod(IEventBus modEventBus) {
        ModAttachments.ATTACHMENT_TYPES.register(modEventBus);
        ModDataComponents.DATA_COMPONENTS.register(modEventBus);
        ModBlocks.BLOCKS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITY_TYPES.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);
        modEventBus.addListener(NetworkHandler::register);
    }

    @EventBusSubscriber(modid = MOD_ID)
    public static final class GameEvents {

        @SubscribeEvent
        public static void onRegisterCommands(RegisterCommandsEvent event) {
            LanguageCommand.register(event.getDispatcher());
        }

        @SubscribeEvent
        public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
            if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                NetworkHandler.sendSync(serverPlayer);
                NetworkHandler.sendAllSpeakerLanguages(serverPlayer);
            }
        }

        private GameEvents() {
        }
    }
}
