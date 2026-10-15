package com.cleannrooster.anarchy_rpg.faction;

import com.cleannrooster.anarchy_rpg.RPGMinibosses;
import com.cleannrooster.anarchy_rpg.entity.MinibossEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.stream.Collectors;

/**
 * Logs an adventurer targeting a player it does not regard as hostile: the case behind "it turned on me for no
 * reason". Each line names who set the target (the first frames of the call outside vanilla's targeting
 * plumbing), how the adventurer regards the player and why (standing, anger), and what last hit it. Rate-limited
 * per adventurer, so a fight does not flood the log.
 */
public final class TargetDiagnostics {
    private static final long MIN_GAP_TICKS = 100L;
    private static final int FRAMES = 4;
    private static final Map<MinibossEntity, Long> LAST = new WeakHashMap<>();

    private TargetDiagnostics() {
    }

    public static void onTarget(MinibossEntity npc, LivingEntity previous, ServerPlayerEntity player) {
        if (previous == player) {
            return;
        }
        Disposition disposition = Factions.disposition(npc, player);
        if (disposition == Disposition.HOSTILE) {
            return; // Expected: it is supposed to attack.
        }
        long now = npc.getWorld().getTime();
        Long last = LAST.get(npc);
        if (last != null && now - last < MIN_GAP_TICKS) {
            return;
        }
        LAST.put(npc, now);

        FactionSlot faction = Factions.factionOf(npc);
        String standing = faction == null ? "n/a (no guild)"
                : String.valueOf(StandingLedger.get(player.getServer()).get(player.getUuid(), faction));
        LivingEntity attacker = npc.getAttacker();
        String setBy = StackWalker.getInstance().walk(frames -> frames
                .map(frame -> frame.getClassName().substring(frame.getClassName().lastIndexOf('.') + 1) + "." + frame.getMethodName())
                .filter(name -> !name.startsWith("TargetDiagnostics.") && !name.startsWith("MinibossEntity.setTarget"))
                .limit(FRAMES)
                .collect(Collectors.joining(" < ")));
        RPGMinibosses.LOGGER.info("[{}] {} ({}) targets {} though it is {}: standing {}, anger {} at {}, last hit by {}; set by {}",
                RPGMinibosses.CONTENT_NAMESPACE, npc.getName().getString(), npc.getType().getUntranslatedName(),
                player.getName().getString(), disposition, standing, npc.getAngerTime(),
                npc.getAngryAt() == null ? "nobody" : npc.getAngryAt().toString().substring(0, 8),
                attacker == null ? "nothing" : attacker.getName().getString() + " (" + attacker.getType().getUntranslatedName() + ")",
                setBy);
    }
}
