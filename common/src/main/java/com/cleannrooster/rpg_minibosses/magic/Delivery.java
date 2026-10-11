package com.cleannrooster.rpg_minibosses.magic;

import dev.eann.atlasvfx.telegraph.TelegraphArea;
import org.jetbrains.annotations.Nullable;

/**
 * How an ability reaches its targets. The replacement for Spell Engine's deliver types (DIRECT, AREA,
 * PROJECTILE, METEOR, CLOUD) for boss abilities.
 */
public sealed interface Delivery permits Delivery.Direct, Delivery.Area, Delivery.Projectile, Delivery.Meteor, Delivery.Cloud {

    /** Where an area or cloud is centred. */
    enum Anchor {
        /** On the caster, facing the way it committed. */
        CASTER,
        /** Where the target stood when the action committed — what lets a player dodge after the wind-up. */
        TARGET
    }

    /** The impacts land on the cast's target only. */
    record Direct() implements Delivery {
    }

    /**
     * Everything inside {@code shape}, moved to the anchor and turned to the committed facing.
     *
     * @param height how far above the shape's plane a target can stand and still be hit
     */
    record Area(TelegraphArea shape, Anchor anchor, double height) implements Delivery {
        public static Area around(TelegraphArea shape) {
            return new Area(shape, Anchor.CASTER, 3.0);
        }

        public static Area at(TelegraphArea shape) {
            return new Area(shape, Anchor.TARGET, 3.0);
        }
    }

    /** A projectile from the caster's eyes toward the target. */
    record Projectile(ProjectileSpec spec) implements Delivery {
    }

    /** A projectile dropped from {@code height} blocks above the target's committed position. */
    record Meteor(ProjectileSpec spec, double height) implements Delivery {
    }

    /** A lingering area that applies the impacts every {@code spec.intervalTicks()}. */
    record Cloud(CloudSpec spec, Anchor anchor) implements Delivery {
    }

    /**
     * @param speed         blocks per tick
     * @param gravity       blocks per tick² pulled down; 0 for a straight shot
     * @param homingDegrees how far per tick it may turn toward its target; 0 for none
     * @param homingDelay   ticks it flies straight before it starts homing (a volley that fans out first)
     * @param lifetimeTicks discarded after this many ticks if it has hit nothing
     * @param pierce        extra entities it may pass through after the first
     * @param burst         an area applied where it lands, or null for single-target
     */
    record ProjectileSpec(double speed, double gravity, float homingDegrees, int homingDelay, int lifetimeTicks,
                          int pierce, @Nullable Area burst) {
        public static ProjectileSpec straight(double speed) {
            return new ProjectileSpec(speed, 0.0, 0.0f, 0, 100, 0, null);
        }

        public ProjectileSpec homing(float degreesPerTick) {
            return homing(degreesPerTick, 0);
        }

        public ProjectileSpec homing(float degreesPerTick, int afterTicks) {
            return new ProjectileSpec(this.speed, this.gravity, degreesPerTick, afterTicks, this.lifetimeTicks, this.pierce, this.burst);
        }

        public ProjectileSpec withGravity(double gravity) {
            return new ProjectileSpec(this.speed, gravity, this.homingDegrees, this.homingDelay, this.lifetimeTicks, this.pierce, this.burst);
        }

        public ProjectileSpec withLifetime(int ticks) {
            return new ProjectileSpec(this.speed, this.gravity, this.homingDegrees, this.homingDelay, ticks, this.pierce, this.burst);
        }

        public ProjectileSpec piercing(int extraTargets) {
            return new ProjectileSpec(this.speed, this.gravity, this.homingDegrees, this.homingDelay, this.lifetimeTicks, extraTargets, this.burst);
        }

        /** Applies {@code shape} where it lands. The anchor of {@code shape} is ignored: it is the landing point. */
        public ProjectileSpec bursting(TelegraphArea shape, double height) {
            return new ProjectileSpec(this.speed, this.gravity, this.homingDegrees, this.homingDelay, this.lifetimeTicks,
                    this.pierce, new Area(shape, Anchor.TARGET, height));
        }
    }

    /**
     * @param shape          the area each pulse covers, centred on the cloud
     * @param height         as {@link Area#height}
     * @param durationTicks  how long the cloud lasts
     * @param intervalTicks  ticks between pulses; the first pulse lands immediately
     */
    record CloudSpec(TelegraphArea shape, double height, int durationTicks, int intervalTicks) {
    }
}
