package com.cleannrooster.anarchy_rpg.exchange;

import com.cleannrooster.anarchy_rpg.RPGMinibosses;
import com.cleannrooster.anarchy_rpg.conversation.ConversationOption;
import com.cleannrooster.anarchy_rpg.conversation.ConversationOptions;
import com.cleannrooster.anarchy_rpg.conversation.Conversations;
import com.cleannrooster.anarchy_rpg.conversation.OptionAvailability;
import com.cleannrooster.anarchy_rpg.entity.MinibossEntity;
import dev.architectury.registry.ReloadListenerRegistry;
import dev.architectury.registry.menu.ExtendedMenuProvider;
import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.resource.ResourceType;
import net.minecraft.resource.SynchronousResourceReloader;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/**
 * Exchange's wiring: the menu type, the value-table reload listener, and the conversation option that
 * opens the exchange window.
 */
public final class ExchangeMenus {
    /** Created eagerly so the client can bind its screen during mod construction; registered later. */
    public static final ScreenHandlerType<ExchangeScreenHandler> EXCHANGE =
            MenuRegistry.ofExtended((syncId, inventory, buf) -> ExchangeScreenHandler.client(syncId, inventory, buf.readVarInt()));

    private ExchangeMenus() {
    }

    /** SCREEN_HANDLER registry phase. */
    public static void registerMenus() {
        Registry.register(Registries.SCREEN_HANDLER, RPGMinibosses.id("exchange"), EXCHANGE);
    }

    /** Non-registry init. */
    public static void register() {
        ReloadListenerRegistry.register(ResourceType.SERVER_DATA,
                (SynchronousResourceReloader) ItemValues::reload, RPGMinibosses.id("item_values"));
        ConversationOptions.register(ConversationOption.EXCHANGE, ExchangeMenus::availability, (npc, player, argument) -> open(npc, player));
    }

    private static OptionAvailability availability(MinibossEntity npc, ServerPlayerEntity player,
                                                   com.cleannrooster.anarchy_rpg.faction.Disposition disposition) {
        if (npc.getOwnerUuid() != null) {
            // A companion is not a trader: its owner gets Storage, everyone else nothing.
            return OptionAvailability.HIDDEN;
        }
        npc.getExchangeStock().ensureGenerated(npc, player);
        if (npc.getExchangeStock().isEmpty()) {
            return OptionAvailability.disabled(Text.translatable("conversation." + RPGMinibosses.CONTENT_NAMESPACE + ".exchange.empty"));
        }
        return OptionAvailability.ENABLED;
    }

    /**
     * Opens the exchange window. The conversation is ended quietly first: the exchange window replaces the
     * dialogue screen, and the adventurer stays put for its new customer instead.
     */
    private static boolean open(MinibossEntity npc, ServerPlayerEntity player) {
        Conversations.end(npc, player, false);
        npc.setCustomer(player);
        MenuRegistry.openExtendedMenu(player, new ExtendedMenuProvider() {
            @Override
            public void saveExtraData(PacketByteBuf buf) {
                buf.writeVarInt(npc.getId());
            }

            @Override
            public Text getDisplayName() {
                return npc.getName();
            }

            @Override
            public ScreenHandler createMenu(int syncId, PlayerInventory inventory, PlayerEntity menuPlayer) {
                return ExchangeScreenHandler.server(syncId, inventory, npc);
            }
        });
        return true;
    }
}
