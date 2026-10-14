package com.cleannrooster.anarchy_rpg.client.conversation;

import com.cleannrooster.anarchy_rpg.conversation.net.ConversationClosedPayload;
import com.cleannrooster.anarchy_rpg.conversation.net.OpenConversationPayload;
import dev.architectury.networking.NetworkManager;
import net.minecraft.client.MinecraftClient;

/** Client receivers for conversations. Registered during mod construction, like every payload receiver. */
public final class ConversationClient {

    private ConversationClient() {
    }

    public static void registerReceivers() {
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, OpenConversationPayload.ID, OpenConversationPayload.CODEC,
                (payload, context) -> context.queue(() -> {
                    MinecraftClient client = MinecraftClient.getInstance();
                    // The server already settled the old conversation; replacing its screen must not send a leave.
                    if (client.currentScreen instanceof ConversationScreen previous) {
                        previous.markSuperseded();
                    }
                    client.setScreen(new ConversationScreen(payload));
                }));
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, ConversationClosedPayload.ID, ConversationClosedPayload.CODEC,
                (payload, context) -> context.queue(() -> {
                    if (MinecraftClient.getInstance().currentScreen instanceof ConversationScreen screen
                            && screen.npcId() == payload.npcId()) {
                        screen.closeFromServer();
                    }
                }));
    }
}
