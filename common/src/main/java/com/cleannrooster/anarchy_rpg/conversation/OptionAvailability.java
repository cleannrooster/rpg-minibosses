package com.cleannrooster.anarchy_rpg.conversation;

import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

/**
 * Whether an option shows in a conversation, and if it shows, whether it can be picked and why not. {@code label}
 * replaces the option's usual name when an option's text depends on the moment (a place to be led to).
 */
public record OptionAvailability(boolean shown, boolean enabled, @Nullable Text reason, @Nullable Text label) {
    public OptionAvailability(boolean shown, boolean enabled, @Nullable Text reason) {
        this(shown, enabled, reason, null);
    }

    public static final OptionAvailability HIDDEN = new OptionAvailability(false, false, null);
    public static final OptionAvailability ENABLED = new OptionAvailability(true, true, null);

    public static OptionAvailability disabled(Text reason) {
        return new OptionAvailability(true, false, reason);
    }

    /** Shown and enabled under its own label, with {@code tooltip} on hover. */
    public static OptionAvailability labelled(Text label, @Nullable Text tooltip) {
        return new OptionAvailability(true, true, tooltip, label);
    }
}
