package com.cleannrooster.rpg_minibosses.magic.entity;

import com.cleannrooster.rpg_minibosses.magic.AbilityExecutor;
import com.cleannrooster.rpg_minibosses.magic.AbilityPresentation;
import com.cleannrooster.rpg_minibosses.magic.MagicSchool;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.data.TrackedDataHandlerRegistry;
import com.cleannrooster.rpg_minibosses.magic.AbilitySpec;
import com.cleannrooster.rpg_minibosses.magic.Delivery;
import com.cleannrooster.rpg_minibosses.magic.ImpactContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * The carrier for {@link Delivery.Cloud}: sits where it was placed and applies its ability to everyone in
 * its area each interval, then expires. Transient, like {@link BossProjectile}.
 */
public class BossCloud extends Entity {
    private @Nullable AbilitySpec ability;
    private @Nullable Delivery.CloudSpec spec;
    private @Nullable ImpactContext context;
    private float yaw;

    private static final TrackedData<Byte> SCHOOL = DataTracker.registerData(BossCloud.class, TrackedDataHandlerRegistry.BYTE);
    private static final TrackedData<Float> RADIUS = DataTracker.registerData(BossCloud.class, TrackedDataHandlerRegistry.FLOAT);

    /** Client-side ambient visual, installed by the client; does nothing on a server. */
    public static Consumer<BossCloud> clientTick = cloud -> {
    };

    public BossCloud(EntityType<? extends BossCloud> type, World world) {
        super(type, world);
        this.noClip = true;
        this.setNoGravity(true);
    }

    public static BossCloud spawn(ServerWorld world, LivingEntity caster, AbilitySpec ability, Delivery.CloudSpec spec,
                                  ImpactContext context, Vec3d at, float yaw) {
        var cloud = new BossCloud(MagicEntities.CLOUD, world);
        cloud.ability = ability;
        cloud.spec = spec;
        cloud.context = context;
        cloud.yaw = yaw;
        cloud.getDataTracker().set(SCHOOL, (byte) ability.lookSchool().ordinal());
        cloud.getDataTracker().set(RADIUS, (float) spec.shape().extent());
        cloud.setPosition(at);
        world.spawnEntity(cloud);
        return cloud;
    }

    @Override
    protected void initDataTracker(DataTracker.Builder builder) {
        builder.add(SCHOOL, (byte) 0);
        builder.add(RADIUS, 1.0f);
    }

    public MagicSchool school() {
        var schools = MagicSchool.values();
        return schools[Math.floorMod(this.getDataTracker().get(SCHOOL), schools.length)];
    }

    public float radius() {
        return this.getDataTracker().get(RADIUS);
    }

    @Override
    public boolean shouldSave() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (!(this.getWorld() instanceof ServerWorld world)) {
            clientTick.accept(this);
            return;
        }
        if (this.ability == null || this.spec == null || this.context == null || this.age > this.spec.durationTicks()) {
            this.discard();
            return;
        }
        if ((this.age - 1) % Math.max(1, this.spec.intervalTicks()) == 0) {
            var area = new Delivery.Area(this.spec.shape(), Delivery.Anchor.CASTER, this.spec.height());
            AbilityExecutor.applyArea(world, this.ability, this.context.withDirect(this), area, this.getPos(), this.yaw, null);
            AbilityPresentation.cloudPulse(world, this.ability, this.getPos(), (float) this.spec.shape().extent());
        }
    }

    @Override
    protected void readCustomDataFromNbt(NbtCompound nbt) {
    }

    @Override
    protected void writeCustomDataToNbt(NbtCompound nbt) {
    }
}
