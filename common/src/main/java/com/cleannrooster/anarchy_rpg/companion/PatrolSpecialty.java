package com.cleannrooster.anarchy_rpg.companion;

import com.cleannrooster.anarchy_rpg.conversation.OptionAvailability;
import com.cleannrooster.anarchy_rpg.entity.MinibossEntity;
import com.cleannrooster.anarchy_rpg.entity.RPGMinibossesEntities;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;

import java.util.Comparator;

/**
 * Juggernaut, "Guard my home" (3.0 M4d step 4): a standing order to guard the perimeter around the owner's
 * spawn point. The companion walks a ring of {@link #WAYPOINTS} points at {@code companionPatrolRadius}
 * and fights hostiles that come within reach of the spawn, never chasing far beyond it.
 *
 * <p>The order ({@link CompanionRecord#patrolling}) outlives trips and being downed: arriving from a trip or
 * getting back up returns it to {@link CompanionMode#PATROLLING}. Any other order clears it.
 *
 * <p><b>Circuits.</b> Each ring point is resolved once per round: <i>reached</i> (it got within
 * {@link #REACHED} of it), or <i>unreachable</i> (no full path there: water, a cliff), or <i>missed</i> (it set
 * off but stopped short). A round is complete when all {@link #WAYPOINTS} are resolved and at least
 * {@link #MIN_REACHED} were reached, so awkward terrain cannot stall it but a Juggernaut that cannot get
 * anywhere never completes one. Time spent on a round counts only while it is not fighting. A round not
 * completed within {@code companionPatrolCircuitTicks} means it is stuck: it is leashed back to the spawn
 * point and starts the round over.
 */
final class PatrolSpecialty {
    private static final int WAYPOINTS = 8;
    /** Hostiles this far past the ring are still its business. */
    private static final double GUARD_MARGIN = 8.0;
    /** It drops a chase this far past the ring and comes back. */
    private static final double LEASH_MARGIN = 16.0;
    /** Close enough to a ring point to count it reached. */
    private static final double REACHED = 3.0;
    /** A round needs at least this many points actually reached to count. */
    private static final int MIN_REACHED = WAYPOINTS / 2;
    /** This runs once a second. */
    private static final int TICKS_PER_CALL = 20;

    private PatrolSpecialty() {
    }

    static void register() {
        CompanionSpecialties.register(CompanionSpecialty.PATROL,
                (npc, player, record) -> record.patrolling ? OptionAvailability.HIDDEN : OptionAvailability.ENABLED,
                (npc, player, record, argument) -> {
                    order(player, npc, record);
                    return false;
                });
    }

    private static void order(ServerPlayerEntity player, MinibossEntity npc, CompanionRecord record) {
        MinecraftServer server = player.getServer();
        record.patrolling = true;
        record.mode = CompanionMode.PATROLLING;
        resetRound(record);
        npc.setSitting(true);
        Companions.dirty(server, npc, record);
        Companions.say(player, npc, "I will guard your home.");
    }

    /** Once a second from the companion's tick, while PATROLLING. */
    static void tick(MinecraftServer server, ServerWorld world, MinibossEntity npc, CompanionRecord record) {
        npc.setSitting(true);
        GlobalPos spawn = Companions.spawnPointOf(server, record.owner);
        Vec3d center = Vec3d.ofBottomCenter(spawn.pos());
        double walkRange = RPGMinibossesEntities.config.companionWalkRange;
        if (!spawn.dimension().equals(world.getRegistryKey()) || horizontal(npc.getPos(), center) > walkRange) {
            // Too far to walk there: it travels, and resumes the patrol on arrival.
            Companions.beginTrip(server, npc, record, spawn);
            npc.discard();
            return;
        }
        double radius = RPGMinibossesEntities.config.companionPatrolRadius;

        LivingEntity target = npc.getTarget();
        if (target != null) {
            if (!target.isAlive() || horizontal(target.getPos(), center) > radius + LEASH_MARGIN) {
                npc.setTarget(null);
                npc.getNavigation().stop();
            } else {
                return; // Its own combat AI handles the fight; the round's clock is paused.
            }
        }
        double reach = radius + GUARD_MARGIN;
        LivingEntity threat = world.getEntitiesByClass(LivingEntity.class, new Box(center, center).expand(reach, 16, reach),
                        entity -> isThreat(entity) && horizontal(entity.getPos(), center) <= reach)
                .stream().min(Comparator.comparingDouble(npc::squaredDistanceTo)).orElse(null);
        if (threat != null) {
            npc.setTarget(threat);
            return;
        }

        record.patrolRoundTicks += TICKS_PER_CALL;
        if (record.patrolRoundTicks > RPGMinibossesEntities.config.companionPatrolCircuitTicks) {
            leash(world, npc, record, spawn.pos());
            return;
        }
        if (!npc.getNavigation().isIdle()) {
            return;
        }
        // The point it was heading for is settled now: reached, or missed.
        if (record.patrolGoal != null) {
            if (horizontal(npc.getPos(), Vec3d.ofBottomCenter(record.patrolGoal)) <= REACHED) {
                record.patrolReached++;
            }
            record.patrolResolved++;
            record.patrolGoal = null;
        }
        if (record.patrolResolved >= WAYPOINTS) {
            if (record.patrolReached >= MIN_REACHED) {
                record.completedPatrols++;
                record.patrolRoundTicks = 0;
            }
            // A round with too few reached points does not count, and its clock keeps running.
            record.patrolResolved = 0;
            record.patrolReached = 0;
        }

        record.patrolWaypoint = (record.patrolWaypoint + 1) % WAYPOINTS;
        double angle = record.patrolWaypoint * (Math.PI * 2 / WAYPOINTS);
        BlockPos ring = BlockPos.ofFloored(center.x + Math.cos(angle) * radius, center.y, center.z + Math.sin(angle) * radius);
        if (!world.isChunkLoaded(ring)) {
            record.patrolResolved++; // Not loaded: nothing to walk to, and not its fault.
            return;
        }
        BlockPos ground = Companions.standableNear(world, world.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, ring));
        Path path = npc.getNavigation().findPathTo(ground, 1);
        if (path == null || !path.reachesTarget()) {
            record.patrolResolved++; // Unreachable: skipped, but not counted as reached.
            return;
        }
        npc.getNavigation().startMovingAlong(path, 0.9);
        record.patrolGoal = ground;
    }

    /** Stuck: back to the spawn point, and the round starts over. Waits for the spawn chunk if it is unloaded. */
    private static void leash(ServerWorld world, MinibossEntity npc, CompanionRecord record, BlockPos spawn) {
        if (!world.isChunkLoaded(spawn)) {
            return;
        }
        BlockPos at = Companions.standableNear(world, spawn);
        npc.getNavigation().stop();
        npc.requestTeleport(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
        record.patrolLeashes++;
        resetRound(record);
    }

    static void resetRound(CompanionRecord record) {
        record.patrolRoundTicks = 0;
        record.patrolResolved = 0;
        record.patrolReached = 0;
        record.patrolGoal = null;
    }

    /**
     * What it guards against: monsters (creepers excepted: it would bring the explosion to your door) and
     * unowned adventurers already fighting a player.
     */
    private static boolean isThreat(LivingEntity entity) {
        if (!entity.isAlive()) {
            return false;
        }
        if (entity instanceof MinibossEntity adventurer) {
            return adventurer.getOwnerUuid() == null && adventurer.getTarget() instanceof PlayerEntity;
        }
        return entity instanceof Monster && !(entity instanceof CreeperEntity);
    }

    private static double horizontal(Vec3d a, Vec3d b) {
        double dx = a.x - b.x;
        double dz = a.z - b.z;
        return Math.sqrt(dx * dx + dz * dz);
    }
}
