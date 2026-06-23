package com.cleannrooster.rpg_minibosses.block;

import com.cleannrooster.rpg_minibosses.RPGMinibosses;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

import static com.cleannrooster.rpg_minibosses.entity.RPGMinibossesEntities.KEY;

public class RPGMinibossesBlocks {
    public static Block MERCPOSTERBLOCK;
    public static BlockItem MERCPOSTERBLOCKITEM;

    public static Block MAGEPOSTERBLOCK;
    public static BlockItem MAGEPOSTERBLOCKITEM;
    public static Block JUGGPOSTERBLOCK;
    public static BlockItem JUGGPOSTERBLOCKITEM;
    public static Block ROGUEPOSTERBLOCK;
    public static BlockItem ROGUEPOSTERBLOCKITEM;
    public static Block TEMPLARPOSTERBLOCK;
    public static BlockItem TEMPLARPOSTERBLOCKITEM;

    /** Convenience for Fabric (registries open during init). NeoForge calls the phase methods. */
    public static void register(){
        registerBlocks();
        registerBlockItems();
        addToItemGroup();
    }

    /** BLOCK registry phase only. */
    public static void registerBlocks(){
        MERCPOSTERBLOCK = Registry.register(Registries.BLOCK, Identifier.of(RPGMinibosses.CONTENT_NAMESPACE,"poster_merc"),new PosterBlock(AbstractBlock.Settings.copy(net.minecraft.block.Blocks.OAK_FENCE).breakInstantly()));
        ROGUEPOSTERBLOCK = Registry.register(Registries.BLOCK, Identifier.of(RPGMinibosses.CONTENT_NAMESPACE,  "poster_rogue"),new PosterBlock(AbstractBlock.Settings.copy(net.minecraft.block.Blocks.OAK_FENCE).breakInstantly()));
        JUGGPOSTERBLOCK = Registry.register(Registries.BLOCK, Identifier.of(RPGMinibosses.CONTENT_NAMESPACE,  "poster_jugg"),new PosterBlock(AbstractBlock.Settings.copy(net.minecraft.block.Blocks.OAK_FENCE).breakInstantly()));
        MAGEPOSTERBLOCK = Registry.register(Registries.BLOCK, Identifier.of(RPGMinibosses.CONTENT_NAMESPACE,  "poster_mage"),new PosterBlock(AbstractBlock.Settings.copy(net.minecraft.block.Blocks.OAK_FENCE).breakInstantly()));
        TEMPLARPOSTERBLOCK = Registry.register(Registries.BLOCK, Identifier.of(RPGMinibosses.CONTENT_NAMESPACE,  "poster_templar"),new PosterBlock(AbstractBlock.Settings.copy(net.minecraft.block.Blocks.OAK_FENCE).breakInstantly()));
    }

    /** ITEM registry phase only. Requires {@link #registerBlocks()}. */
    public static void registerBlockItems(){
        MERCPOSTERBLOCKITEM = Registry.register(Registries.ITEM,Identifier.of(RPGMinibosses.CONTENT_NAMESPACE,"poster_merc"),new BlockItem(MERCPOSTERBLOCK, new Item.Settings()));
        ROGUEPOSTERBLOCKITEM = Registry.register(Registries.ITEM,Identifier.of(RPGMinibosses.CONTENT_NAMESPACE,"poster_rogue"),new BlockItem(ROGUEPOSTERBLOCK, new Item.Settings()));
        JUGGPOSTERBLOCKITEM = Registry.register(Registries.ITEM,Identifier.of(RPGMinibosses.CONTENT_NAMESPACE,"poster_jugg"),new BlockItem(JUGGPOSTERBLOCK, new Item.Settings()));
        MAGEPOSTERBLOCKITEM = Registry.register(Registries.ITEM,Identifier.of(RPGMinibosses.CONTENT_NAMESPACE,"poster_mage"),new BlockItem(MAGEPOSTERBLOCK, new Item.Settings()));
        TEMPLARPOSTERBLOCKITEM = Registry.register(Registries.ITEM,Identifier.of(RPGMinibosses.CONTENT_NAMESPACE,"poster_templar"),new BlockItem(TEMPLARPOSTERBLOCK, new Item.Settings()));
    }

    /** Event-callback registration; safe outside registry events. */
    public static void addToItemGroup(){
        ItemGroupEvents.modifyEntriesEvent(KEY).register((content) -> {
            content.add(MERCPOSTERBLOCKITEM);
            content.add(JUGGPOSTERBLOCKITEM);
            content.add(MAGEPOSTERBLOCKITEM);
            content.add(TEMPLARPOSTERBLOCKITEM);
            content.add(ROGUEPOSTERBLOCKITEM);
        });
    }
}
