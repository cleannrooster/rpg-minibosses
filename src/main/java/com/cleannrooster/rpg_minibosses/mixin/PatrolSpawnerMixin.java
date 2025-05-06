package com.cleannrooster.rpg_minibosses.mixin;

import com.cleannrooster.rpg_minibosses.entity.RPGMinibossesEntities;
import com.cleannrooster.rpg_minibosses.patrols.Patrol;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.spawner.PatrolSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = PatrolSpawner.class)

public class PatrolSpawnerMixin  {


    @Inject(at = @At("RETURN"), method = "spawn", cancellable = true)
    public void spawnRPG(ServerWorld world, boolean spawnMonsters, boolean spawnAnimals, CallbackInfoReturnable<Integer> callbackInfoReturnable) {
        if (!RPGMinibossesEntities.config.betrayal && RPGMinibossesEntities.config.despotism) {
            for (Patrol patrol : Patrol.patrolList) {
                int result = patrol.spawnRPG(world, spawnMonsters, spawnAnimals);
            }
        }
    }

}
