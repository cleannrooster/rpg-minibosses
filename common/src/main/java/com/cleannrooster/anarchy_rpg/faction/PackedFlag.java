package com.cleannrooster.anarchy_rpg.faction;

import net.minecraft.block.entity.BannerPattern;
import net.minecraft.component.type.BannerPatternsComponent;
import net.minecraft.registry.DynamicRegistryManager;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.DyeColor;

/**
 * A faction flag packed into one int, so it can ride a tracked-data field or a payload:
 * {@code base | patternColor << 4 | (patternRawId + 1) << 8}, or {@link #NONE}.
 *
 * <p>The pattern travels as its raw id in the banner pattern registry. That registry is synced from server
 * to client, so raw ids agree on both ends of a connection (never persist a packed flag).
 */
public final class PackedFlag {
    public static final int NONE = -1;

    private PackedFlag() {
    }

    public static int pack(FactionFlag flag, DynamicRegistryManager registries) {
        Registry<BannerPattern> patterns = registries.get(RegistryKeys.BANNER_PATTERN);
        BannerPattern pattern = patterns.get(RegistryKey.of(RegistryKeys.BANNER_PATTERN, flag.pattern()));
        int rawId = pattern == null ? -1 : patterns.getRawId(pattern);
        return (flag.base().getId() & 0xF) | (flag.patternColor().getId() & 0xF) << 4 | (rawId + 1) << 8;
    }

    public static DyeColor base(int packed) {
        return DyeColor.byId(packed & 0xF);
    }

    /** The banner's pattern layers, or empty if the pattern is unknown here. */
    public static BannerPatternsComponent patterns(int packed, DynamicRegistryManager registries) {
        int rawId = (packed >>> 8) - 1;
        if (rawId < 0) {
            return BannerPatternsComponent.DEFAULT;
        }
        DyeColor color = DyeColor.byId((packed >>> 4) & 0xF);
        return registries.get(RegistryKeys.BANNER_PATTERN).getEntry(rawId)
                .map(pattern -> new BannerPatternsComponent.Builder().add(pattern, color).build())
                .orElse(BannerPatternsComponent.DEFAULT);
    }
}
