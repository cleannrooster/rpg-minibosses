package com.cleannrooster.anarchy_rpg.service;

import com.cleannrooster.anarchy_rpg.RPGMinibosses;
import com.cleannrooster.anarchy_rpg.conversation.Conversations;
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

/** The one menu type every paid service shares, and opening it for a given service. */
public final class PaidServiceMenus {
    /** Created eagerly so the client can bind its screen during mod construction; registered later. */
    public static final ScreenHandlerType<PaidServiceScreenHandler> TYPE =
            MenuRegistry.ofExtended((syncId, inventory, buf) -> PaidServiceScreenHandler.client(syncId, inventory,
                    buf.readVarInt(), PaidServiceKind.fromIndex(buf.readVarInt())));

    private PaidServiceMenus() {
    }

    /** SCREEN_HANDLER registry phase (called from {@code CompanionMenus.registerMenus}). */
    public static void registerMenus() {
        Registry.register(Registries.SCREEN_HANDLER, RPGMinibosses.id("paid_service"), TYPE);
    }

    /**
     * Opens the payment window for {@code kind}. Ends the conversation quietly first (the window replaces it)
     * and holds the adventurer still for its customer. Returns true, as the other windows do: the conversation
     * was already ended quietly, so it must not be ended (and the client told) a second time.
     */
    public static boolean open(MinibossEntity npc, ServerPlayerEntity player, PaidServiceKind kind) {
        Conversations.end(npc, player, false);
        npc.setCustomer(player);
        MenuRegistry.openExtendedMenu(player, new ExtendedMenuProvider() {
            @Override
            public void saveExtraData(PacketByteBuf buf) {
                buf.writeVarInt(npc.getId());
                buf.writeVarInt(kind.ordinal());
            }

            @Override
            public Text getDisplayName() {
                return kind.service().windowTitle(npc, player);
            }

            @Override
            public ScreenHandler createMenu(int syncId, PlayerInventory inventory, PlayerEntity menuPlayer) {
                return PaidServiceScreenHandler.server(syncId, inventory, npc, kind);
            }
        });
        return true;
    }
}
