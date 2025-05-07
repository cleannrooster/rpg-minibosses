package com.cleannrooster.rpg_minibosses.mixin;

import com.cleannrooster.rpg_minibosses.RPGMinibosses;
import com.cleannrooster.rpg_minibosses.entity.RPGMinibossesEntities;
import net.spell_engine.api.loot.LootConfigV2;
import net.spell_engine.rpg_series.RPGSeriesCore;
import net.spell_engine.rpg_series.config.Defaults;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Defaults.class)
public class DefaultLootMixin {
    private static  String W1 = "#rpg_series:tier_1_weapons";

    private static  String W2 = "#rpg_series:tier_2_weapons";
    private static  String W3 = "#rpg_series:tier_3_weapons";

    private  static String   A2 = "#rpg_series:tier_2_armors";
    private  static String   X2 = "#rpg_series:tier_2_accessories";



    @Shadow
    public static LootConfigV2 lootConfig;

    static {
        for(RPGMinibossesEntities.Entry entry : RPGMinibossesEntities.entries) {
            if(entry.shouldSpawn){
                lootConfig.injectors.put("rpg-minibosses:entities/"+entry.id.getPath(),
                        new LootConfigV2.Pool().bonus_rolls(0.2F).rolls(2)
                                .add(W1, 3,true)
                                .add(W2, 3,true)
                                .add(A2, 2,true)
                                .add(X2))
                ;
            }
            else{
                lootConfig.injectors.put("rpg-minibosses:entities/"+entry.id.getPath(),
                            new LootConfigV2.Pool().bonus_rolls(0.2F).rolls(2)
                                    .add(W2,3, true)
                                    .add(W3,3, true)
                                    .add(A2,2,true)
                                    .add(X2))
                    ;
            }
        }
    }

}
