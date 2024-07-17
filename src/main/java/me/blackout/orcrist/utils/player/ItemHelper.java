package me.blackout.orcrist.utils.player;

import net.minecraft.item.Item;
import net.minecraft.item.Items;

import java.util.ArrayList;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class ItemHelper<itemStack> {

    public static ArrayList<Item> wools = new ArrayList<Item>() {{
        add(Items.WHITE_WOOL);
        add(Items.ORANGE_WOOL);
        add(Items.MAGENTA_WOOL);
        add(Items.LIGHT_BLUE_WOOL);
        add(Items.YELLOW_WOOL);
        add(Items.LIME_WOOL);
        add(Items.PINK_WOOL);
        add(Items.GRAY_WOOL);
        add(Items.LIGHT_GRAY_WOOL);
        add(Items.CYAN_WOOL);
        add(Items.PURPLE_WOOL);
        add(Items.BLUE_WOOL);
        add(Items.BROWN_WOOL);
        add(Items.GREEN_WOOL);
        add(Items.RED_WOOL);
        add(Items.BLACK_WOOL);
    }};
    public static ArrayList<Item> planks = new ArrayList<Item>() {{
        add(Items.OAK_PLANKS);
        add(Items.SPRUCE_PLANKS);
        add(Items.BIRCH_PLANKS);
        add(Items.JUNGLE_PLANKS);
        add(Items.ACACIA_PLANKS);
        add(Items.DARK_OAK_PLANKS);
    }};
    public static ArrayList<Item> shulkers = new ArrayList<Item>() {{
        add(Items.SHULKER_BOX);
        add(Items.BLACK_SHULKER_BOX);
        add(Items.BLUE_SHULKER_BOX);
        add(Items.BROWN_SHULKER_BOX);
        add(Items.GREEN_SHULKER_BOX);
        add(Items.RED_SHULKER_BOX);
        add(Items.WHITE_SHULKER_BOX);
        add(Items.LIGHT_BLUE_SHULKER_BOX);
        add(Items.LIGHT_GRAY_SHULKER_BOX);
        add(Items.LIME_SHULKER_BOX);
        add(Items.MAGENTA_SHULKER_BOX);
        add(Items.ORANGE_SHULKER_BOX);
        add(Items.PINK_SHULKER_BOX);
        add(Items.CYAN_SHULKER_BOX);
        add(Items.GRAY_SHULKER_BOX);
        add(Items.PURPLE_SHULKER_BOX);
        add(Items.YELLOW_SHULKER_BOX);
    }};

    // Utils
    public static Item getItemFromSlot(Integer slot) {
        if (slot == -1) return null;
        if (slot == 45) return mc.player.getOffHandStack().getItem();
        return mc.player.getInventory().getStack(slot).getItem();
    }
}
