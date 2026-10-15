package com.cleannrooster.anarchy_rpg.companion;

import com.cleannrooster.anarchy_rpg.RPGMinibosses;
import com.cleannrooster.anarchy_rpg.conversation.ConversationOption;
import com.cleannrooster.anarchy_rpg.conversation.ConversationOptions;
import com.cleannrooster.anarchy_rpg.conversation.Conversations;
import com.cleannrooster.anarchy_rpg.conversation.OptionAvailability;
import com.cleannrooster.anarchy_rpg.entity.MinibossEntity;
import dev.architectury.registry.menu.ExtendedMenuProvider;
import dev.architectury.registry.menu.MenuRegistry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/** The companion windows (storage, equipment, hire): their menu types and the conversation options that open them. */
public final class CompanionMenus {
    /** Created eagerly so the client can bind its screen during mod construction; registered later. */
    public static final ScreenHandlerType<CompanionStorageScreenHandler> STORAGE =
            MenuRegistry.ofExtended((syncId, inventory, buf) -> CompanionStorageScreenHandler.client(syncId, inventory, buf.readVarInt()));
    public static final ScreenHandlerType<CompanionEquipmentScreenHandler> EQUIPMENT =
            MenuRegistry.ofExtended((syncId, inventory, buf) -> CompanionEquipmentScreenHandler.client(syncId, inventory, buf.readVarInt()));
    public static final ScreenHandlerType<HireScreenHandler> HIRE =
            MenuRegistry.ofExtended((syncId, inventory, buf) -> HireScreenHandler.client(syncId, inventory, buf.readVarInt()));

    private CompanionMenus() {
    }

    /** SCREEN_HANDLER registry phase. */
    public static void registerMenus() {
        Registry.register(Registries.SCREEN_HANDLER, RPGMinibosses.id("companion_storage"), STORAGE);
        Registry.register(Registries.SCREEN_HANDLER, RPGMinibosses.id("companion_equipment"), EQUIPMENT);
        Registry.register(Registries.SCREEN_HANDLER, RPGMinibosses.id("hire"), HIRE);
    }

    static void registerOption() {
        ConversationOptions.register(ConversationOption.STORAGE,
                (npc, player, disposition) -> Companions.recordFor(player, npc) != null && !TraderSpecialty.isTrader(npc)
                        ? OptionAvailability.ENABLED : OptionAvailability.HIDDEN,
                (npc, player, argument) -> open(npc, player, Window.STORAGE));
        ConversationOptions.register(ConversationOption.COMPANION_GEAR,
                (npc, player, disposition) -> Companions.recordFor(player, npc) != null ? OptionAvailability.ENABLED : OptionAvailability.HIDDEN,
                (npc, player, argument) -> open(npc, player, Window.EQUIPMENT));
        // Hire: any adventurer not yet anyone's companion; disabled, with the reason, when it can't be hired.
        ConversationOptions.register(ConversationOption.HIRE,
                (npc, player, disposition) -> {
                    if (npc.getOwnerUuid() != null) {
                        return OptionAvailability.HIDDEN;
                    }
                    Text reason = Hiring.whyNot(npc, player, disposition);
                    return reason == null ? OptionAvailability.ENABLED : OptionAvailability.disabled(reason);
                },
                (npc, player, argument) -> open(npc, player, Window.HIRE));
    }

    private enum Window { STORAGE, EQUIPMENT, HIRE }

    /** Ends the conversation quietly (the window replaces it) and holds the companion still. */
    private static boolean open(MinibossEntity npc, ServerPlayerEntity player, Window window) {
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
                return switch (window) {
                    case STORAGE -> CompanionStorageScreenHandler.server(syncId, inventory, npc);
                    case EQUIPMENT -> CompanionEquipmentScreenHandler.server(syncId, inventory, npc);
                    case HIRE -> HireScreenHandler.server(syncId, inventory, npc);
                };
            }
        });
        return true;
    }
}
