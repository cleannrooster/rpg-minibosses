package com.cleannrooster.anarchy_rpg.hub;

import com.cleannrooster.anarchy_rpg.RPGMinibosses;
import com.cleannrooster.anarchy_rpg.entity.RPGMinibossesEntities;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.map.MapDecorationTypes;
import net.minecraft.item.map.MapState;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.structure.Structure;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Every player starts out with a map to the nearest adventurers' camp ({@code startWithCampMap}, on by default),
 * since camps are where adventurers live and trade and where the mod's services are.
 *
 * <p>It is a vanilla explorer map, made the way a cartographer makes one: the terrain sketched in and a red X on the
 * camp. Given once per player: a command tag on the player (saved with them) records it, set only once the map is
 * actually handed over. If no camp is within reach, nothing is given and it is tried again on their next join.
 *
 * <p>It is handed over {@code campMapDelayTicks} after joining, not on the join itself. Starter-kit mods (Serilum's
 * Starter Kit and the like) act on first join too, some clearing or replacing the inventory, and join handlers run
 * in no fixed order between mods; arriving a little later, the map lands in whatever inventory the kit left.
 */
public final class CampMap {
    /** Any of the camps; towers are hubs too, but the map is to a camp. */
    private static final TagKey<Structure> CAMPS = TagKey.of(RegistryKeys.STRUCTURE, RPGMinibosses.id("hub_camps"));
    private static final String GIVEN_TAG = RPGMinibosses.CONTENT_NAMESPACE + ".camp_map_given";
    /** How far to search, in chunks: as far as vanilla's /locate. */
    private static final int SEARCH_RADIUS = 100;
    /** Explorer-map scale, as a cartographer's maps use. */
    private static final byte SCALE = 2;

    private CampMap() {
    }

    /** Players waiting for their map, and the server tick it is due. */
    private static final Map<UUID, Integer> DUE = new HashMap<>();

    public static void register() {
        PlayerEvent.PLAYER_JOIN.register(player -> {
            if (RPGMinibossesEntities.config.startWithCampMap && !player.getCommandTags().contains(GIVEN_TAG)) {
                DUE.put(player.getUuid(), player.getServer().getTicks() + Math.max(0, RPGMinibossesEntities.config.campMapDelayTicks));
            }
        });
        PlayerEvent.PLAYER_QUIT.register(player -> DUE.remove(player.getUuid()));
        LifecycleEvent.SERVER_STOPPED.register(server -> DUE.clear());
        TickEvent.SERVER_POST.register(server -> {
            if (DUE.isEmpty()) {
                return;
            }
            int now = server.getTicks();
            DUE.entrySet().removeIf(entry -> {
                if (now < entry.getValue()) {
                    return false;
                }
                ServerPlayerEntity player = server.getPlayerManager().getPlayer(entry.getKey());
                if (player != null) {
                    give(player);
                }
                return true;
            });
        });
    }

    private static void give(ServerPlayerEntity player) {
        if (!RPGMinibossesEntities.config.startWithCampMap || player.getCommandTags().contains(GIVEN_TAG) || !player.isAlive()) {
            return;
        }
        // Camps are an overworld thing: search there, from the player if they are in it, else from world spawn.
        ServerWorld overworld = player.getServer().getOverworld();
        BlockPos from = player.getWorld().getRegistryKey() == World.OVERWORLD ? player.getBlockPos() : overworld.getSpawnPos();
        BlockPos camp = overworld.locateStructure(CAMPS, from, SEARCH_RADIUS, false);
        if (camp == null) {
            return;
        }
        ItemStack map = FilledMapItem.createMap(overworld, camp.getX(), camp.getZ(), SCALE, true, true);
        FilledMapItem.fillExplorationMap(overworld, map);
        MapState.addDecorationsNbt(map, camp, "+", MapDecorationTypes.RED_X);
        map.set(DataComponentTypes.CUSTOM_NAME, Text.translatable("item." + RPGMinibosses.CONTENT_NAMESPACE + ".camp_map")
                .formatted(Formatting.YELLOW));
        player.getInventory().offerOrDrop(map);
        player.addCommandTag(GIVEN_TAG);
        player.sendMessage(Text.translatable("hub." + RPGMinibosses.CONTENT_NAMESPACE + ".camp_map_given").formatted(Formatting.GRAY));
    }
}
