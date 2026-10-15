package com.cleannrooster.anarchy_rpg.client.journal;

import com.cleannrooster.anarchy_rpg.RPGMinibosses;
import com.cleannrooster.anarchy_rpg.journal.net.JournalPayload;
import com.cleannrooster.anarchy_rpg.journal.net.JournalRequestPayload;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.networking.NetworkManager;
import dev.architectury.registry.client.keymappings.KeyMappingRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/** The Guild Journal's key and receiver. The key asks the server for the journal; the server's answer opens it. */
public final class JournalClient {
    public static final KeyBinding OPEN = new KeyBinding("key." + RPGMinibosses.CONTENT_NAMESPACE + ".journal",
            InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_J, "key.categories." + RPGMinibosses.CONTENT_NAMESPACE);

    private JournalClient() {
    }

    /**
     * Must run during mod construction: on NeoForge, Architectury hands key mappings over when
     * {@code RegisterKeyMappingsEvent} fires, before client setup, as with payload receivers.
     */
    public static void earlyInit() {
        KeyMappingRegistry.register(OPEN);
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, JournalPayload.ID, JournalPayload.CODEC,
                (payload, context) -> context.queue(() -> {
                    MinecraftClient client = MinecraftClient.getInstance();
                    if (client.currentScreen instanceof JournalScreen screen) {
                        screen.update(payload);
                    } else if (client.currentScreen == null) {
                        client.setScreen(new JournalScreen(payload));
                    }
                }));
    }

    public static void init() {
        ClientTickEvent.CLIENT_POST.register(client -> {
            while (OPEN.wasPressed()) {
                if (client.player != null && client.currentScreen == null
                        && NetworkManager.canServerReceive(JournalRequestPayload.ID)) {
                    NetworkManager.sendToServer(JournalRequestPayload.INSTANCE);
                }
            }
        });
    }
}
