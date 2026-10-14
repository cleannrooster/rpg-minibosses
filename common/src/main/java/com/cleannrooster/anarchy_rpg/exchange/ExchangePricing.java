package com.cleannrooster.anarchy_rpg.exchange;

import com.cleannrooster.anarchy_rpg.config.ServerConfig;
import com.cleannrooster.anarchy_rpg.entity.RPGMinibossesEntities;
import com.cleannrooster.anarchy_rpg.faction.StandingThresholds;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;

import java.util.List;

/**
 * Prices an exchange. The cost of what is requested is its value, raised by a markup that grows with every
 * stack requested, scaled by how the adventurer regards the player:
 * <pre>
 *   cost  = value(request) x (1 + markupPerStack x (stacks - 1)) x dispositionFactor(standing)
 *   offer = value(offer)
 * </pre>
 * A trade goes through when the offer is worth at least the cost. Offered items are destroyed by the trade,
 * so nothing a player sells can be bought back, and the offer side needs no anti-exploit margin.
 */
public final class ExchangePricing {

    /** One quote, as shown to the player. */
    public record Quote(double cost, double offer) {
        public static final Quote EMPTY = new Quote(0.0, 0.0);

        public boolean acceptable() {
            return this.cost > 0.0 && this.offer + 1.0E-6 >= this.cost;
        }
    }

    private ExchangePricing() {
    }

    /**
     * @param standing the player's standing with the adventurer's faction
     * @param owner    whether the player owns this adventurer; an owner always gets the devoted price
     */
    public static Quote quote(List<ItemStack> request, List<ItemStack> offer, int standing, boolean owner) {
        double requestValue = ItemValues.total(request);
        if (requestValue <= 0.0) {
            return new Quote(0.0, ItemValues.total(offer));
        }
        long stacks = request.stream().filter(stack -> !stack.isEmpty()).count();
        ServerConfig config = RPGMinibossesEntities.config;
        double markup = 1.0 + config.exchangeMarkupPerStack * Math.max(0, stacks - 1);
        double factor = owner ? config.exchangeDevotedPriceMultiplier : dispositionFactor(standing);
        return new Quote(requestValue * markup * factor, ItemValues.total(offer));
    }

    /**
     * A smooth curve over standing rather than a step per tier, so prices move as standing moves: the angry
     * multiplier at the hostile threshold (the angriest standing that still talks), 1.0 at a standing of 0,
     * and the devoted multiplier at the devoted threshold. It never goes past either end.
     */
    public static double dispositionFactor(int standing) {
        ServerConfig config = RPGMinibossesEntities.config;
        StandingThresholds thresholds = StandingThresholds.from(config);
        if (standing < 0) {
            double t = thresholds.hostileBelow() == 0 ? 1.0 : MathHelper.clamp((double) standing / thresholds.hostileBelow(), 0.0, 1.0);
            return MathHelper.lerp(t, 1.0, config.exchangeAngryPriceMultiplier);
        }
        double t = thresholds.devotedAbove() <= 0 ? 1.0 : MathHelper.clamp((double) standing / thresholds.devotedAbove(), 0.0, 1.0);
        return MathHelper.lerp(t, 1.0, config.exchangeDevotedPriceMultiplier);
    }
}
