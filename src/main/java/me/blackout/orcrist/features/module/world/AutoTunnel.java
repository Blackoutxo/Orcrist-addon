package me.blackout.orcrist.features.module.world;

import me.blackout.orcrist.OrcristAddon;
import me.blackout.orcrist.utils.world.BlockHelper;
import me.blackout.orcrist.utils.world.TimerUtils;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.combat.Offhand;
import meteordevelopment.meteorclient.systems.modules.player.AutoEat;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.Pool;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;
import meteordevelopment.meteorclient.utils.player.*;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockIterator;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.SlabBlock;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.PickaxeItem;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class AutoTunnel extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgTunnel = settings.createGroup("Tunnel");
    private final SettingGroup sgAutomend = settings.createGroup("Auto Mend");
    private final SettingGroup sgAutoeat = settings.createGroup("Auto Eat");
    private final SettingGroup sgShulker = settings.createGroup("Shulker");
    private final SettingGroup sgAutomation = settings.createGroup("Automation");
    private final SettingGroup sgRender = settings.createGroup("Render");

    // General
    private final Setting<Integer> maxBlocksPerTick = sgGeneral.add(new IntSetting.Builder().name("max-blocks-per-tick").description("Maximum blocks to try to break per tick. Useful when insta mining.").defaultValue(1).min(1).sliderRange(1, 6).build());
    private final Setting<Boolean> strictAxis = sgGeneral.add(new BoolSetting.Builder().name("strict-axis").description("Stays on the exact same height.").defaultValue(true).build());
    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder().name("rotate").description("Rotates to the blocks being placed & broke.").defaultValue(true).build());
    private final Setting<List<Block>> block = sgGeneral.add(new BlockListSetting.Builder().name("blocks").description("Blocks to be used for scaffolding, backfilling  & filling lava pockets.").defaultValue(Blocks.NETHERRACK).build());

    // Tunnel
    private final Setting<Integer> width = sgTunnel.add(new IntSetting.Builder().name("width").description("Width of the tunnel.").sliderRange(1, 3).defaultValue(1).build());
    private final Setting<Integer> height = sgTunnel.add(new IntSetting.Builder().name("height").description("Height of the tunnel.").sliderRange(1, 3).defaultValue(2).build());
    private final Setting<Integer> depth = sgTunnel.add(new IntSetting.Builder().name("depth").description("Depth of the tunnel.").sliderRange(1, 3).defaultValue(2).build());

    // Auto Mend
    private final Setting<Boolean> autoMend = sgAutomend.add(new BoolSetting.Builder().name("auto-mend").description("Automatically mends your pickaxe.").defaultValue(true).build());
    private final Setting<Boolean> mendWhileMine = sgAutomend.add(new BoolSetting.Builder().name("mend-while-mine").description("Will mend your tool when you're mining.").defaultValue(true).build());
    private final Setting<Integer> bottleSlot = sgAutomend.add(new IntSetting.Builder().name("bottle-slot").description("Bottle slot to be at.").sliderRange(1, 9).visible(() -> !mendWhileMine.get()).defaultValue(2).build());
    private final Setting<Double> minThreshold = sgAutomend.add(new DoubleSetting.Builder().name("min-threshold").description("Minimum threshold for pickaxe to be started to repair at.").sliderRange(1, 100).defaultValue(40).visible(autoMend::get).build());
    private final Setting<Double> maxThreshold = sgAutomend.add(new DoubleSetting.Builder().name("max-threshold").description("Maximum threshold the pickaxe should be at.").sliderRange(1, 100).defaultValue(80).visible(autoMend::get).build());

    // Auto Eat
    private final Setting<Boolean> autoEat = sgAutoeat.add(new BoolSetting.Builder().name("auto-eat").description("Automatically eat foods.").defaultValue(true).build());
    private final Setting<List<Item>> blacklist = sgAutoeat.add(new ItemListSetting.Builder().name("blacklist").description("Which items to not eat.")        .defaultValue(Items.ENCHANTED_GOLDEN_APPLE, Items.GOLDEN_APPLE, Items.CHORUS_FRUIT, Items.POISONOUS_POTATO, Items.PUFFERFISH, Items.CHICKEN, Items.ROTTEN_FLESH, Items.SPIDER_EYE, Items.SUSPICIOUS_STEW).filter(item -> item.getComponents().get(DataComponentTypes.FOOD) != null).build());
    private final Setting<Integer> hungerThreshold = sgAutoeat.add(new IntSetting.Builder().name("hunger-threshold").description("The level of hunger you eat at.").defaultValue(16).range(1, 19).sliderRange(1, 19).build());

    // Shulker
    private final Setting<Boolean> useShulker = sgShulker.add(new BoolSetting.Builder().name("use-shulker").description("Uses shulker to retrieve foods, pickaxe & xp bottles.").defaultValue(true).build());
    private final Setting<Integer> activateAt = sgShulker.add(new IntSetting.Builder().name("activate-at").description("Amount of items that should be in inventory in order to start getting items from shulker.").sliderRange(1, 10).defaultValue(1).build());
    private final Setting<Integer> itemAmt = sgShulker.add(new IntSetting.Builder().name("item-amount").description("Amount of item to retrieve from shulker.").sliderRange(1, 10).defaultValue(4).build());
    private final Setting<List<Item>> stealItems = sgShulker.add(new ItemListSetting.Builder().name("steal").description("Items to steal.").build());
    private final Setting<List<Block>> dumpItem = sgShulker.add(new BlockListSetting.Builder().name("dump-items").description("Dumps every items that is in the list except one whole stack which is for scaffolding.").defaultValue(Blocks.NETHERRACK).build());

    // Automation
    //private final Setting<Boolean> backFill = sgAutomation.add(new BoolSetting.Builder().name("back-fill").description("Fills your back previously dug tunnel.").defaultValue(false).build());
    private final Setting<Boolean> autoPot = sgAutomation.add(new BoolSetting.Builder().name("auto-pot").description("Uses speed potions.").defaultValue(true).build());
    private final Setting<Integer> potSlot = sgAutomation.add(new IntSetting.Builder().name("pot-slot").description("Which slot the potion should be at.").sliderRange(1, 9).defaultValue(3).build());
    private final Setting<Boolean> autoSwitch = sgAutomation.add(new BoolSetting.Builder().name("auto-switch").description("Automatically switches to pickaxe.").defaultValue(true).build());
    private final Setting<Integer> pickaxeSlot = sgAutomation.add(new IntSetting.Builder().name("pickaxe-slot").description("Which slot to move pickaxe to.").sliderRange(1, 9).defaultValue(1).build());
    private final Setting<Boolean> fillLiquid = sgAutomation.add(new BoolSetting.Builder().name("fill-liquid").description("Fills lava pocket.").defaultValue(true).build());
    private final Setting<Integer> heightL = sgAutomation.add(new IntSetting.Builder().name("sphere-height").description("Height of sphere to fill lava.").sliderRange(1, 3).defaultValue(3).visible(fillLiquid::get).build());
    private final Setting<Integer> radius = sgAutomation.add(new IntSetting.Builder().name("sphere-radius").description("Radius of sphere to fill lava.").sliderRange(1, 3).defaultValue(3).visible(fillLiquid::get).build());

    // Render
    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder().name("render").description("Renders stuff.").defaultValue(true).build());
    private final Setting<Boolean> swingHand = sgRender.add(new BoolSetting.Builder().name("swing-hand").description("Swing hand client side.").defaultValue(true).build());
    private final Setting<Boolean> displayInfo = sgRender.add(new BoolSetting.Builder().name("display-info").description("Displays info when module is deactivated.").defaultValue(true).build());
    private final Setting<Boolean> renderAutomation = sgRender.add(new BoolSetting.Builder().name("render-automation").description("Renders automations.").defaultValue(true).build());
    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>().name("shape-mode").description("How the shapes are rendered.").defaultValue(ShapeMode.Both).build());
    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder().name("side-color").description("The side color of the target block rendering.").defaultValue(new SettingColor(197, 137, 232, 10)).build());
    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder().name("line-color").description("The line color of the target block rendering.").defaultValue(new SettingColor(197, 137, 232)).build());

    // Fields
    private final Pool<BlockPos.Mutable> blockPosPool = new Pool<>(BlockPos.Mutable::new);
    private final List<BlockPos.Mutable> blocks = new ArrayList<>();

    private BlockPos.Mutable pos1 = new BlockPos.Mutable();
    private BlockPos.Mutable pos2 = new BlockPos.Mutable();

    private final ItemStack[] containerItems = new ItemStack[9 * 3];

    private final TimerUtils rubberbandTimer = new TimerUtils();

    private boolean canMove, mend;
    private boolean mine;

    public static int distance;
    private Vec3d start;
    private int blocksBroken;

    private Box box;

    private int yaw;

    int maxh = 0;
    int maxv = 0;

    public AutoTunnel() {
        super(OrcristAddon.World, "auto-tunnel", "Automatically digs up tunnel for you.");
    }

    @Override
    public void onActivate() {
        mine = false;

        distance = 0;
        start = mc.player.getPos();
        blocksBroken = 0;
    }

    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event) {
        long c = System.currentTimeMillis();
         if (event.packet instanceof PlayerPositionLookS2CPacket && scaffold()) {
             canMove = c == 1000;
         } else c = 0;
    }

    @EventHandler
    private void onTickPre(TickEvent.Pre event) {
        // Item
        FindItemResult pickaxe = InvUtils.find(itemStack -> itemStack.getItem() instanceof PickaxeItem);

        // Check value
        if (width.get() > 3 || width.get() < 1 || height.get() > 3 || height.get() < 2) {
            info("Height must be at least 2 blocks tall and less than 4, width must be at least 1 and less than 4....disabling");
            toggle();
        }

        // Search Item
        if (!InvUtils.find(itemStack -> block.get().contains(Block.getBlockFromItem(itemStack.getItem()))).found()) {
            error("Suitable blocks not found for scaffolding, filling liquid.....disabling");
            toggle();
            return;
        }

        // Re-level
        if (strictAxis.get()) reLevel(start);

        // Move pickaxe to said slot
        if (autoSwitch.get() && canMove && pickaxe.slot() != (pickaxeSlot.get() - 1)) InvUtils.move().from(pickaxe.slot()).to(pickaxeSlot.get() - 1);

        // Switch to pickaxe only if moving
        if (canMove) InvUtils.swap(pickaxe.slot(), false);

        // Calculate some stuff
        double pX = mc.player.getX();
        double pY = strictAxis.get() ? start.y : mc.player.getY();
        double pZ = mc.player.getZ();

        // Some render stuff
        int direction = Math.round((mc.player.getRotationClient().y % 360) / 90);
        direction = Math.floorMod(direction, 4);
        int addition = ((width.get() % 2 == 0) ? 0 : 1);

        /* direction 1 (yaw angle 90): West
           direction 2 (yaw angle 180): North
           direction 3 (yaw angle 270): East
           direction 0 (yaw angle 360, also equivalent to 0): South */

        // direction
        if (direction == 1) {
            pos1.set(pX, pY, pZ + width.get() / 2 + addition);
            pos2.set(pX - depth.get(), pY + height.get(), pZ - width.get() / 2);
        } else if (direction == 0) {
            pos1.set(pX + width.get() / 2 + addition, pY, pZ);
            pos2.set(pX - width.get() / 2, pY + height.get(), pZ + depth.get());
        } else if (direction == 2) {
            pos1.set(pX - width.get() / 2, pY, pZ);
            pos2.set(pX + width .get()/ 2 + addition, pY + height.get(), pZ - depth.get());
        } else if (direction == 3) {
            pos1.set(pX, pY, pZ - width.get() / 2);
            pos2.set(pX + depth.get(), pY + height.get(), pZ + width.get() / 2 + addition);
        }

        // get largest horizontal
        maxh = 1 + Math.max(Math.max(Math.max(0, width.get()), depth.get()), width.get());
        maxv = 1 + Math.max(height.get(), 0);

        box = new Box(pos1.toCenterPos(), pos2.toCenterPos());

        // Find blocks to break
        BlockIterator.register(Math.max((int) Math.ceil(2+1), maxh), Math.max((int) Math.ceil(2), maxv), (blockPos, blockState) -> {

            boolean toofar = !box.contains(Vec3d.ofCenter(blockPos));

            // Check for air, unbreakable blocks and distance
            if (!BlockUtils.canBreak(blockPos, blockState) || toofar) return;

            // Add block
            blocks.add(blockPosPool.get().set(blockPos));
        });

        // Break block if found
        BlockIterator.after(() -> {
            // Sort blocks
            blocks.sort(Comparator.comparingDouble(value -> Utils.squaredDistance(pX, pY, pZ, value.getX() + 0.5, value.getY() + 0.5, value.getZ() + 0.5) * 1 ));

            // Break
            int count = 0;

            for (BlockPos blockpos : blocks) {
                if (count >= maxBlocksPerTick.get()) break;

                boolean canInstaMine = BlockUtils.canInstaBreak(blockpos);

                if (!BlockHelper.isSolid(blockpos)) canMove = true;

                if (rotate.get()) {
                    Rotations.rotate(Rotations.getYaw(blockpos), Rotations.getPitch(blockpos), () -> {
                        if (BlockUtils.breakBlock(blockpos, swingHand.get())) {
                            blocksBroken++;
                            canMove = width.get() < 2 && height.get() < 3 && canInstaMine;
                        }
                    });
                } else {
                    if (BlockUtils.breakBlock(blockpos, swingHand.get())) {
                        blocksBroken++;
                        canMove = width.get() < 2 && height.get() < 3 && canInstaMine;
                    }
                }


                if (render.get()) RenderUtils.renderTickingBlock(blockpos.toImmutable(), sideColor.get(), lineColor.get(), shapeMode.get(), 0, 8, true, false);

                count++;
                if (!canInstaMine) break;
            }

            // Clear current block positions
            for (BlockPos.Mutable blockPos : blocks) blockPosPool.free(blockPos);
            blocks.clear();

            canMove = true;
        });

        // Move
        move(canMove);
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {

        // Constantly update distance
        distance = (int) PlayerUtils.distanceTo(start);

        // Auto pot
        if (autoPot.get()) usePotion();

        // Auto Mend
        if (autoMend.get()) mend();

        // Auto eat
        if (autoEat.get()) eat();

        // Scaffold
        scaffold();

        /// Fill Liquid
        fillLiquid();

        // Take items from shulker
        if (useShulker.get()) shulker();

        // Break blocks that can cause scaffold to goof up
        breakFlowBreaker();
    }

    // Re-level
    private void reLevel(Vec3d initPos) {
        if ((int) mc.player.getY() < (int) initPos.getY()) {
            if (!mc.world.getBlockState(mc.player.getBlockPos().up(2)).isSolid()) {
                mc.player.jump();
                if (BlockUtils.place(mc.player.getBlockPos().down(), InvUtils.find(itemStack -> block.get().contains(Block.getBlockFromItem(itemStack.getItem()))), 90, rotate.get()))
                    RenderUtils.renderTickingBlock(mc.player.getBlockPos().down(), sideColor.get(), lineColor.get(), shapeMode.get(), 0, 8, true, false);
            } else if (BlockUtils.breakBlock(mc.player.getBlockPos().up(2), swingHand.get()))
                RenderUtils.renderTickingBlock(mc.player.getBlockPos().up(2), sideColor.get(), lineColor.get(), shapeMode.get(), 0, 8, true, false);
        } else if ((int) mc.player.getY() > (int) initPos.getY()) {
            if (BlockUtils.breakBlock(mc.player.getBlockPos().up(2), swingHand.get()))
                RenderUtils.renderTickingBlock(mc.player.getBlockPos().down(), sideColor.get(), lineColor.get(), shapeMode.get(), 0, 8, true, false);
        }
    }

    // Scaffold
    private boolean scaffold() {
        if (BlockUtils.place(mc.player.getBlockPos().down(), InvUtils.find(itemStack -> block.get().contains(Block.getBlockFromItem(itemStack.getItem()))), 50, rotate.get())
            && render.get()
            && renderAutomation.get()) {
            RenderUtils.renderTickingBlock(mc.player.getBlockPos().down(), sideColor.get(), lineColor.get(), shapeMode.get(), 0, 8, true, false);
            return true;
        }

        return false;
    }

    // Fill Liquid
    private boolean fillLiquid() {
        for (BlockPos pos : BlockHelper.getSphere(mc.player.getBlockPos(), heightL.get(), radius.get())) {
            if (BlockHelper.getBlock(pos) == Blocks.LAVA || BlockHelper.getBlock(pos) == Blocks.WATER) {
                if (BlockUtils.place(pos, InvUtils.find(itemStack -> block.get().contains(Block.getBlockFromItem(itemStack.getItem()))), 90, rotate.get())) {
                    if (renderAutomation.get() && render.get())
                        RenderUtils.renderTickingBlock(pos,
                            sideColor.get(), lineColor.get(),
                            shapeMode.get(),
                            0,
                            8, true, false
                        );
                    return true;
                }
            }
        }
        return false;
    }

    // Move
    private void move(boolean b) {
        mc.options.forwardKey.setPressed(b);
        mc.options.sprintKey.setPressed(b);
    }

    // Item from Shulker
    private void shulker() {
        BlockPos pos = null;

        ItemStack container = getContainer();
        if (container != null) Utils.getItemsInContainerItem(container, containerItems);

        // Search for these
        FindItemResult sItem = InvUtils.find(itemStack -> itemStack.getItem() == stealItems.get());
        FindItemResult dItem = InvUtils.find(itemStack -> itemStack.getItem() == dumpItem.get());
        FindItemResult shulker = InvUtils.find(itemStack -> itemStack.getItem() == container.getItem());

        for (int row = 0; row < 3; row++) {
            for (int i = 0; i < 9; i++) {
                int index = row * 9 + i;
                ItemStack stack = containerItems[index];

                if (stack.getItem() == stealItems.get() && stack != null && sItem.count() <= activateAt.get()) {
                    if (BlockUtils.place(mc.player.getBlockPos().east(1), shulker, 90, rotate.get()))
                        RenderUtils.renderTickingBlock(mc.player.getBlockPos().west(), sideColor.get(), lineColor.get(), shapeMode.get(), 0, 8, true, false);
                }
            }
        }
    }

    private int getSleepTime() {
        return 2 + (1 > 0 ? ThreadLocalRandom.current().nextInt(0, 1) : 0);
    }

    private void moveSlots(ScreenHandler handler, int start, int end, boolean steal) {
        boolean initial = true;
        for (int i = start; i < end; i++) {
            if (!handler.getSlot(i).hasStack()) continue;

            int sleep;
            if (initial) {
                sleep = 1;
                initial = false;
            } else sleep = getSleepTime();
            if (sleep > 0) {
                try {
                    Thread.sleep(sleep);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            // Exit if user closes screen or exit world
            if (mc.currentScreen == null || !Utils.canUpdate()) break;

            Item item = handler.getSlot(i).getStack().getItem();
            if (steal &&  !stealItems.get().contains(item)) continue;
            else if (!steal && !dumpItem.get().contains(item)) continue;

            InvUtils.shiftClick().slotId(i);
        }
    }

    public void steal(ScreenHandler handler) {
        MeteorExecutor.execute(() -> moveSlots(handler, 0, SlotUtils.indexToId(SlotUtils.MAIN_START), true));
    }

    public void dump(ScreenHandler handler) {
        int playerInvOffset = SlotUtils.indexToId(SlotUtils.MAIN_START);
        MeteorExecutor.execute(() -> moveSlots(handler, playerInvOffset, playerInvOffset + 4 * 9, false));
    }

    // Get Container
    private ItemStack getContainer() {
        ItemStack stack = mc.player.getOffHandStack();
        if (Utils.hasItems(stack) || stack.getItem() == Items.ENDER_CHEST) return stack;

        for (int i = 0; i < SlotUtils.indexToId(SlotUtils.MAIN_START); i++) {
            stack = mc.player.getInventory().getStack(i);
            if (Utils.hasItems(stack) || stack.getItem() == Items.ENDER_CHEST) return stack;
        }

        return null;
    }

    // Auto mend
    private void mend() {
        FindItemResult exp = InvUtils.find(Items.EXPERIENCE_BOTTLE);

        if (!exp.found()) {
            info("Experience bottle not found...disabling");
            toggle();
        }

        if (Modules.get().get(Offhand.class).isActive()) {
            info("Offhand may interfere with auto mend...disabling offhand");
            Modules.get().get(Offhand.class).toggle();
        }

        if (needsRepair(mc.player.getMainHandStack(), minThreshold.get())) mend = true;
        else if (getDamage(mc.player.getInventory().getMainHandStack()) >= maxThreshold.get()) mend = false;

        if (mend) {
            if (mendWhileMine.get()) {
                if (isXP(45)) mc.interactionManager.interactItem(mc.player, Hand.OFF_HAND);
                else if (!isXP(45)) InvUtils.move().from(exp.slot()).toOffhand();
                canMove = true;
            } else {
                if (!isXP(bottleSlot.get() - 1)) InvUtils.move().from(exp.slot()).to(bottleSlot.get() - 1);
                canMove = false;
                Rotations.rotate(mc.player.getYaw(), 90, () -> {
                    InvUtils.swap(exp.slot(), true);
                    mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                    InvUtils.swapBack();
                });
            }
        }

        //if (getDamage(mc.player.getInventory().getMainHandStack()) >= maxThreshold.get()) mend = false;
    }

    private boolean isXP(int slot) {
        return mc.player.getInventory().getStack(slot).isOf(Items.EXPERIENCE_BOTTLE);
    }

    public static double getDamage(ItemStack i) {
        return (((double) (i.getMaxDamage() - i.getDamage()) / i.getMaxDamage()) * 100);
    }

    private boolean needsRepair(ItemStack itemStack, double threshold) {
        if (itemStack.isEmpty() || Utils.hasEnchantment(itemStack, Enchantments.MENDING)) return false;
        return (itemStack.getMaxDamage() - itemStack.getDamage()) / (double) itemStack.getMaxDamage() * 100 <= threshold;
    }

    // Auto Pot
    private void usePotion() {
        //boolean alerted = false;
        //int sI = speedPotSlot(true);
        int pI = speedPotSlot(false);

        if (pI == -1) {
            info("No potions found");
            toggle();
            return;
        }

        if (mc.player != null && !mc.player.getStatusEffects().equals("effect.minecraft.speed")) {
            canMove = false;
            if (pI != potSlot.get() - 1) InvUtils.move().from(pI).to(potSlot.get() - 1);
            changeSlot(potSlot.get() - 1);
            if (isPotion(mc.player.getMainHandStack())) mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
        }
    }

    private void changeSlot(int slot) {
        mc.player.getInventory().selectedSlot = slot;
    }

    private int speedPotSlot(boolean splash) {
        int slot = -1;
        for (int i = 0; i < 9; i++) {
            // Skip if item stack is empty
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isEmpty()) continue;
            if (splash) {
                if (stack.getItem() != Items.SPLASH_POTION) continue;
                if (stack.getItem() == Items.POTION) {
                    Iterator<StatusEffectInstance> effects = stack.getItem().getComponents().get(DataComponentTypes.POTION_CONTENTS).getEffects().iterator();
                    if (effects.hasNext()) {
                        StatusEffectInstance effect = effects.next();
                        if (effect.getTranslationKey().equals("effect.minecraft.speed")) {
                            slot = i;
                            break;
                        }
                    }
                }
            } else {
                if (stack.getItem() != Items.POTION) continue;
                if (stack.getItem() == Items.POTION) {
                    Iterator<StatusEffectInstance> effects = stack.getItem().getComponents().get(DataComponentTypes.POTION_CONTENTS).getEffects().iterator();
                    if (effects.hasNext()) {
                        StatusEffectInstance effect = effects.next();
                        if (effect.getTranslationKey().equals("effect.minecraft.speed")) {
                            slot = i;
                            break;
                        }
                    }
                }
            }
        }
        return slot;
    }

    private boolean isPotion(ItemStack stack) {
        Item item = stack.getItem();
        return item == Items.POTION;
    }

    private boolean isSplashPotion(ItemStack stack) {
        Item item = stack.getItem();
        return item == Items.SPLASH_POTION;
    }

    // Auto Eat
    private void eat() {
        float hunger = mc.player.getHungerManager().getFoodLevel();
        int prevSlot = mc.player.getInventory().selectedSlot;

        if (hunger <= hungerThreshold.get()) {
            int slot = findSlot();
            startEating(slot);
            canMove = false;
        } else if (mc.player.isUsingItem() && hunger >= hungerThreshold.get()) {
            canMove = true;
            stopEating(prevSlot);
        }
    }

    private void startEating(int slot) {
        changeSlot(slot);
        mc.options.useKey.setPressed(true);
        if (!mc.player.isUsingItem()) Utils.rightClick();
    }

    private void stopEating(int slot) {
        InvUtils.swap(slot, false);
        mc.options.useKey.setPressed(false);
    }

    private int findSlot() {
        int slot = -1;
        int bestHunger = -1;

        for (int i = 0; i < 9; i++) {
            // Skip if item isn't food
            Item item = mc.player.getInventory().getStack(i).getItem();
            FoodComponent foodComponent = item.getComponents().get(DataComponentTypes.FOOD);
            if (foodComponent == null) continue;

            // Check if hunger value is better
            int hunger = foodComponent.nutrition();
            if (hunger > bestHunger) {
                // Skip if item is in blacklist
                if (blacklist.get().contains(item)) continue;

                // Select the current item
                slot = i;
                bestHunger = hunger;
            }
        }

        Item offHandItem = mc.player.getOffHandStack().getItem();
        if (offHandItem.getComponents().get(DataComponentTypes.FOOD) != null && !blacklist.get().contains(offHandItem) && offHandItem.getComponents().get(DataComponentTypes.FOOD).nutrition() > bestHunger)
            slot = SlotUtils.OFFHAND;

        return slot;
    }

    // Break Flow Breaker
    private void breakFlowBreaker() {
        if (getFlowBreakBlock(pos1.down())) BlockUtils.breakBlock(pos1.down(), swingHand.get());

        //canMove = !getFlowBreakBlock(pos1.down());
    }

    private boolean getFlowBreakBlock(BlockPos pos) {
        return BlockHelper.getBlock(pos) == Blocks.VINE || BlockHelper.getBlock(pos) == Blocks.CAVE_VINES || BlockHelper.getBlock(pos) == Blocks.TWISTING_VINES || BlockHelper.getBlock(pos) == Blocks.WEEPING_VINES || BlockHelper.getBlock(pos) == Blocks.SOUL_SAND || BlockHelper.getBlock(pos) instanceof SlabBlock;
    }

    @Override
    public void onDeactivate() {
        move(false);

        if (autoMend.get() && !Modules.get().get(Offhand.class).isActive()) Modules.get().get(Offhand.class).toggle();

        if (displayInfo.get()) {
            info("Distance: (highlight)%.0f", PlayerUtils.distanceTo(start));
            info("Blocks broken: (highlight)%d", blocksBroken);
        }
    }
}
