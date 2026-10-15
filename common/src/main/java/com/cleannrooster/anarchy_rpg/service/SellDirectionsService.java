package com.cleannrooster.anarchy_rpg.service;

import com.cleannrooster.anarchy_rpg.config.ServerConfig;
import com.cleannrooster.anarchy_rpg.conversation.Conversations;
import com.cleannrooster.anarchy_rpg.entity.MinibossEntity;
import com.cleannrooster.anarchy_rpg.entity.RPGMinibossesEntities;
import com.cleannrooster.anarchy_rpg.rumor.RumorSources;
import com.cleannrooster.anarchy_rpg.rumor.RumorState;
import com.cleannrooster.anarchy_rpg.rumor.RumorSubject;
import dev.architectury.event.events.common.PlayerEvent;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Rogue, "Sell directions" (camp service): for a price, the exact surface coordinates of one rumorable place.
 * It will not lead you there; that is a companion Rogue's specialty.
 *
 * <p>Picking the option runs the rumor structure search (places the player already knows are skipped) and the
 * payment window's title names the place, so you know what you are buying. Finding nowhere new, it says so and
 * no window opens. Paying marks the place told and hands over the coordinates: in chat (clickable in creative,
 * suggesting a teleport) and on a paper note, so they are not lost. This is the one way to get coordinates; plain
 * rumors never give them.
 */
final class SellDirectionsService extends CampService {

    /** The place a player's open window is selling: whose, and where. */
    private record Offer(UUID npc, RumorSubject place) {
    }

    private static final Map<UUID, Offer> OFFERS = new HashMap<>();

    static void registerCleanup() {
        PlayerEvent.PLAYER_QUIT.register(player -> OFFERS.remove(player.getUuid()));
    }

    @Override
    protected PaidServiceKind kind() {
        return PaidServiceKind.SELL_DIRECTIONS;
    }

    @Override
    protected double basePrice(ServerConfig config) {
        return config.campDirectionsPrice;
    }

    /** The conversation gate: find a place to sell, or say there is none. */
    static boolean beforeOpen(MinibossEntity npc, ServerPlayerEntity player) {
        if (!(npc.getWorld() instanceof ServerWorld world)) {
            return false;
        }
        List<RumorSubject> places = RumorSources.structures(world, npc.getBlockPos(), player.getUuid(),
                RumorState.get(world.getServer()), RPGMinibossesEntities.config, npc.getRandom(), 1);
        if (places.isEmpty()) {
            Conversations.reply(npc, player, CampServices.line("directions.nothing"));
            return false;
        }
        OFFERS.put(player.getUuid(), new Offer(npc.getUuid(), places.get(0)));
        return true;
    }

    private static @Nullable Offer offerFor(MinibossEntity npc, ServerPlayerEntity player) {
        Offer offer = OFFERS.get(player.getUuid());
        return offer != null && offer.npc().equals(npc.getUuid()) ? offer : null;
    }

    @Override
    public Text windowTitle(MinibossEntity npc, ServerPlayerEntity player) {
        Offer offer = offerFor(npc, player);
        return offer == null ? npc.getName() : CampServices.line("directions.title", offer.place().name());
    }

    @Override
    protected boolean deliver(MinibossEntity npc, ServerPlayerEntity player) {
        Offer offer = offerFor(npc, player);
        if (offer == null || !(npc.getWorld() instanceof ServerWorld world)) {
            return false;
        }
        OFFERS.remove(player.getUuid());
        RumorSubject place = offer.place();
        RumorState.get(world.getServer()).markTold(player.getUuid(), place.key());

        // The surface there, from the generator, so it works even where the chunk is not loaded.
        int x = (int) Math.floor(place.position().x);
        int z = (int) Math.floor(place.position().z);
        int y = world.getChunkManager().getChunkGenerator().getHeight(x, z, Heightmap.Type.WORLD_SURFACE_WG, world,
                world.getChunkManager().getNoiseConfig());
        BlockPos at = new BlockPos(x, y, z);
        String coordinates = at.getX() + " " + at.getY() + " " + at.getZ();

        MutableText shown = Text.literal(coordinates).formatted(Formatting.GREEN);
        if (player.isCreative()) {
            shown = shown.styled(style -> style
                    .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/tp @s " + coordinates))
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, CampServices.line("directions.click"))));
        }
        player.sendMessage(npc.getName().copy().append(": ").append(CampServices.line("directions.done", place.name(), shown)));

        ItemStack note = new ItemStack(Items.PAPER);
        note.set(DataComponentTypes.CUSTOM_NAME, CampServices.line("directions.note", place.name()).copy().formatted(Formatting.WHITE));
        note.set(DataComponentTypes.LORE, new LoreComponent(List.of(Text.literal(coordinates).formatted(Formatting.GRAY))));
        player.getInventory().offerOrDrop(note);
        world.playSound(null, player.getBlockPos(), SoundEvents.ITEM_BOOK_PAGE_TURN, SoundCategory.NEUTRAL, 0.8F, 1.0F);
        return true;
    }
}
