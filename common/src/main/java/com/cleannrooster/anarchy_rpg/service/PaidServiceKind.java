package com.cleannrooster.anarchy_rpg.service;

import com.cleannrooster.anarchy_rpg.RPGMinibosses;
import net.minecraft.text.Text;

import java.util.Locale;
import java.util.function.Supplier;

/**
 * Every paid service, by name. The window is told which one it is selling (sent to the client by ordinal),
 * and the kind gives its button label; the server-side rules come from {@link #service()}.
 */
public enum PaidServiceKind {
    HIRE(HireService::new),
    LIFT_CURSE(LiftCurseService::new),
    BURN_ENCHANTMENT(BurnEnchantmentService::new),
    GAMBLE(GambleService::new),
    SELL_DIRECTIONS(SellDirectionsService::new),
    GUARD_CAMP(GuardCampService::new);

    private static final PaidServiceKind[] VALUES = values();

    private final Supplier<PaidService> factory;
    private PaidService service;

    PaidServiceKind(Supplier<PaidService> factory) {
        this.factory = factory;
    }

    /** The rules behind this kind. Server side only. */
    public PaidService service() {
        if (this.service == null) {
            this.service = this.factory.get();
        }
        return this.service;
    }

    /** The confirm button's label, e.g. "Hire". */
    public Text buttonLabel() {
        return Text.translatable("service." + RPGMinibosses.CONTENT_NAMESPACE + ".button." + this.name().toLowerCase(Locale.ROOT));
    }

    public static PaidServiceKind fromIndex(int index) {
        return index >= 0 && index < VALUES.length ? VALUES[index] : HIRE;
    }
}
