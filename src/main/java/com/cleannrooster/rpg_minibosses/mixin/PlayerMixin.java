package com.cleannrooster.rpg_minibosses.mixin;

import com.cleannrooster.rpg_minibosses.RPGMinibosses;
import com.cleannrooster.spellblades.mixin.SpellContainerHelperMixin;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.stat.Stat;
import net.minecraft.stat.Stats;
import net.minecraft.util.Identifier;
import net.spell_engine.api.spell.registry.SpellRegistry;
import net.spell_engine.internals.SpellHelper;
import net.spell_engine.internals.casting.SpellCast;
import net.spell_engine.internals.casting.SpellCasterEntity;
import net.spell_engine.internals.container.SpellContainerSource;
import net.spell_engine.internals.target.EntityRelations;
import net.spell_engine.internals.target.SpellTarget;
import net.spell_engine.utils.TargetHelper;
import org.apache.commons.compress.harmony.pack200.NewAttributeBands;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntity.class)
public class PlayerMixin {
    @Inject(at = @At("HEAD"), method = "increaseStat", cancellable = true)

    public void incrementStatRPGMINI(Identifier stat, int amount, CallbackInfo info) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        if(stat.equals(Stats.SPRINT_ONE_CM) &&  player instanceof ServerPlayerEntity serverPlayer && SpellContainerSource.getFirstSourceOfSpell(Identifier.of(RPGMinibosses.MOD_ID,"abberrath_nova"),player) != null){
            if(serverPlayer.getStatHandler().getStat(Stats.CUSTOM.getOrCreateStat(stat))  % 400 +amount >= 400){
                    SpellHelper.performSpell(player.getWorld(), player,SpellRegistry.from( player.getWorld()).getEntry(Identifier.of(RPGMinibosses.MOD_ID,"abberrath_nova")).get(), SpellTarget.SearchResult.of(TargetHelper.targetsFromArea(player,
                            SpellRegistry.from( player.getWorld()).getEntry(Identifier.of(RPGMinibosses.MOD_ID,"abberrath_nova")).get().value().range,
                            SpellRegistry.from( player.getWorld()).getEntry(Identifier.of(RPGMinibosses.MOD_ID,"abberrath_nova")).get().value().target.area, (target) -> EntityRelations.actionAllowed(SpellTarget.FocusMode.AREA, SpellTarget.Intent.HARMFUL,player,target)))
                    , SpellCast.Action.RELEASE,1.0F);
            }
        }
    }

}
