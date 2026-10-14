package com.cleannrooster.anarchy_rpg.magic;

import com.cleannrooster.anarchy_rpg.RPGMinibosses;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import dev.eann.atlasvfx.telegraph.TelegraphArea;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import java.util.OptionalDouble;

/**
 * {@code /rpgminibosses ability <id> [power] [windupTicks]} — cast any registered ability from yourself at
 * what you are looking at, with an explicit base power (default 10). With a wind-up it first plays the
 * ability's wind-up (sound and telegraph) and lands that many ticks later, as a boss's action would. {@code /rpgminibosses backend} names the active
 * damage backend. Operators only. Also defines a handful of {@code debug/*} abilities that exercise every
 * delivery, so the mechanics can be checked before any boss uses them.
 */
public final class MagicDebugCommand {
    private static final double REACH = 48.0;

    private MagicDebugCommand() {
    }

    public static void register() {
        DebugAbilities.init();
        CommandRegistrationEvent.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("rpgminibosses")
                        .requires(source -> source.hasPermissionLevel(2))
                        .then(CommandManager.literal("backend").executes(context -> {
                            var name = MagicBackend.active().getClass().getSimpleName();
                            context.getSource().sendFeedback(() -> Text.literal("Damage backend: " + name), false);
                            return 1;
                        }))
                        .then(CommandManager.literal("ability")
                                .then(CommandManager.argument("id", IdentifierArgumentType.identifier())
                                        .suggests((context, builder) -> CommandSource.suggestIdentifiers(
                                                AbilityRegistry.all().stream().map(AbilitySpec::id), builder))
                                        .executes(context -> cast(context, 10.0, 0))
                                        .then(CommandManager.argument("power", DoubleArgumentType.doubleArg(0.0))
                                                .executes(context -> cast(context, DoubleArgumentType.getDouble(context, "power"), 0))
                                                .then(CommandManager.argument("windup", IntegerArgumentType.integer(0, 200))
                                                        .executes(context -> cast(context, DoubleArgumentType.getDouble(context, "power"),
                                                                IntegerArgumentType.getInteger(context, "windup")))))))));
    }

    private static int cast(CommandContext<ServerCommandSource> context, double power, int windup) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        Identifier id = IdentifierArgumentType.getIdentifier(context, "id");
        var spec = AbilityRegistry.get(id);
        if (spec == null) {
            context.getSource().sendError(Text.literal("No ability " + id));
            return 0;
        }
        var target = aim(player);
        var world = player.getServerWorld();
        var aimedAt = target.entity() != null ? target.entity().getName().getString() : target.position().toString();
        if (windup > 0) {
            AbilityExecutor.windup(world, player, spec, target, windup);
            TickScheduler.schedule(world, windup, () -> AbilityExecutor.cast(world, player, spec, target, OptionalDouble.of(power)));
            context.getSource().sendFeedback(() -> Text.literal("Winding up " + id + " at " + aimedAt + " for " + windup + " ticks"), false);
            return 1;
        }
        boolean cast = AbilityExecutor.cast(world, player, spec, target, OptionalDouble.of(power));
        context.getSource().sendFeedback(() -> Text.literal((cast ? "Cast " : "Nothing to aim ") + id + " at " + aimedAt), false);
        return cast ? 1 : 0;
    }

    /** The living entity under the crosshair within reach, else the block looked at, else a point ahead. */
    private static CastTarget aim(ServerPlayerEntity player) {
        Vec3d eye = player.getEyePos();
        Vec3d end = eye.add(player.getRotationVector().multiply(REACH));
        var box = player.getBoundingBox().stretch(player.getRotationVector().multiply(REACH)).expand(1.0);
        var entityHit = ProjectileUtil.raycast(player, eye, end, box,
                entity -> entity instanceof LivingEntity && !entity.isSpectator() && entity != player, REACH * REACH);
        if (entityHit != null && entityHit.getEntity() instanceof LivingEntity living) {
            return CastTarget.at(player, living);
        }
        var blockHit = player.raycast(REACH, 0.0f, false);
        return CastTarget.at(player, blockHit.getPos());
    }

    /** Sample abilities, one per delivery kind. */
    static final class DebugAbilities {
        private static final Vec3d O = Vec3d.ZERO;

        static void init() {
            AbilitySpec.builder(id("bolt"), MagicSchool.ARCANE)
                    .delivery(new Delivery.Projectile(Delivery.ProjectileSpec.straight(1.2).homing(4.0f)))
                    .damage(1.0f)
                    .build();
            AbilitySpec.builder(id("fireball"), MagicSchool.FIRE)
                    .delivery(new Delivery.Projectile(Delivery.ProjectileSpec.straight(1.0)
                            .bursting(new TelegraphArea.Circle(O, 3.0f), 3.0)))
                    .damage(1.0f)
                    .impact(new Impact.Ignite(3))
                    .build();
            AbilitySpec.builder(id("nova"), MagicSchool.FROST)
                    .delivery(Delivery.Area.around(new TelegraphArea.Circle(O, 6.0f)))
                    .impact(new Impact.Damage(0.8f, 1.5f, false))
                    .impact(new Impact.ApplyEffect(StatusEffects.SLOWNESS, 60, 1))
                    .build();
            AbilitySpec.builder(id("ring"), MagicSchool.SOUL)
                    .delivery(Delivery.Area.around(new TelegraphArea.Ring(O, 3.0f, 7.0f)))
                    .damage(1.0f)
                    .build();
            AbilitySpec.builder(id("cone"), MagicSchool.FIRE)
                    .delivery(Delivery.Area.around(new TelegraphArea.Cone(O, 0.0f, 8.0f, 35.0f)))
                    .damage(1.0f)
                    .impact(new Impact.Ignite(2))
                    .build();
            AbilitySpec.builder(id("line"), MagicSchool.LIGHTNING)
                    .delivery(Delivery.Area.around(new TelegraphArea.Line(O, 0.0f, 14.0f, 1.0f)))
                    .damage(1.2f)
                    .build();
            AbilitySpec.builder(id("meteor"), MagicSchool.FIRE)
                    .delivery(new Delivery.Meteor(Delivery.ProjectileSpec.straight(0.9)
                            .bursting(new TelegraphArea.Circle(O, 3.5f), 3.0), 14.0))
                    .damage(1.5f)
                    .build();
            AbilitySpec.builder(id("cloud"), MagicSchool.FROST)
                    .delivery(new Delivery.Cloud(new Delivery.CloudSpec(new TelegraphArea.Circle(O, 4.0f), 3.0, 100, 20),
                            Delivery.Anchor.TARGET))
                    .impact(new Impact.Damage(0.3f, 0.0f, true))
                    .build();
            AbilitySpec.builder(id("heal"), MagicSchool.HEALING)
                    .helpful()
                    .delivery(Delivery.Area.around(new TelegraphArea.Circle(O, 6.0f)))
                    .impact(new Impact.Heal(1.0f))
                    .build();
        }

        private static Identifier id(String name) {
            return RPGMinibosses.id("debug/" + name);
        }
    }
}
