package com.cleannrooster.rpg_minibosses.mixin;

import com.cleannrooster.rpg_minibosses.RPGMinibosses;
import com.cleannrooster.rpg_minibosses.entity.RPGMinibossesEntities;
import net.spell_engine.rpg_series.RPGSeriesCore;
import net.spell_engine.rpg_series.config.Defaults;
import net.spell_engine.rpg_series.loot.LootConfig;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Defaults.class)
public class DefaultLootMixin {
    private static  String W2 = "#rpg_series:tier_2_weapons";
    private  static String   A2 = "#rpg_series:tier_2_armors";
    private  static String   X2 = "#rpg_series:tier_2_accessories";

    private  static String   R2 = "#rpg_series:tier_2_relics";


    @Shadow
    public static LootConfig itemLootConfig;

    static {
        for(RPGMinibossesEntities.Entry entry : RPGMinibossesEntities.entries) {
            itemLootConfig.injectors.put("rpg-minibosses:entities/"+entry.id.getPath(),
                    new LootConfig.Pool().rolls(2)
                            .add(W2, true)
                            .add(A2, true)
                            .add(X2)
                            .add(R2));
        }
    }

}
