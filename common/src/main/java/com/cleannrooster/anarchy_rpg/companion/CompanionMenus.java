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

/** The companion storage menu: its type, and the conversation option that opens it (owner only). */
public final class CompanionMenus {
    /** Created eagerly so the client can bind its screen during mod construction; registered later. */
    public static final ScreenHandlerType<CompanionStorageScreenHandler> STORAGE =
            MenuRegistry.ofExtended((syncId, inventory, buf) -> CompanionStorageScreenHandler.client(syncId, inventory, buf.readVarInt()));
    public static final ScreenHandlerType<CompanionEquipmentScreenHandler> EQUIPMENT =
            MenuRegistry.ofExtended((syncId, inventory, buf) -> CompanionEquipmentScreenHandler.client(syncId, inventory, buf.readVarInt()));

    private CompanionMenus() {
    }

    /** SCREEN_HANDLER registry phase. */
    public static void registerMenus() {
        Registry.register(Registries.SCREEN_HANDLER, RPGMinibosses.id("companion_storage"), STORAGE);
        Registry.register(Registries.SCREEN_HANDLER, RPGMinibosses.id("companion_equipment"), EQUIPMENT);
    }

    static void registerOption() {
        ConversationOptions.register(ConversationOption.STORAGE,
                (npc, player, disposition) -> Companions.recordFor(player, npc) != null ? OptionAvailability.ENABLED : OptionAvailability.HIDDEN,
                (npc, player, argument) -> open(npc, player, false));
        ConversationOptions.register(ConversationOption.COMPANION_GEAR,
                (npc, player, disposition) -> Companions.recordFor(player, npc) != null ? OptionAvailability.ENABLED : OptionAvailability.HIDDEN,
                (npc, player, argument) -> open(npc, player, true));
    }

    /** Ends the conversation quietly (the window replaces it) and holds the companion still. */
    private static boolean open(MinibossEntity npc, ServerPlayerEntity player, boolean equipment) {
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
                return equipment
                        ? CompanionEquipmentScreenHandler.server(syncId, inventory, npc)
                        : CompanionStorageScreenHandler.server(syncId, inventory, npc);
            }
        });
        return true;
    }
}
