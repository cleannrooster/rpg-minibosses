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
/*    public static Block MERCPOSTERBLOCK;
    public static BlockItem MERCPOSTERBLOCKITEM;

    public static Block MAGEPOSTERBLOCK;
    public static BlockItem MAGEPOSTERBLOCKITEM;
    public static Block JUGGPOSTERBLOCK;
    public static BlockItem JUGGPOSTERBLOCKITEM;
    public static Block ROGUEPOSTERBLOCK;
    public static BlockItem ROGUEPOSTERBLOCKITEM;
    public static Block TEMPLARPOSTERBLOCK;
    public static BlockItem TEMPLARPOSTERBLOCKITEM;

    public static void register(){

        MERCPOSTERBLOCK = Registry.register(Registries.BLOCK, Identifier.of(RPGMinibosses.MOD_ID,"poster_merc"),new PosterBlock(AbstractBlock.Settings.copy(net.minecraft.block.Blocks.OAK_FENCE).breakInstantly()));
        MERCPOSTERBLOCKITEM = Registry.register(Registries.ITEM,Identifier.of(RPGMinibosses.MOD_ID,"poster_merc"),new BlockItem(MERCPOSTERBLOCK, new Item.Settings()));

        ROGUEPOSTERBLOCK = Registry.register(Registries.BLOCK, Identifier.of(RPGMinibosses.MOD_ID,  "poster_rogue"),new PosterBlock(AbstractBlock.Settings.copy(net.minecraft.block.Blocks.OAK_FENCE).breakInstantly()));
        ROGUEPOSTERBLOCKITEM = Registry.register(Registries.ITEM,Identifier.of(RPGMinibosses.MOD_ID,"poster_rogue"),new BlockItem(ROGUEPOSTERBLOCK, new Item.Settings()));

        JUGGPOSTERBLOCK = Registry.register(Registries.BLOCK, Identifier.of(RPGMinibosses.MOD_ID,  "poster_jugg"),new PosterBlock(AbstractBlock.Settings.copy(net.minecraft.block.Blocks.OAK_FENCE).breakInstantly()));
        JUGGPOSTERBLOCKITEM = Registry.register(Registries.ITEM,Identifier.of(RPGMinibosses.MOD_ID,"poster_jugg"),new BlockItem(JUGGPOSTERBLOCK, new Item.Settings()));

        MAGEPOSTERBLOCK = Registry.register(Registries.BLOCK, Identifier.of(RPGMinibosses.MOD_ID,  "poster_mage"),new PosterBlock(AbstractBlock.Settings.copy(net.minecraft.block.Blocks.OAK_FENCE).breakInstantly()));
        MAGEPOSTERBLOCKITEM = Registry.register(Registries.ITEM,Identifier.of(RPGMinibosses.MOD_ID,"poster_mage"),new BlockItem(MAGEPOSTERBLOCK, new Item.Settings()));

        TEMPLARPOSTERBLOCK = Registry.register(Registries.BLOCK, Identifier.of(RPGMinibosses.MOD_ID,  "poster_templar"),new PosterBlock(AbstractBlock.Settings.copy(net.minecraft.block.Blocks.OAK_FENCE).breakInstantly()));
        TEMPLARPOSTERBLOCKITEM = Registry.register(Registries.ITEM,Identifier.of(RPGMinibosses.MOD_ID,"poster_templar"),new BlockItem(TEMPLARPOSTERBLOCK, new Item.Settings()));

        ItemGroupEvents.modifyEntriesEvent(KEY).register((content) -> {
            content.add(MERCPOSTERBLOCKITEM);
            content.add(JUGGPOSTERBLOCKITEM);
            content.add(MAGEPOSTERBLOCKITEM);
            content.add(TEMPLARPOSTERBLOCKITEM);
            content.add(ROGUEPOSTERBLOCKITEM);

        });
    }*/
}
