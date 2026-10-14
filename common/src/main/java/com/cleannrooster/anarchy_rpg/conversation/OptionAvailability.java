package com.cleannrooster.anarchy_rpg.conversation;

import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

/** Whether an option shows in a conversation, and if it shows, whether it can be picked and why not. */
public record OptionAvailability(boolean shown, boolean enabled, @Nullable Text reason) {
    public static final OptionAvailability HIDDEN = new OptionAvailability(false, false, null);
    public static final OptionAvailability ENABLED = new OptionAvailability(true, true, null);

    public static OptionAvailability disabled(Text reason) {
        return new OptionAvailability(true, false, reason);
    }
}
