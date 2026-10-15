package com.cleannrooster.anarchy_rpg.rumor;

import com.cleannrooster.anarchy_rpg.RPGMinibosses;
import com.cleannrooster.anarchy_rpg.companion.CompanionRecord;
import com.cleannrooster.anarchy_rpg.companion.CompanionRegistry;
import com.cleannrooster.anarchy_rpg.companion.Companions;
import com.cleannrooster.anarchy_rpg.config.ServerConfig;
import com.cleannrooster.anarchy_rpg.conversation.ConversationOption;
import com.cleannrooster.anarchy_rpg.conversation.ConversationOptions;
import com.cleannrooster.anarchy_rpg.conversation.Conversations;
import com.cleannrooster.anarchy_rpg.conversation.OptionAvailability;
import com.cleannrooster.anarchy_rpg.entity.MinibossEntity;
import com.cleannrooster.anarchy_rpg.entity.RPGMinibossesEntities;
import com.cleannrooster.anarchy_rpg.faction.Disposition;
import com.cleannrooster.anarchy_rpg.faction.FactionState;
import com.cleannrooster.anarchy_rpg.faction.Factions;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.mob.Monster;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

/**
 * Rumors (3.0 M5): ask an adventurer what it has heard, and it tells of a real structure, boss or player
 * nearby, as a bearing and a rough distance.
 *
 * <p>Each answer costs one of the adventurer's charges; finding nothing to tell costs none. How far to trust
 * it follows the asker's standing ({@link RumorAccuracy}): a false rumor is about a real subject, with the
 * bearing and/or distance wrong, and the player is never told which. Only true rumors are remembered as
 * told, so the truth can still be learned by asking again. When a player is the subject, they hear about it
 * as gossip the next time they talk to any adventurer.
 */
public final class Rumors {
    private static final String NS = RPGMinibosses.CONTENT_NAMESPACE;
    /** How many wordings each kind of rumor has in the lang file. */
    private static final int VARIANTS = 2;
    /** Owner kills count for their companion only when it is this close (it helped). */
    private static final double ASSIST_RANGE = 32.0;
    /** A companion "moving" farther than this in one second was teleported, not walking. */
    private static final double MAX_STEP = 64.0;

    /** Where each companion was a second ago, for counting the ground it covers. */
    private static final Map<MinibossEntity, Vec3d> LAST_POSITION = new WeakHashMap<>();

    private Rumors() {
    }

    public static void register() {
        ConversationOptions.register(ConversationOption.RUMORS, Rumors::availability, Rumors::tell);
        EntityEvent.LIVING_DEATH.register(Rumors::onDeath);
        RumorDebugCommand.register();
    }

    private static ServerConfig config() {
        return RPGMinibossesEntities.config;
    }

    private static OptionAvailability availability(MinibossEntity npc, ServerPlayerEntity player, Disposition disposition) {
        int charges = npc.getRumorCharges().current(config().rumorChargesMax);
        if (charges > 0) {
            return new OptionAvailability(true, true, Text.translatable("rumor." + NS + ".charges", charges));
        }
        return OptionAvailability.disabled(Text.translatable("rumor." + NS
                + (npc.getOwnerUuid() != null ? ".no_charges_companion" : ".no_charges")));
    }

    /** The Rumors option: find a subject, spend a charge, and say it. The conversation stays open. */
    private static boolean tell(MinibossEntity npc, ServerPlayerEntity player, int argument) {
        if (!(npc.getWorld() instanceof ServerWorld world)) {
            return false;
        }
        ServerConfig config = config();
        RumorState state = RumorState.get(world.getServer());
        Random random = npc.getRandom();

        List<RumorKind> kinds = new ArrayList<>(List.of(RumorKind.STRUCTURE, RumorKind.BOSS));
        if (config.rumorPlayers) {
            kinds.add(RumorKind.PLAYER);
        }
        Collections.shuffle(kinds, new java.util.Random(random.nextLong()));
        Optional<RumorSubject> found = Optional.empty();
        for (RumorKind kind : kinds) {
            found = RumorSources.find(kind, world, npc, player, state, config, random);
            if (found.isPresent()) {
                break;
            }
        }
        if (found.isEmpty()) {
            // Nothing to tell: no charge spent.
            Conversations.reply(npc, player, Text.translatable("rumor." + NS + ".nothing"));
            return true;
        }
        if (!npc.getRumorCharges().spend(config.rumorChargesMax)) {
            return false;
        }
        RumorSubject subject = found.get();
        RumorAccuracy accuracy = RumorAccuracy.of(Factions.disposition(npc, player));
        boolean truth = random.nextDouble() < accuracy.truthChance(config);
        Text line = phrase(subject, world, npc.getPos(), accuracy, truth, random);
        if (truth) {
            state.markTold(player.getUuid(), subject.key());
        }
        if (subject.kind() == RumorKind.PLAYER && subject.player() != null) {
            Bearing fromTarget = subject.dimension().equals(world.getRegistryKey())
                    ? Bearing.of(npc.getX() - subject.position().x, npc.getZ() - subject.position().z) : null;
            state.recordAskedAbout(subject.player(), new RumorState.AskedAbout(player.getName().getString(),
                    Factions.factionOf(npc), fromTarget, world.getServer().getOverworld().getTime()));
        }
        // The screen shows it; a copy in chat so it is still there after the window closes.
        player.sendMessage(Text.translatable("rumor." + NS + ".chat", npc.getName(), line).formatted(Formatting.GRAY));
        Conversations.reply(npc, player, line);
        com.cleannrooster.anarchy_rpg.journal.GuildJournal.recordRumor(player, npc, line);
        return true;
    }

    /**
     * Words a rumor. Precise ones use the 16-point bearing and a tight band; others the 8-point bearing and
     * a broad band. A false one gets a wrong bearing, a wrong band, or both.
     */
    private static Text phrase(RumorSubject subject, ServerWorld world, Vec3d from, RumorAccuracy accuracy,
                               boolean truth, Random random) {
        boolean sameWorld = subject.dimension().equals(world.getRegistryKey());
        String kind = subject.kind().name().toLowerCase(Locale.ROOT);
        if (!sameWorld && truth) {
            return Text.translatable("rumor." + NS + "." + kind + ".elsewhere", subject.name(), dimensionName(subject.dimension()));
        }
        boolean fine = accuracy.fine();
        Bearing bearing;
        DistanceBand band;
        if (sameWorld) {
            bearing = Bearing.of(subject.position().x - from.x, subject.position().z - from.z);
            band = DistanceBand.of(RumorSources.horizontalDistance(from, subject.position()), fine);
        } else {
            // A false rumor about someone in another world: placed somewhere in this one instead.
            bearing = Bearing.fromIndex(random.nextInt(16));
            band = DistanceBand.of(300 + random.nextInt(2000), fine);
        }
        if (!fine) {
            bearing = bearing.coarse();
        }
        if (!truth && sameWorld) {
            int wrong = random.nextInt(3); // 0: bearing, 1: distance, 2: both
            if (wrong != 1) {
                bearing = bearing.distort(random);
            }
            if (wrong != 0) {
                band = band.distort(random);
            }
        }
        int variant = 1 + random.nextInt(VARIANTS);
        return Text.translatable("rumor." + NS + "." + kind + "." + variant, subject.name(), bearing.label(), band.label());
    }

    private static Text dimensionName(RegistryKey<World> dimension) {
        var id = dimension.getValue();
        return Text.translatableWithFallback("rumor." + NS + ".dimension." + id.getNamespace() + "." + id.getPath(),
                "the " + id.getPath().replace('_', ' '));
    }

    /**
     * Called when a conversation opens. Marks the player as part of the adventurers' world (so they can be
     * the subject of rumors from now on) and hands over the oldest gossip about them, if any.
     */
    public static @Nullable Text onConversationOpened(ServerPlayerEntity player) {
        MinecraftServer server = player.getServer();
        RumorState state = RumorState.get(server);
        state.markEngaged(player.getUuid());
        RumorState.AskedAbout gossip = state.takeGossip(player.getUuid());
        if (gossip == null) {
            return null;
        }
        MutableText text = gossip.faction() != null
                ? Text.translatable("rumor." + NS + ".gossip.guild", gossip.askerName(), FactionState.roster(server).get(gossip.faction()).name())
                : Text.translatable("rumor." + NS + ".gossip.companion", gossip.askerName());
        if (gossip.fromTarget() != null) {
            text.append(" ").append(Text.translatable("rumor." + NS + ".gossip.direction", gossip.fromTarget().coarse().label()));
        }
        return text;
    }

    /**
     * Once a second from the adventurer's server tick. A wandering adventurer regains charges with world
     * time; a companion regains them by covering ground with its owner away from home.
     */
    public static void tick(MinibossEntity npc) {
        if (npc.age % 20 != 0 || !(npc.getWorld() instanceof ServerWorld world)) {
            return;
        }
        ServerConfig config = config();
        RumorCharges charges = npc.getRumorCharges();
        if (npc.getOwnerUuid() == null) {
            charges.regenerate(world.getTime(), config.rumorRegenTicks, config.rumorChargesMax);
            return;
        }
        Vec3d position = npc.getPos();
        Vec3d last = LAST_POSITION.put(npc, position);
        // A Rogue leading the way is "sitting" (its follow goal is off) but very much adventuring.
        boolean sitting = npc.isSitting() && !com.cleannrooster.anarchy_rpg.companion.GuideSpecialty.isGuiding(npc);
        if (last == null || sitting || npc.isIncapacitated() || config.rumorCompanionBlocksPerCharge <= 0) {
            return;
        }
        double step = RumorSources.horizontalDistance(last, position);
        if (step > MAX_STEP) {
            return;
        }
        GlobalPos base = Companions.baseOf(world.getServer(), npc.getOwnerUuid());
        double radius = config.companionRestRadius;
        boolean awayFromHome = !base.dimension().equals(world.getRegistryKey())
                || base.pos().getSquaredDistance(position) > radius * radius;
        if (awayFromHome) {
            charges.accrue(step / config.rumorCompanionBlocksPerCharge, config.rumorChargesMax);
        }
    }

    /** Fights won together count toward a companion's next rumor: its own kills, and its owner's nearby. */
    private static EventResult onDeath(LivingEntity killed, DamageSource source) {
        if (killed.getWorld().isClient() || config().rumorCompanionKillsPerCharge <= 0) {
            return EventResult.pass();
        }
        boolean hostile = killed instanceof Monster || (killed instanceof MinibossEntity miniboss && miniboss.getOwnerUuid() == null);
        if (!hostile) {
            return EventResult.pass();
        }
        MinibossEntity companion = null;
        if (source.getAttacker() instanceof MinibossEntity attacker && attacker.getOwnerUuid() != null) {
            companion = attacker;
        } else if (source.getAttacker() instanceof ServerPlayerEntity player) {
            CompanionRecord record = CompanionRegistry.get(player.getServer()).ofOwner(player.getUuid());
            if (record != null && player.getServerWorld().getEntity(record.npc()) instanceof MinibossEntity npc
                    && npc.squaredDistanceTo(player) <= ASSIST_RANGE * ASSIST_RANGE) {
                companion = npc;
            }
        }
        if (companion != null) {
            companion.getRumorCharges().accrue(1.0 / config().rumorCompanionKillsPerCharge, config().rumorChargesMax);
        }
        return EventResult.pass();
    }
}
