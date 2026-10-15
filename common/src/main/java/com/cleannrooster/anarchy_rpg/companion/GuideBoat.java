package com.cleannrooster.anarchy_rpg.companion;

import com.cleannrooster.anarchy_rpg.RPGMinibosses;
import com.cleannrooster.anarchy_rpg.entity.MinibossEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.vehicle.BoatEntity;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;

import java.util.Comparator;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * Crossing water while leading (Rogue, "Lead me somewhere"). When the way forward has no path, no ledge to vault,
 * and water within {@link #WATER_REACH} blocks, the Rogue gets into a boat: an empty one within
 * {@link #FIND_RADIUS} blocks, or one it conjures at the water's edge.
 *
 * <p>Vanilla boats only answer to a player at the controls. With the Rogue in the front seat the server simulates
 * the boat, so the Rogue steers it itself, every tick, toward the destination. It sails only while its owner is
 * aboard (the second seat) or in a boat of their own and close enough; otherwise it holds still and says so.
 * When the boat stops making headway (a bank, a beach, the shallows), the Rogue steps onto dry land. A conjured boat is put away
 * (anyone still in it lands at the water's edge); a found one is left where it landed.
 */
final class GuideBoat {
    private static final String NS = RPGMinibosses.CONTENT_NAMESPACE;
    private static final int WATER_REACH = 6;
    private static final double FIND_RADIUS = 8.0;
    /** Per-tick speed the Rogue rows at; vanilla's water drag takes about a tenth back each tick. */
    private static final double ROW_SPEED = 0.32;
    /** Degrees the boat may turn per tick. */
    private static final float TURN_RATE = 6.0F;
    /** An owner in their own boat this far away is "with" the Rogue. */
    private static final double CONVOY_RANGE = 24.0;
    private static final int HINT_EVERY = 10;
    /**
     * Landing is decided by progress, not by collision flags: a boat that is being rowed but has covered less than
     * {@link #STALL_DISTANCE} in a second is up against something (a bank, a beach, a grounding in the shallows),
     * and {@link #STALL_SECONDS} of that in a row means it has reached the shore. The first
     * {@link #GRACE_SECONDS} are not counted, since it sets off pressed against the shore it is leaving.
     */
    private static final int GRACE_SECONDS = 2;
    private static final double STALL_DISTANCE = 1.0;
    private static final int STALL_SECONDS = 2;
    /** After landing it will not take a boat again this soon (ticks), so it cannot loop at an awkward shore. */
    private static final long REBOARD_COOLDOWN = 200L;
    /** Where it last stepped ashore, and when: for the reboard cooldown. */
    private static final Map<MinibossEntity, Long> LANDED_AT = new WeakHashMap<>();

    /** A crossing in progress: which boat, whether the Rogue conjured it, and whether it is rowing right now. */
    private static final class Crossing {
        final UUID boat;
        final boolean conjured;
        boolean rowing;
        /** Seconds rowed; where the boat was a second ago; seconds in a row it has barely moved. */
        int rowedSeconds;
        @org.jetbrains.annotations.Nullable
        Vec3d lastPos;
        int stalledSeconds;

        Crossing(UUID boat, boolean conjured) {
            this.boat = boat;
            this.conjured = conjured;
        }
    }

    private static final Map<MinibossEntity, Crossing> CROSSINGS = new WeakHashMap<>();

    private GuideBoat() {
    }

    static boolean isBoating(MinibossEntity npc) {
        return npc.getVehicle() instanceof BoatEntity;
    }

    /** Gets into a boat if there is water ahead. Returns whether it did. */
    static boolean tryBoard(ServerWorld world, MinibossEntity npc, ServerPlayerEntity owner, Vec3d heading) {
        Long landed = LANDED_AT.get(npc);
        if (npc.hasVehicle() || (landed != null && world.getTime() - landed < REBOARD_COOLDOWN)) {
            return false;
        }
        BlockPos water = null;
        for (int reach = 1; reach <= WATER_REACH && water == null; reach++) {
            BlockPos column = BlockPos.ofFloored(npc.getX() + heading.x * reach, npc.getY(), npc.getZ() + heading.z * reach);
            if (!world.isChunkLoaded(column)) {
                return false;
            }
            BlockPos above = world.getTopPosition(Heightmap.Type.MOTION_BLOCKING, column);
            if (world.getFluidState(above.down()).isIn(FluidTags.WATER) && Math.abs(above.getY() - npc.getBlockY()) <= 3) {
                water = above;
            }
        }
        if (water == null) {
            return false;
        }
        BoatEntity boat = world.getEntitiesByClass(BoatEntity.class, npc.getBoundingBox().expand(FIND_RADIUS),
                        candidate -> candidate.getPassengerList().isEmpty() && candidate.isTouchingWater())
                .stream().min(Comparator.comparingDouble(npc::squaredDistanceTo)).orElse(null);
        boolean conjured = boat == null;
        if (conjured) {
            boat = new BoatEntity(world, water.getX() + 0.5, water.getY(), water.getZ() + 0.5);
            boat.setVariant(BoatEntity.Type.OAK);
            boat.setYaw((float) Math.toDegrees(Math.atan2(-heading.x, heading.z)));
            if (!world.spawnEntity(boat)) {
                return false;
            }
            world.playSound(null, boat.getBlockPos(), SoundEvents.ENTITY_GENERIC_SPLASH, SoundCategory.NEUTRAL, 0.6F, 1.2F);
        }
        npc.getNavigation().stop();
        if (!npc.startRiding(boat, true)) {
            if (conjured) {
                boat.discard();
            }
            return false;
        }
        CROSSINGS.put(npc, new Crossing(boat.getUuid(), conjured));
        owner.sendMessage(npc.getName().copy().append(": ").append(Text.translatable("conversation." + NS + ".guide.boat")));
        return true;
    }

    /**
     * Once a second while in the boat: decide whether to row (the owner is aboard, or close behind in their own
     * boat), nudge the owner if not, and land on reaching the far shore.
     */
    static void second(ServerWorld world, MinibossEntity npc, ServerPlayerEntity owner, CompanionRecord record, double waitDistance) {
        if (!(npc.getVehicle() instanceof BoatEntity boat)) {
            CROSSINGS.remove(npc);
            return;
        }
        Crossing crossing = CROSSINGS.computeIfAbsent(npc, ignored -> new Crossing(boat.getUuid(), false));
        boolean aboard = owner.getVehicle() == boat;
        boolean convoy = owner.getVehicle() instanceof BoatEntity && owner.squaredDistanceTo(boat) <= CONVOY_RANGE * CONVOY_RANGE;
        boolean close = owner.squaredDistanceTo(boat) <= waitDistance * waitDistance;
        // Heading home after waiting too long, it rows alone; otherwise only with its owner along.
        crossing.rowing = record.guideReturning || aboard || (convoy && close);
        if (!crossing.rowing) {
            boat.setVelocity(boat.getVelocity().multiply(0.0, 1.0, 0.0));
            boat.velocityModified = true;
            if (record.guideWaited++ % HINT_EVERY == 0) {
                owner.sendMessage(Text.translatable("conversation." + NS + ".guide.boat_hint", npc.getName()), true);
            }
            return;
        }
        record.guideWaited = 0;
        crossing.rowedSeconds++;
        Vec3d now = boat.getPos();
        if (crossing.lastPos != null && crossing.rowedSeconds > GRACE_SECONDS) {
            double dx = now.x - crossing.lastPos.x;
            double dz = now.z - crossing.lastPos.z;
            crossing.stalledSeconds = dx * dx + dz * dz < STALL_DISTANCE * STALL_DISTANCE ? crossing.stalledSeconds + 1 : 0;
        }
        crossing.lastPos = now;
        boolean beached = !boat.isTouchingWater() && boat.isOnGround();
        if (crossing.stalledSeconds >= STALL_SECONDS || (beached && crossing.rowedSeconds > GRACE_SECONDS)) {
            land(world, npc, boat, crossing);
        }
    }

    /** Every tick: while rowing, turn toward and drive at the destination. */
    static void tick(MinibossEntity npc, @org.jetbrains.annotations.Nullable Vec3d goal) {
        Crossing crossing = CROSSINGS.get(npc);
        if (crossing == null || !crossing.rowing || goal == null || !(npc.getVehicle() instanceof BoatEntity boat)) {
            return;
        }
        double dx = goal.x - boat.getX();
        double dz = goal.z - boat.getZ();
        float wanted = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float yaw = boat.getYaw() + MathHelper.clamp(MathHelper.wrapDegrees(wanted - boat.getYaw()), -TURN_RATE, TURN_RATE);
        boat.setYaw(yaw);
        double radians = Math.toRadians(yaw);
        boat.setVelocity(-Math.sin(radians) * ROW_SPEED, boat.getVelocity().y, Math.cos(radians) * ROW_SPEED);
        boat.velocityModified = true;
    }

    /** Onto dry land beside the bow; a conjured boat is put away. */
    private static void land(ServerWorld world, MinibossEntity npc, BoatEntity boat, Crossing crossing) {
        BlockPos shore = dryGroundNear(world, boat);
        npc.stopRiding();
        if (shore != null) {
            npc.requestTeleport(shore.getX() + 0.5, shore.getY(), shore.getZ() + 0.5);
        }
        LANDED_AT.put(npc, world.getTime());
        if (crossing.conjured) {
            boat.removeAllPassengers();
            boat.discard();
        }
        CROSSINGS.remove(npc);
    }

    /**
     * Dry ground to step onto: beside the bow first, then around the boat, then a little further ahead. Null if
     * there is none in reach (it then gets out where it is and wades).
     */
    private static @org.jetbrains.annotations.Nullable BlockPos dryGroundNear(ServerWorld world, BoatEntity boat) {
        double radians = Math.toRadians(boat.getYaw());
        double fx = -Math.sin(radians);
        double fz = Math.cos(radians);
        BlockPos[] tries = {
                BlockPos.ofFloored(boat.getX() + fx * 1.5, boat.getY() + 1, boat.getZ() + fz * 1.5),
                BlockPos.ofFloored(boat.getX(), boat.getY() + 1, boat.getZ()),
                BlockPos.ofFloored(boat.getX() + fx * 4.0, boat.getY() + 1, boat.getZ() + fz * 4.0),
        };
        for (BlockPos near : tries) {
            BlockPos spot = Companions.standableNear(world, near);
            if (world.getFluidState(spot).isEmpty() && world.getFluidState(spot.down()).isEmpty()
                    && !world.getBlockState(spot.down()).getCollisionShape(world, spot.down()).isEmpty()) {
                return spot;
            }
        }
        return null;
    }

    /** The lead is over (arrived, Follow, teleport, timeout): out of the boat, and a conjured one is put away. */
    static void disembark(MinibossEntity npc) {
        Crossing crossing = CROSSINGS.remove(npc);
        Entity vehicle = npc.getVehicle();
        if (vehicle instanceof BoatEntity boat) {
            npc.stopRiding();
            if (crossing != null && crossing.conjured) {
                boat.removeAllPassengers();
                boat.discard();
            }
        }
    }
}
