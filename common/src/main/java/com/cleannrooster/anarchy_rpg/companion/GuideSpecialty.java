package com.cleannrooster.anarchy_rpg.companion;

import com.cleannrooster.anarchy_rpg.RPGMinibosses;
import com.cleannrooster.anarchy_rpg.config.ServerConfig;
import com.cleannrooster.anarchy_rpg.conversation.ConversationOption;
import com.cleannrooster.anarchy_rpg.conversation.ConversationOptions;
import com.cleannrooster.anarchy_rpg.conversation.Conversations;
import com.cleannrooster.anarchy_rpg.conversation.OptionAvailability;
import com.cleannrooster.anarchy_rpg.entity.MinibossEntity;
import com.cleannrooster.anarchy_rpg.entity.RPGMinibossesEntities;
import com.cleannrooster.anarchy_rpg.rumor.Bearing;
import com.cleannrooster.anarchy_rpg.rumor.DistanceBand;
import com.cleannrooster.anarchy_rpg.rumor.RumorKind;
import com.cleannrooster.anarchy_rpg.rumor.RumorSources;
import com.cleannrooster.anarchy_rpg.rumor.RumorState;
import com.cleannrooster.anarchy_rpg.rumor.RumorSubject;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Heightmap;

import java.util.List;
import java.util.Optional;

/**
 * Rogue, "Lead me somewhere" (3.0 M4d step 5). Once a day it names a rumorable place nearby (the M5 structure
 * search: same kinds and radius, skipping places the owner already knows) and then leads the owner there.
 *
 * <p>It always tells the truth, precisely: its owner is devoted. While {@link CompanionMode#GUIDING} it walks in
 * {@link #STRIDE}-block legs toward the place's surface spot, waits (facing the owner) whenever they fall more
 * than {@code companionGuideWaitDistance} behind, and tries a slanted leg when the straight one has no path.
 * It moves at least as fast as the owner is going, and never slower than the owner's full sprint
 * ({@link #leadSpeed}). Where there is no walkable way forward it vaults up a ledge of up to
 * {@link #VAULT_HEIGHT} blocks ({@link #tryVault}), and where there is water instead it takes a boat
 * ({@link GuideBoat}). Within {@link #ARRIVED} blocks it announces the place and follows again. The Follow order, the owner leaving
 * the dimension or logging off, or {@code companionGuideMaxTicks} passing also end it. Being downed pauses it.
 */
public final class GuideSpecialty {
    private static final String NS = RPGMinibosses.CONTENT_NAMESPACE;
    private static final double STRIDE = 20.0;
    private static final double ARRIVED = 12.0;
    /** Close enough to home to call the way back done. */
    private static final double HOME_REACHED = 4.0;
    /** How often the "keep up" hint repeats while waiting, in seconds (this runs once a second). */
    private static final int HINT_EVERY = 10;
    private static final double[] SLANTS = {0.0, Math.PI / 4, -Math.PI / 4};
    /** Leg lengths tried, as fractions of {@link #STRIDE}: a whole stride, then half. */
    private static final double[] STRIDE_SCALES = {1.0, 0.5};
    /** A partial path is only taken if its end is at least this much closer to the goal. */
    private static final double MIN_PROGRESS = 4.0;
    /** More path nodes than this in water means swimming, not wading a stream. */
    private static final int MAX_WATER_NODES = 2;
    /** How many places it offers to lead to, and how long the offer stands (ticks). */
    private static final int CHOICES = 3;
    private static final long OFFER_TICKS = 2400L;

    /** Places offered to a player by one Rogue, waiting for their pick. */
    private record Offer(java.util.UUID npc, List<RumorSubject> places, long expiresAt) {
    }

    private static final java.util.Map<java.util.UUID, Offer> OFFERS = new java.util.HashMap<>();
    private static final ConversationOption[] CHOICE_OPTIONS = {
            ConversationOption.GUIDE_TO_1, ConversationOption.GUIDE_TO_2, ConversationOption.GUIDE_TO_3};
    /** Vanilla's sprint boost: +30% movement speed. */
    private static final double SPRINT = 1.3;
    /**
     * Blocks per tick on flat ground for each point of a player's movement speed (walking 0.1 is about 4.3
     * blocks a second). Used only to turn a measured owner speed into movement-speed units.
     */
    private static final double GROUND_BLOCKS_PER_SPEED = 2.16;
    /** Head room over the owner's pace, for sprint-jumping and to stay ahead rather than level. */
    private static final double PACE_MARGIN = 1.15;
    /** A leg with this few path nodes left gets its successor planned now, so it never stops between legs. */
    private static final int REPLAN_NODES_LEFT = 4;
    /** Faster than this over a second was a teleport or a launch, not running. */
    private static final double MAX_MEASURED = 1.5;
    /** The highest ledge it vaults onto, and how far ahead it looks for one. */
    private static final int VAULT_HEIGHT = 3;
    private static final int VAULT_REACH = 2;
    /** Ticks between vaults, so it never chains them. */
    private static final long VAULT_COOLDOWN = 40L;
    /** Gravity and air drag per tick, as vanilla applies them to mobs. */
    private static final double GRAVITY = 0.08;
    private static final double DRAG = 0.98;
    /**
     * The clip played for a vault, on the "attacks" layer over the run cycle: the Rogue's roll, which reads as an
     * acrobatic hop and is about as long as the jump (chosen 2026-10-14 over a dedicated clip).
     */
    private static final String VAULT_CLIP = "animation.v2.trickster.roll";

    private GuideSpecialty() {
    }

    static void register() {
        CompanionSpecialties.register(CompanionSpecialty.GUIDE, GuideSpecialty::check, GuideSpecialty::lead);
        for (int i = 0; i < CHOICE_OPTIONS.length; i++) {
            int index = i;
            ConversationOptions.register(CHOICE_OPTIONS[i],
                    (npc, player, disposition) -> choiceAvailability(index, npc, player),
                    (npc, player, argument) -> choose(index, npc, player));
        }
        dev.architectury.event.events.common.PlayerEvent.PLAYER_QUIT.register(player -> OFFERS.remove(player.getUuid()));
    }

    /** Whether this companion is leading its owner somewhere: it counts as adventuring for rumor charges. */
    public static boolean isGuiding(MinibossEntity npc) {
        if (!(npc.getWorld() instanceof ServerWorld world) || npc.getOwnerUuid() == null) {
            return false;
        }
        CompanionRecord record = CompanionRegistry.get(world.getServer()).ofNpc(npc.getUuid());
        return record != null && record.mode == CompanionMode.GUIDING;
    }

    private static Text line(String key, Object... args) {
        return Text.translatable("conversation." + NS + ".guide." + key, args);
    }

    private static void tell(ServerPlayerEntity player, MinibossEntity npc, Text text) {
        player.sendMessage(npc.getName().copy().append(": ").append(text));
    }

    private static OptionAvailability check(MinibossEntity npc, ServerPlayerEntity player, CompanionRecord record) {
        Offer offer = OFFERS.get(player.getUuid());
        boolean choosing = offer != null && offer.npc().equals(npc.getUuid()) && Companions.now(player.getServer()) <= offer.expiresAt();
        if (record.mode == CompanionMode.GUIDING || choosing) {
            return OptionAvailability.HIDDEN; // While choosing, the places stand in for this option.
        }
        return record.mode == CompanionMode.FOLLOWING
                ? OptionAvailability.ENABLED : OptionAvailability.disabled(line("not_following"));
    }

    /**
     * "Lead me somewhere": the Rogue names up to {@link #CHOICES} places and the owner picks one with the buttons
     * that replace this option ({@code GUIDE_TO_1..3}). Nothing is spent until a place is picked; finding nowhere
     * new costs nothing either.
     */
    private static boolean lead(MinibossEntity npc, ServerPlayerEntity player, CompanionRecord record, int argument) {
        if (!(npc.getWorld() instanceof ServerWorld world)) {
            return false;
        }
        MinecraftServer server = world.getServer();
        List<RumorSubject> places = RumorSources.structures(world, npc.getBlockPos(), player.getUuid(), RumorState.get(server),
                RPGMinibossesEntities.config, npc.getRandom(), CHOICES);
        if (places.isEmpty()) {
            Conversations.reply(npc, player, line("nowhere"));
            return true;
        }
        OFFERS.put(player.getUuid(), new Offer(npc.getUuid(), List.copyOf(places), Companions.now(server) + OFFER_TICKS));
        Conversations.reply(npc, player, line(places.size() == 1 ? "choose_one" : "choose"));
        return true;
    }

    /** One of the offered places, as a button: what it is, which way, about how far. */
    private static OptionAvailability choiceAvailability(int index, MinibossEntity npc, ServerPlayerEntity player) {
        CompanionRecord record = Companions.recordFor(player, npc);
        Offer offer = OFFERS.get(player.getUuid());
        if (record == null || offer == null || !offer.npc().equals(npc.getUuid()) || index >= offer.places().size()
                || Companions.now(player.getServer()) > offer.expiresAt() || record.mode != CompanionMode.FOLLOWING
                || CompanionSpecialties.usedToday(record, player.getServer())) {
            return OptionAvailability.HIDDEN;
        }
        RumorSubject place = offer.places().get(index);
        Vec3d to = place.position();
        double distance = Math.hypot(to.x - npc.getX(), to.z - npc.getZ());
        Bearing bearing = Bearing.of(to.x - npc.getX(), to.z - npc.getZ());
        long rounded = Math.max(50L, Math.round(distance / 50.0) * 50L);
        Text label = line("choice", place.name(), bearing.abbreviation(), rounded);
        Text detail = line("choice_detail", place.name(), bearing.label(), DistanceBand.of(distance, true).label());
        return OptionAvailability.labelled(label, detail);
    }

    private static boolean choose(int index, MinibossEntity npc, ServerPlayerEntity player) {
        Offer offer = OFFERS.remove(player.getUuid());
        CompanionRecord record = Companions.recordFor(player, npc);
        if (offer == null || record == null || index >= offer.places().size()) {
            return false;
        }
        start(npc, player, record, offer.places().get(index));
        return false; // The conversation ends: time to walk.
    }

    /** Sets off for {@code place}: marked told, today's lead spent, and the way announced in chat. */
    private static void start(MinibossEntity npc, ServerPlayerEntity player, CompanionRecord record, RumorSubject place) {
        MinecraftServer server = player.getServer();
        Vec3d to = place.position();
        Bearing bearing = Bearing.of(to.x - npc.getX(), to.z - npc.getZ());
        DistanceBand band = DistanceBand.of(Math.hypot(to.x - npc.getX(), to.z - npc.getZ()), true);
        RumorState.get(server).markTold(player.getUuid(), place.key());

        record.mode = CompanionMode.GUIDING;
        record.guideTarget = GlobalPos.create(place.dimension(), BlockPos.ofFloored(to));
        record.guideStructure = structureId(place.key());
        record.guideStartedAt = Companions.now(server);
        record.guideWaited = 0;
        npc.setSitting(true);
        CompanionSpecialties.markUsed(record, server);
        Companions.dirty(server, npc, record);
        tell(player, npc, line("start", place.name(), bearing.label(), band.label()));
    }

    /** The structure id inside a rumor key, {@code structure:<id>@<chunk>}. */
    private static Identifier structureId(String key) {
        int start = key.indexOf(':') + 1;
        int end = key.lastIndexOf('@');
        return Identifier.tryParse(end > start ? key.substring(start, end) : key.substring(start));
    }

    /**
     * Once a second from the companion's tick, while GUIDING. Leads toward the place, waiting for the owner when
     * they fall behind; waited on for longer than {@code companionGuideWaitTimeoutTicks}, it heads home instead,
     * and picks the lead back up if the owner catches it on the way.
     */
    static void tick(MinecraftServer server, ServerWorld world, MinibossEntity npc, CompanionRecord record) {
        npc.setSitting(true);
        ServerConfig config = RPGMinibossesEntities.config;
        ServerPlayerEntity owner = server.getPlayerManager().getPlayer(record.owner);
        GlobalPos target = record.guideTarget;
        if (owner == null || target == null || owner.getWorld() != world || !target.dimension().equals(world.getRegistryKey())) {
            stop(record, npc);
            return;
        }
        if (Companions.now(server) - record.guideStartedAt > config.companionGuideMaxTicks) {
            tell(owner, npc, line("gave_up"));
            stop(record, npc);
            return;
        }
        double wait = config.companionGuideWaitDistance;
        boolean ownerClose = owner.squaredDistanceTo(npc) <= wait * wait;
        if (record.guideReturning) {
            if (!ownerClose) {
                returnHome(server, world, npc, record, owner);
                return;
            }
            // Caught up with on the way home: the lead goes on.
            record.guideReturning = false;
            record.guideWaited = 0;
            tell(owner, npc, line("resume"));
        }

        double speed = leadSpeed(npc, owner, record);
        Vec3d goal = Vec3d.ofBottomCenter(target.pos());
        if (horizontal(npc.getPos(), goal) <= ARRIVED) {
            tell(owner, npc, line("arrived", RumorSources.structureName(record.guideStructure)));
            stop(record, npc);
            return;
        }
        if (GuideBoat.isBoating(npc)) {
            GuideBoat.second(world, npc, owner, record, wait);
        } else if (npc.getTarget() != null) {
            return; // A fight on the way: its combat AI has the body.
        } else if (!ownerClose) {
            npc.getNavigation().stop();
            npc.getLookControl().lookAt(owner, 30.0F, 30.0F);
            if (record.guideWaited++ % HINT_EVERY == 0) {
                owner.sendMessage(line("keep_up", npc.getName()), true);
            }
        } else {
            record.guideWaited = 0;
            walkToward(world, npc, record, owner, goal, speed);
        }
        // Waited on (ashore or in the boat) too long: home it goes.
        if ((long) record.guideWaited * 20L >= config.companionGuideWaitTimeoutTicks) {
            record.guideReturning = true;
            record.guideWaited = 0;
            npc.getNavigation().stop();
            tell(owner, npc, line("returning"));
        }
    }

    /**
     * Heading home (the rendezvous point, else the spawn point) at its own pace, by the same legs, vaults and
     * boats as leading; in a boat it rows alone. Home in another dimension means the usual simulated trip.
     * Arriving, it waits there and the lead is over.
     */
    private static void returnHome(MinecraftServer server, ServerWorld world, MinibossEntity npc, CompanionRecord record,
                                   ServerPlayerEntity owner) {
        GlobalPos home = Companions.baseOf(server, record.owner);
        if (!home.dimension().equals(world.getRegistryKey())) {
            GuideBoat.disembark(npc);
            clear(record);
            Companions.sendHome(server, record, npc);
            return;
        }
        Vec3d goal = Vec3d.ofBottomCenter(home.pos());
        if (horizontal(npc.getPos(), goal) <= HOME_REACHED) {
            GuideBoat.disembark(npc);
            clear(record);
            record.mode = CompanionMode.AT_BASE;
            npc.getNavigation().stop();
            return;
        }
        if (GuideBoat.isBoating(npc)) {
            GuideBoat.second(world, npc, owner, record, RPGMinibossesEntities.config.companionGuideWaitDistance);
            return;
        }
        if (npc.getTarget() != null) {
            return;
        }
        walkToward(world, npc, record, owner, goal, 1.0);
    }

    /**
     * Whether {@code path} swims: more than {@link #MAX_WATER_NODES} of its nodes stand in water. Land
     * navigation happily paths through water, and once partial paths count as progress, a swim into the lake
     * would win over taking the boat.
     */
    private static boolean swims(ServerWorld world, Path path) {
        int wet = 0;
        for (int i = 0; i < path.getLength(); i++) {
            BlockPos at = path.getNodePos(i);
            if (world.getFluidState(at).isIn(net.minecraft.registry.tag.FluidTags.WATER) && ++wet > MAX_WATER_NODES) {
                return true;
            }
        }
        return false;
    }

    /** Where a Rogue in a boat is rowing to: home while returning, otherwise the place it is leading to. */
    static @org.jetbrains.annotations.Nullable Vec3d rowingGoal(MinecraftServer server, CompanionRecord record) {
        if (record.guideReturning) {
            return Vec3d.ofBottomCenter(Companions.baseOf(server, record.owner).pos());
        }
        return record.guideTarget == null ? null : Vec3d.ofBottomCenter(record.guideTarget.pos());
    }

    /**
     * One leg toward {@code goal}: keeps the current leg going (at {@code speed}) until it is nearly done, then
     * plans the next, trying a whole and then half a {@link #STRIDE} at 0° and ±45°. A full path wins; otherwise
     * the partial path that makes the most progress. With neither it vaults a ledge, or else takes to the water by
     * boat; failing all of that it stays put and tries again next second.
     */
    private static void walkToward(ServerWorld world, MinibossEntity npc, CompanionRecord record, ServerPlayerEntity owner,
                                   Vec3d goal, double speed) {
        if (!npc.getNavigation().isIdle()) {
            npc.getNavigation().setSpeed(speed); // Keeps pace if the owner speeds up mid-leg.
            Path current = npc.getNavigation().getCurrentPath();
            if (current == null || current.getLength() - current.getCurrentNodeIndex() > REPLAN_NODES_LEFT) {
                return;
            }
            // Nearly at the end of this leg: plan the next one now rather than stopping first.
        }
        double left = horizontal(npc.getPos(), goal);
        Vec3d heading = new Vec3d(goal.x - npc.getX(), 0, goal.z - npc.getZ()).normalize();
        // A full path is best. Downhill, the point a whole stride ahead is often further below than a mob will
        // path down to, so every full path fails; rather than stand there a second and retry (a hitch), take the
        // partial path that gets furthest toward the goal, as long as it is real progress, and replan from there.
        Path best = null;
        double bestLeft = left - MIN_PROGRESS;
        for (double strideScale : STRIDE_SCALES) {
            double stride = Math.min(STRIDE * strideScale, left);
            for (double slant : SLANTS) {
                Vec3d leg = heading.rotateY((float) slant).multiply(stride);
                BlockPos column = BlockPos.ofFloored(npc.getX() + leg.x, npc.getY(), npc.getZ() + leg.z);
                if (!world.isChunkLoaded(column)) {
                    continue;
                }
                BlockPos ground = Companions.standableNear(world, world.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, column));
                Path path = npc.getNavigation().findPathTo(ground, 1);
                if (path == null || swims(world, path)) {
                    continue; // A swim is not a way forward: open water is what the boat is for.
                }
                if (path.reachesTarget()) {
                    npc.getNavigation().startMovingAlong(path, speed);
                    return;
                }
                var end = path.getEnd();
                if (end != null) {
                    double endLeft = horizontal(Vec3d.ofBottomCenter(end.getBlockPos()), goal);
                    if (endLeft < bestLeft) {
                        best = path;
                        bestLeft = endLeft;
                    }
                }
            }
        }
        if (best != null) {
            npc.getNavigation().startMovingAlong(best, speed);
            return;
        }
        if (!tryVault(world, npc, record, heading)) {
            GuideBoat.tryBoard(world, npc, owner, heading);
        }
    }

    /**
     * The navigation speed multiplier that keeps the Rogue ahead of its owner: whichever is faster of the
     * owner's full sprint (their movement speed with the sprint boost, so potions and gear count) and how
     * fast they actually went over the last second (horses, elytra hops, Speed effects), plus
     * {@link #PACE_MARGIN}. Never below 1.
     *
     * <p>Mobs and players do not convert movement speed into ground speed the same way. A player's input is
     * always full strength, so ground speed grows with movement speed. A mob's navigation sets its forward
     * input to the same value as its movement speed, and vanilla scales acceleration by both, so its ground
     * speed grows with the <i>square</i> of movement speed: a Rogue at 0.31 runs like a player at about
     * 0.095, slower than a sprint. Matching a player at speed {@code p} therefore takes {@code sqrt(p)}.
     */
    private static double leadSpeed(MinibossEntity npc, ServerPlayerEntity owner, CompanionRecord record) {
        double ownerSpeed = owner.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED);
        double sprint = owner.isSprinting() ? ownerSpeed : ownerSpeed * SPRINT;
        Vec3d now = owner.getPos();
        Vec3d last = record.guideOwnerLast;
        record.guideOwnerLast = now;
        double current = 0.0;
        if (last != null) {
            double perTick = horizontal(last, now) / 20.0;
            if (perTick <= MAX_MEASURED) {
                current = perTick / GROUND_BLOCKS_PER_SPEED;
            }
        }
        double pace = Math.max(sprint, current) * PACE_MARGIN;
        double own = Math.max(0.01, npc.getAttributeValue(EntityAttributes.GENERIC_MOVEMENT_SPEED));
        return Math.max(1.0, Math.sqrt(pace) / own);
    }

    /**
     * Looks up to {@link #VAULT_REACH} blocks ahead along {@code heading} for a ledge 1 to {@link #VAULT_HEIGHT}
     * blocks up, with room to stand on top and headroom for the jump, and leaps onto the lowest one found.
     * Only from the ground, and not again within {@link #VAULT_COOLDOWN} ticks.
     */
    private static boolean tryVault(ServerWorld world, MinibossEntity npc, CompanionRecord record, Vec3d heading) {
        long now = world.getTime();
        if (!npc.isOnGround() || now - record.guideVaultAt < VAULT_COOLDOWN) {
            return false;
        }
        BlockPos feet = npc.getBlockPos();
        for (int reach = 1; reach <= VAULT_REACH; reach++) {
            BlockPos front = BlockPos.ofFloored(npc.getX() + heading.x * reach, npc.getY(), npc.getZ() + heading.z * reach);
            for (int height = 1; height <= VAULT_HEIGHT; height++) {
                BlockPos top = front.up(height);
                if (!standable(world, top) || !clearAbove(world, feet, height + 2)) {
                    continue;
                }
                launch(world, npc, top, height);
                record.guideVaultAt = now;
                return true;
            }
        }
        return false;
    }

    /** Solid underfoot, two blocks of air, no fluid: somewhere it can land and stand. */
    private static boolean standable(ServerWorld world, BlockPos pos) {
        BlockPos below = pos.down();
        return world.getBlockState(below).isSideSolidFullSquare(world, below, net.minecraft.util.math.Direction.UP)
                && world.getBlockState(pos).getCollisionShape(world, pos).isEmpty()
                && world.getBlockState(pos.up()).getCollisionShape(world, pos.up()).isEmpty()
                && world.getFluidState(pos).isEmpty();
    }

    /** Nothing in the way of the jump straight up from where it stands. */
    private static boolean clearAbove(ServerWorld world, BlockPos feet, int blocks) {
        for (int up = 2; up <= blocks; up++) {
            BlockPos pos = feet.up(up);
            if (!world.getBlockState(pos).getCollisionShape(world, pos).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /** A vault in the air: where it is landing, and when it jumped. */
    private record Vault(Vec3d landing, double ledgeY, long startedAt) {
    }

    /** Vaults in progress, per companion. */
    private static final java.util.Map<MinibossEntity, Vault> VAULTING = new java.util.WeakHashMap<>();
    /** Forward speed once it has cleared the ledge, and the longest a vault may last. */
    private static final double VAULT_FORWARD = 0.3;
    private static final long VAULT_MAX_TICKS = 30L;

    /**
     * The jump, phase one: straight up, fast enough to rise the ledge's height plus a little under vanilla gravity
     * and drag. Going forward at the same time would only run it into the ledge's face, which stops it dead, so
     * the forward push waits for {@link #tickVault} to see its feet clear the top.
     */
    private static void launch(ServerWorld world, MinibossEntity npc, BlockPos top, int height) {
        double rise = height + 0.35;
        double up = Math.sqrt(2.0 * GRAVITY * rise) / DRAG;
        Vec3d landing = Vec3d.ofBottomCenter(top);
        Vec3d toLanding = new Vec3d(landing.x - npc.getX(), 0, landing.z - npc.getZ());
        npc.getNavigation().stop();
        npc.setVelocity(0.0, up, 0.0);
        npc.velocityModified = true;
        npc.setYaw((float) Math.toDegrees(Math.atan2(-toLanding.x, toLanding.z)));
        npc.setBodyYaw(npc.getYaw());
        VAULTING.put(npc, new Vault(landing, top.getY(), world.getTime()));
        npc.dispatcher.play("attacks", VAULT_CLIP, false, 1.0f);
        world.playSound(null, npc.getBlockPos(), net.minecraft.sound.SoundEvents.ENTITY_GOAT_LONG_JUMP,
                net.minecraft.sound.SoundCategory.NEUTRAL, 0.8F, 1.2F);
    }

    /** Every tick, from the companion's tick: the parts of leading that cannot wait a second (vaulting, rowing). */
    static void tickEveryTick(MinibossEntity npc) {
        tickVault(npc);
        if (GuideBoat.isBoating(npc) && npc.getWorld() instanceof ServerWorld world) {
            CompanionRecord record = CompanionRegistry.get(world.getServer()).ofNpc(npc.getUuid());
            if (record != null && record.mode == CompanionMode.GUIDING) {
                GuideBoat.tick(npc, rowingGoal(world.getServer(), record));
            }
        }
    }

    /**
     * Every tick, while a vault is in the air. Phase two: once its feet are level with the ledge top it is pushed
     * onto it. The vault ends when it lands (after leaving the ground) or after {@link #VAULT_MAX_TICKS}.
     */
    static void tickVault(MinibossEntity npc) {
        Vault vault = VAULTING.get(npc);
        if (vault == null) {
            return;
        }
        long elapsed = npc.getWorld().getTime() - vault.startedAt();
        if (elapsed > VAULT_MAX_TICKS || (elapsed > 3 && npc.isOnGround())) {
            VAULTING.remove(npc);
            return;
        }
        if (npc.getY() + 0.1 >= vault.ledgeY()) {
            Vec3d toLanding = new Vec3d(vault.landing().x - npc.getX(), 0, vault.landing().z - npc.getZ());
            if (toLanding.lengthSquared() > 0.04) {
                Vec3d push = toLanding.normalize().multiply(Math.min(VAULT_FORWARD, toLanding.length()));
                npc.setVelocity(push.x, npc.getVelocity().y, push.z);
                npc.velocityModified = true;
            }
        }
    }

    /** Back to following; the lead is over. */
    static void stop(CompanionRecord record, MinibossEntity npc) {
        GuideBoat.disembark(npc);
        record.mode = CompanionMode.FOLLOWING;
        clear(record);
        npc.setSitting(false);
        npc.getNavigation().stop();
    }

    static void clear(CompanionRecord record) {
        record.guideReturning = false;
        record.guideOwnerLast = null;
        record.guideTarget = null;
        record.guideStructure = null;
        record.guideWaited = 0;
    }

    private static double horizontal(Vec3d a, Vec3d b) {
        double dx = a.x - b.x;
        double dz = a.z - b.z;
        return Math.sqrt(dx * dx + dz * dz);
    }
}
