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
    private static  String W1 = "#rpg_series:loot_tier/tier_1_weapons";

    private static  String W2 = "#rpg_series:loot_tier/tier_2_weapons";
    private static  String W3 = "#rpg_series:loot_tier/tier_3_weapons";

    private  static String   A2 = "#rpg_series:loot_tier/tier_2_armors";
    private  static String   X2 = "#rpg_series:loot_tier/tier_2_accessories";

    private  static String   R2 = "#rpg_series:loot_tier/tier_2_relics";


    @Shadow
    public static LootConfig itemLootConfig;

    static {
        for(RPGMinibossesEntities.Entry entry : RPGMinibossesEntities.entries) {
            if(entry.shouldSpawn){
                itemLootConfig.injectors.put("rpg-minibosses:entities/"+entry.id.getPath(),
                        new LootConfig.Pool().bonus_rolls(0.2F).rolls(2)
                                .add(W1, true,3)
                                .add(W2, true,3)
                                .add(A2, true,2)
                                .add(X2)
                                .add(R2))
                ;
            }
            else{
                    itemLootConfig.injectors.put("rpg-minibosses:entities/"+entry.id.getPath(),
                            new LootConfig.Pool().bonus_rolls(0.2F).rolls(2)
                                    .add(W2, true,3)
                                    .add(W3, true,3)

                                    .add(A2, true,2)
                                    .add(X2)
                                    .add(R2))
                    ;
            }
        }
    }

}
