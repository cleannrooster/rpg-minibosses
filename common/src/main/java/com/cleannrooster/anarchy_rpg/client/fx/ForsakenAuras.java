package com.cleannrooster.anarchy_rpg.client.fx;

import com.cleannrooster.anarchy_rpg.entity.MinibossEntity;
import dev.architectury.event.events.client.ClientPlayerEvent;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.eann.atlasvfx.FxAttachment;
import dev.eann.atlasvfx.FxAttachments;
import dev.eann.atlasvfx.FxBlend;
import dev.eann.atlasvfx.FxColor;
import dev.eann.atlasvfx.FxSprite;
import dev.eann.atlasvfx.FxStyle;
import dev.eann.atlasvfx.MoteRecipe;
import dev.eann.atlasvfx.Motion;
import dev.eann.atlasvfx.aura.Aura;
import dev.eann.atlasvfx.aura.AuraLayer;
import dev.eann.atlasvfx.aura.ShellShape;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * The red and black aura every Forsaken Adventurer wears, so a player can tell at a glance it will attack: a
 * blood-red circle turning backwards at its feet, a faint red column of light, and black smoke with red embers
 * rising off it. Black cannot be drawn as added light, so the black is smoke lit by the world.
 *
 * <p>Kept in step with the synced {@link MinibossEntity#isForsaken} flag: put on when an adventurer is seen to be
 * Forsaken, and faded off when it no longer is (recruited) or is gone.</p>
 */
public final class ForsakenAuras {
    private static final int CHECK_INTERVAL = 10;
    private static final int RED = 0xB0141A;
    private static final int EMBER = 0xFF3A24;
    private static final int SMOKE = 0x140A0C;

    private static final FxStyle STYLE = style();
    private static final Aura AURA = new Aura(STYLE, java.util.List.of(
            new AuraLayer.GroundSigil(1.1f, -0.03f, 8),
            new AuraLayer.Shell(ShellShape.CYLINDER, 0.95f, 0.2f, 0.02f, 0.35f),
            new AuraLayer.Rise(smoke(), 0.35f, 0.9f, 0.025f),
            new AuraLayer.Rise(STYLE.wisp(), 0.25f, 0.7f, 0.04f)), 20, 20).scaledToBody();

    /** Attachments by entity id. */
    private static final Map<Integer, FxAttachment> WORN = new HashMap<>();
    private static int ticks;

    private ForsakenAuras() {
    }

    public static void init() {
        ClientTickEvent.CLIENT_POST.register(ForsakenAuras::tick);
        ClientPlayerEvent.CLIENT_PLAYER_QUIT.register(player -> WORN.clear());
    }

    private static void tick(MinecraftClient client) {
        if (client.world == null || ++ticks % CHECK_INTERVAL != 0) {
            return;
        }
        for (Entity entity : client.world.getEntities()) {
            if (entity instanceof MinibossEntity npc && npc.isForsaken() && !WORN.containsKey(npc.getId())) {
                WORN.put(npc.getId(), FxAttachments.get().attach(npc, ForsakenAuras::shapeFor, CueHandlers.DENSITY));
            }
        }
        for (Iterator<Map.Entry<Integer, FxAttachment>> it = WORN.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Integer, FxAttachment> entry = it.next();
            Entity entity = client.world.getEntityById(entry.getKey());
            // One out of view keeps its attachment: AtlasVFX brings the aura back when it returns. One in view
            // that is no longer Forsaken (recruited) has it faded off.
            if (entity != null && !(entity instanceof MinibossEntity npc && npc.isForsaken())) {
                entry.getValue().detach();
                it.remove();
            }
        }
    }

    private static dev.eann.atlasvfx.FxShape shapeFor(Entity entity) {
        return AURA.on(entity, entity.getUuid().getLeastSignificantBits());
    }

    private static FxStyle style() {
        int hot = FxColor.mix(RED, EMBER, 0.6f);
        FxSprite spark = FxSprite.particleRun("spark", 8);
        var debris = new MoteRecipe(spark, FxBlend.EMISSIVE, Motion.BURST, EMBER, 0.08f, 16, 0.0f, 0.05f, 0.0f, 0.0f, 0.1f, true, false);
        var wisp = debris.drifting(-0.002f).lasting(1.6f);
        return new FxStyle(RED, hot, debris, debris, wisp, null, null, RED, false);
    }

    /** Black smoke, lit by the world rather than glowing, curling up off the body. */
    private static MoteRecipe smoke() {
        return new MoteRecipe(FxSprite.particleRun("big_smoke", 12), FxBlend.LIT, Motion.EMIT, SMOKE,
                0.35f, 34, 0.0f, 0.05f, 0.0f, -0.004f, 0.05f, true, false);
    }
}
