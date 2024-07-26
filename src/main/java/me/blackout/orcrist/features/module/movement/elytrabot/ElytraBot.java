package me.blackout.orcrist.features.module.movement.elytrabot;

import baritone.api.BaritoneAPI;
import me.blackout.orcrist.OrcristAddon;
import me.blackout.orcrist.features.module.movement.elytrabot.botutil.AStar;
import me.blackout.orcrist.features.module.movement.elytrabot.botutil.DirectionUtils;
import me.blackout.orcrist.features.module.movement.elytrabot.botutil.MainBotUtils;
import me.blackout.orcrist.utils.misc.BaritoneUtils;
import me.blackout.orcrist.utils.player.CombatHelper;
import me.blackout.orcrist.utils.world.TimerUtils;
import meteordevelopment.meteorclient.events.entity.player.PlayerMoveEvent;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.NoFall;
import meteordevelopment.meteorclient.systems.modules.player.AutoEat;
import meteordevelopment.meteorclient.systems.modules.player.ChestSwap;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.Pool;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.s2c.common.DisconnectS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.function.BooleanSupplier;

import static me.blackout.orcrist.features.module.movement.elytrabot.botutil.MainBotUtils.distance;
import static me.blackout.orcrist.features.module.movement.elytrabot.botutil.MainBotUtils.useItem;
import static meteordevelopment.meteorclient.MeteorClient.EVENT_BUS;

/*
* ported to 1.19 by @Wide_Cat
*/



public class ElytraBot extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgElytraFly = settings.createGroup("Flight");
    private final SettingGroup sgCords = settings.createGroup("Coordinates");
    private final SettingGroup sgAutomation = settings.createGroup("Automation");
    private final SettingGroup sgRender = settings.createGroup("Render");

    // General
    public final Setting<PathMode> botPathMode = sgGeneral.add(new EnumSetting.Builder<PathMode>().name("path-mode").description("Which Mode to use for the bot path finding.").defaultValue(PathMode.Highway).build());
    public final Setting<TakeOff> botTakeOff = sgGeneral.add(new EnumSetting.Builder<TakeOff>().name("take-off").description("Which to use for Takeoff").defaultValue(TakeOff.Normal).build());
    public final Setting<Boolean> baritone = sgGeneral.add(new BoolSetting.Builder().name("baritone").defaultValue(true).build());
    public final Setting<Integer> walkDist = sgGeneral.add(new IntSetting.Builder().name("walk-distance").defaultValue(20).sliderMin(1).sliderMax(30).build());
    public final Setting<Boolean> avoidLava = sgGeneral.add(new BoolSetting.Builder().name("avoid-lava").defaultValue(true).build());
    public final Setting<Integer> maxY = sgGeneral.add(new IntSetting.Builder().name("max-y").defaultValue(50).sliderMin(1).sliderMax(369).build());
    public final Setting<Boolean> autoSwitch = sgGeneral.add(new BoolSetting.Builder().name("auto-switch").defaultValue(true).build());
    public final Setting<Integer> switchDurability = sgGeneral.add(new IntSetting.Builder().name("switch-durability").defaultValue(3).sliderMin(1).sliderMax(100).visible(autoSwitch :: get).build());
    public final Setting<Boolean> disconnectOnGoal = sgGeneral.add(new BoolSetting.Builder().name("disconnect-on-goal").description("Disconnects from world when goal is reached.").defaultValue(true).build());
    public final Setting<Boolean> togglePop = sgGeneral.add(new BoolSetting.Builder().name("toggle-on-pop").description("Toggles ElytraBot when you pop a totem.").defaultValue(true).build());
    public final Setting<Boolean> disconnectOnPop = sgGeneral.add(new BoolSetting.Builder().name("disconnect-on-pop").description("Disconnecs when you pop a totem.").defaultValue(true).build());
    public final Setting<Boolean> debug = sgGeneral.add(new BoolSetting.Builder().name("debug").description("Sends debug messages.").defaultValue(false).build());

    // Flight
    public final Setting<FlightMode> flyMode = sgElytraFly.add(new EnumSetting.Builder<FlightMode>().name("fly-mode").description("Which modes to use when flying.").defaultValue(FlightMode.Control).onChanged(this::onModeChange).build());
    public final Setting<Double> fireworkDelay = sgElytraFly.add(new DoubleSetting.Builder().name("firework-delay").description("Delay for firework.").defaultValue(1).sliderMin(1).sliderMax(10).visible(() -> flyMode.get() == FlightMode.Firework).build());
    public final Setting<Double> flySpeed = sgElytraFly.add(new DoubleSetting.Builder().name("fly-speed").description("Flying speed for control mode.").defaultValue(5).sliderMin(1).sliderMax(10).visible(() -> flyMode.get() == FlightMode.Control).build());
    public final Setting<Double> maneuverSpeed = sgElytraFly.add(new DoubleSetting.Builder().name("maneuver-speed").defaultValue(1).sliderMin(1).sliderMax(20).visible(() -> flyMode.get() == FlightMode.Control).build());

    // Coordinates
    public final Setting<Boolean> useCoordinates = sgCords.add(new BoolSetting.Builder().name("use-coordinates").description("Whether to use coordinates or not.").defaultValue(false).build());
    public final Setting<Integer> gotoX = sgCords.add(new IntSetting.Builder().name("goto-X").description("Goes to the specific X cords").defaultValue(500).sliderMin(-30000000).sliderMax(30000000).visible(useCoordinates :: get).build());
    public final Setting<Integer> gotoZ = sgCords.add(new IntSetting.Builder().name("goto-Z").description("Goes to the specific Z cords").defaultValue(100).sliderMin(-30000000).sliderMax(30000000).visible(useCoordinates :: get).build());

    // Automations
    public final Setting<Boolean> autoEat = sgAutomation.add(new BoolSetting.Builder().name("auto-eat").description("Automatically eats food for you when flying").defaultValue(true).build());
    public final Setting<Boolean> allowGaps = sgAutomation.add(new BoolSetting.Builder().name("allow-gaps").description("Will use gaps when the hunger threshold is reached.").defaultValue(true).build());
    public final Setting<Integer> minHealth = sgAutomation.add(new IntSetting.Builder().name("min-health").description("minimum health for auto eat to eat").defaultValue(10).sliderMin(0).sliderMax(36).visible(autoEat :: get).build());
    public final Setting<Integer> minHunger = sgAutomation.add(new IntSetting.Builder().name("min-hunger").description("minimum health for auto eat to eat").defaultValue(10).sliderMin(0).sliderMax(19).visible(autoEat :: get).build());

    // Render
    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder().name("render").description("Will render blocks.").defaultValue(true).build());
    private final Setting<Boolean> renderGoal = sgRender.add(new BoolSetting.Builder().name("render-goal").description("Will render the goal.").defaultValue(true).build());
    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>().name("shape-mode").description("Which shape to render").defaultValue(ShapeMode.Both).build());
    private final Setting<SettingColor> pathColor = sgRender.add(new ColorSetting.Builder().name("path-color").description("Color for the line mode.").defaultValue(new SettingColor(197, 137, 232, 255)).build());
    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder().name("side-color").description("Color for side mode.").defaultValue(new SettingColor(197, 137, 232,75)).build());
    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder().name("line-color").description("Color for the line mode.").defaultValue(new SettingColor(197, 137, 232, 255)).build());

    //Variables
    private ArrayList<BlockPos> path;
    private Thread thread;
    private BlockPos goal, previous, lastSecondPos;
    private BlockPos last = null;
    private DirectionUtils direction;

    private final Pool<RenderBlock> renderBlockPool = new Pool<>(RenderBlock::new);
    private final List<RenderBlock> renderBlocks = new ArrayList<>();
    private boolean watchTotems;

    private int x, z;
    private double jumpY = -1;
    private int startingTotems;
    private int packetsSent, lagbackCounter, useBaritoneCounter;
    private boolean lagback, toggledNoFall, isRunning;
    private double blocksPerSecond;
    private int blocksPerSecondCounter;

    private TimerUtils blocksPerSecondTimer = new TimerUtils();
    private TimerUtils packetTimer = new TimerUtils();
    private TimerUtils fireworkTimer = new TimerUtils();
    private TimerUtils takeoffTimer = new TimerUtils();

    public String Status = "Disabled";
    public String Goal = null;
    public String Time = null;
    public String Fireworks = null;

    public ElytraBot() {
        super(OrcristAddon.Movement, "elytra-bot", "Automatically navigates and travels using elytra using baritone.");
    }

    @Override
    public void onActivate() {
        int up = 1;
        Status = "Enabled";

        for (RenderBlock renderBlock : renderBlocks) renderBlockPool.free(renderBlock);
        renderBlocks.clear();

        // this should work to properly end the thread, rather than doing thread.suspend()
        isRunning = true;

        // equip an elytra before starting the thread (doesn't seem to work when first starting in the thread)
        if (mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() != Items.ELYTRA) {
            FindItemResult elytra = InvUtils.find(Items.ELYTRA);
            if (elytra.found()) InvUtils.move().from(elytra.slot()).toArmor(2);
        }

        // no fall can mess up takeoff and the entire module, so disable it if its active, and re-enable it after
        NoFall noFall = Modules.get().get(NoFall.class);
        if (noFall.isActive()) {
            noFall.toggle();
            toggledNoFall = true;
            warning("NoFall is on, disabling while ElytraBot is active.");
        }

        // alternative to using packet event for checking if we popped a totem
        FindItemResult totem = InvUtils.find(Items.TOTEM_OF_UNDYING);
        if (!totem.found()) {
            watchTotems = false;
        } else {
            startingTotems = totem.count();
            watchTotems = true;
        }

        if (!useCoordinates.get()) {
            if (Math.abs(Math.abs(mc.player.getX()) - Math.abs(mc.player.getZ())) <= 5 && Math.abs(mc.player.getX()) > 10 && Math.abs(mc.player.getZ()) > 10 && (botPathMode.get() == PathMode.Highway)) {
                direction = DirectionUtils.getDiagonalDirection();
            } else direction = DirectionUtils.getDirection();

            goal = generateGoalFromDirection(direction, up);
            Goal = direction.name;
        }
        else {
            x = gotoX.get();
            z = gotoZ.get();
            goal = new BlockPos(x, (int) (mc.player.getY() + up), z);
            Goal = ("X: " + x + ", Z: " + z);
        }

        thread = new Thread() {
            public void run() {
                // to stop the thread loop just set isRunning to false
                while (thread != null && thread.equals(this) && isRunning) {
                    try {
                        loop();
                    } catch (NullPointerException e) {

                    }

                    try {
                        sleep(50);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };

        blocksPerSecondTimer.reset();
        thread.start();
    }

    @Override
    public void onDeactivate() {
        direction = null;
        path = null;
        lagback = false;

        useBaritoneCounter = 0;
        lagbackCounter = 0;
        blocksPerSecond = 0;
        blocksPerSecondCounter = 0;
        jumpY = -1;

        lastSecondPos = null;
        goal = null;
        last = null;

        PacketFly.toggle(false);
        EFly.toggle(false);

        Status = "Disabled";

        BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().forceCancel();
        MainBotUtils.suspend(thread);

        thread = null;
        Goal = null;
        Time = null;
        Fireworks = null;

        for (RenderBlock renderBlock : renderBlocks) renderBlockPool.free(renderBlock);
        renderBlocks.clear();
    }


    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (!isRunning) { // when the thread is stopped
            enableGroundListener();
            toggle();
        }

        renderBlocks.forEach(RenderBlock::tick);
        renderBlocks.removeIf(renderBlock -> renderBlock.ticks <= 0);

        // "new" totem check
        /*if (watchTotems && togglePop.get()) {
            FindItemResult totem = InvUtils.find(Items.TOTEM_OF_UNDYING); // check current totem count
            if (!totem.found() || totem.getCount() < startingTotems) {
                warning("You've popped a totem! Disabling...");
                isRunning = false; // toggle if the user has none left, or it's below what they started with
            }
        }*/
    }

    @EventHandler
    private void onReceivePacket(PacketEvent.Receive event) {
        if (!(event.packet instanceof EntityStatusS2CPacket p)) return;
        if (p.getStatus() != 35) return;

        Entity entity = p.getEntity(mc.world);

        if (entity != null || entity.equals(mc.player)) {
            if (togglePop.get()) {
                warning("You've popped a totem, disabling...");
                toggle();
            }

            if (disconnectOnPop.get()) {
                mc.player.networkHandler.onDisconnect(new DisconnectS2CPacket(Text.literal("[Elytra Bot] You've popped a totem.")));
            }
            toggle();
        }
    }


    public void loop() {
        if (!Utils.canUpdate()) return;

        // stop if we reached the goal
        if (PlayerUtils.distanceTo(goal) <= 5) {
            mc.world.playSound(mc.player.getX(), mc.player.getY(), mc.player.getZ(), SoundEvent.of(Identifier.of("minecraft:entity.player.levelup")), SoundCategory.PLAYERS, 100, 18, true);
            mc.player.stopFallFlying();
            if (!disconnectOnGoal.get()) info("Goal reached!");
            else mc.player.networkHandler.onDisconnect(new DisconnectS2CPacket(Text.literal("[Elytra Bot] Goal Reached!")));
            isRunning = false;
        }

        // elytra check
        if (mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() != Items.ELYTRA) {
            error("You need an elytra.");
            isRunning = false;
        }

        // no fall check again
        NoFall noFall = Modules.get().get(NoFall.class);
        if (noFall.isActive()) {
            error("You cannot use NoFall while ElytraBot is active!");
            if (!toggledNoFall) toggledNoFall = true;
            noFall.toggle();
        }

        // toggle if no fireworks and using firework mode
        if (flyMode.get() == FlightMode.Firework && InvUtils.find(Items.FIREWORK_ROCKET).count() == 0) {
            error("You need fireworks in your inventory if you are using firework mode.");
            isRunning = false;
        }

        // waiting if in an unloaded chunk
        if (!BaritoneUtils.isInRenderDistance(getPlayerPos())) {
            if (debug.get()) info("Waiting for chunks to load.");
            Status = "Waiting for chunk";
            mc.player.setVelocity(0, 0, 0);
            return;
        }

        // switch low durability elytra with fresh one if setting is on
        if (autoSwitch.get()) {
            ItemStack chestStack = mc.player.getInventory().getArmorStack(2);
            if (chestStack.getItem() == Items.ELYTRA) {
                if (chestStack.getMaxDamage() - chestStack.getDamage() <= switchDurability.get()) {
                    if (debug.get()) info("Trying to switch elytra");
                    FindItemResult elytra = InvUtils.find(stack -> stack.getMaxDamage() - stack.getDamage() > switchDurability.get() && stack.getItem() == Items.ELYTRA);

                    InvUtils.move().from(elytra.slot()).toArmor(2);
                    if (debug.get()) info("Swapped elytra");
                }
            }
        }

        // takeoff
        double preventPhase = (jumpY + 0.6);
        if (mc.player.isFallFlying() || mc.player.getY() < preventPhase || mc.player.isOnGround()) {
            if (PacketFly.toggled) {
                sleep(1500);

                if (mc.player.isFallFlying() || mc.player.getY() < preventPhase || mc.player.isOnGround()) {
                    sleep(100);
                }
            }
        }

        if (!mc.player.isFallFlying()) {
            EFly.toggle(false);

            BlockPos blockPosAbove = getPlayerPos().add(0, 2, 0);

            if (mc.player.isOnGround() && MainBotUtils.isSolid(blockPosAbove) && baritone.get() && botPathMode.get() == PathMode.Highway) {
                Status = "Using baritone";
                useBaritone();
            }

            if (MainBotUtils.isSolid(blockPosAbove) && botPathMode.get() == PathMode.Tunnel) {
                if (MainBotUtils.getBlock(blockPosAbove) != Blocks.BEDROCK) {
                    Status = "Mining obstruction";
                    PlayerUtils.centerPlayer();
                    Rotations.rotate(Rotations.getYaw(blockPosAbove), Rotations.getPitch(blockPosAbove), () -> MainBotUtils.mine(blockPosAbove));
                } else {
                    if (baritone.get()) {
                        Status = "Using baritone";
                        useBaritone();
                    } else {
                        info("The above block is bedrock and useBaritone is false.");
                        isRunning = false;
                    }
                }
            }

            if (jumpY != 1 && Math.abs(mc.player.getY() - jumpY) >= 2) {
                if (baritone.get() && direction != null && botPathMode.get() == PathMode.Highway) {
                    info("Using baritone to get back to the highway.");
                    Status = "Using baritone";
                    useBaritone();
                }
            }

            if (packetsSent < 20) {
                if (debug.get()) info("Trying to takeoff.");
                Status = "Taking off";
            }

            fireworkTimer.ms = 0;

            if (mc.player.isOnGround()) {
                jumpY = mc.player.getY();
                generatePath();
                mc.player.jump();
            } else if (mc.player.getY() < mc.player.prevY) {
                if (botTakeOff.get() == TakeOff.PacketFly) {
                    if (mc.player.getY() > preventPhase && !PacketFly.toggled) PacketFly.toggle(true);
                    if (debug.get()) info("Toggling on packet fly.");
                } else if (botTakeOff.get() == TakeOff.SlowFly) {
                    mc.player.setVelocity(0, -0.04, 0);
                    if (debug.get()) info("Slow gliding.");
                }

                // Don't send any more packets for about 15 seconds if the takeoff isn't successful.
                // Bcs 2b2t has this annoying thing where it will not let u open elytra if u don't stop sending the packets for a while
                if (packetsSent <= 15) {
                    if (takeoffTimer.hasPassed(650)) {
                        mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
                        takeoffTimer.reset();
                        packetTimer.reset();
                        packetsSent++;
                    }
                } else if (packetTimer.hasPassed(15000)) {
                    packetsSent = 0;
                    if (debug.get()) info("15 seconds over.");
                }
                else {
                    info("Waiting 15 seconds before sending elytra opening packets again");
                    Status = "Waiting to takeoff";
                }
            }
            return;
        }

        else if (!PacketFly.toggled) {
            packetsSent = 0;

            double speed = CombatHelper.getSpeed(mc.player);
            if (speed < 0.1) {
                useBaritoneCounter++;

                if (useBaritoneCounter >= 15) {
                    useBaritoneCounter = 0;

                    if (baritone.get()) {
                        info("Using baritone to walk a bit because we are stuck.");
                        Status = "Using baritone";
                        useBaritone();
                    }
                    else {
                        info("We are stuck. Enabling the 'useBaritone' setting would help.");
                        isRunning = false;
                    }
                }
            } else useBaritoneCounter = 0;

            if (flyMode.get() == FlightMode.Firework) {
                if (speed > 3) lagback = true;

                if (lagback) {
                    if (speed < 1) {
                        lagbackCounter++;
                        if (debug.get()) info("Potential lagback detected.");
                        if (lagbackCounter > 3) {
                            lagback = false;
                            lagbackCounter = 0;
                            if (debug.get()) info("Lagback reset.");
                        }
                    } else lagbackCounter = 0;
                }

                if (fireworkTimer.hasPassed((int) (fireworkDelay.get() * 1000)) && !lagback) {
                    clickOnFirework();
                }
            }
        }

        float health = mc.player.getHealth() + mc.player.getAbsorptionAmount();
        float hunger = mc.player.getHungerManager().getFoodLevel();
        int prevSlot = mc.player.getInventory().selectedSlot;

        if (autoEat.get() && !mc.player.isUsingItem() && !Modules.get().get(AutoEat.class).isActive()) {
            if (flyMode.get() != FlightMode.Firework || (flyMode.get() == FlightMode.Firework && !fireworkTimer.hasPassed(100))) {
                if (health <= minHealth.get() || hunger <= minHunger.get()) {

                    if (debug.get()) info("Need to eat.");

                    for (int i = 0; i < 9; i++) {

                        Item item = mc.player.getInventory().getStack(i).getItem();

                        if (debug.get()) info("Finding food item.");

                        if (item.getComponents().get(DataComponentTypes.FOOD) != null) {

                            if (MainBotUtils.shouldEatItem(item)) {
                                MainBotUtils.eat(i);

                                if (debug.get()) info("Trying to eat item.");
                            }
                        }
                    }
                }
            }
        } else if (mc.player.isUsingItem() && health >= minHealth.get() && hunger >= minHunger.get()) {
            stopEating(prevSlot);
            if (debug.get()) info("Stopped eating.");
        }

        if (path == null || path.size() <= 20 || isNextPathTooFar()) {
            generatePath();
            if (debug.get()) info("Generating more path.");
        }

        int distance = 12;
        if (botPathMode.get() == PathMode.Highway || flyMode.get() == FlightMode.Control) distance = 2;

        boolean remove = false;
        ArrayList<BlockPos> removePositions = new ArrayList<BlockPos>();

        for (BlockPos pos : path) {
            if (!remove && distance(pos, getPlayerPos()) <= distance) remove = true;
            if (remove) removePositions.add(pos);
        }

        for (BlockPos pos : removePositions) {
            path.remove(pos);
            previous = pos;
        }

        if (path.size() > 0) {
            if (direction != null) {
                if (debug.get()) info("Going to " + direction.name);
            } else {
                if (debug.get()) info("Going to X: " + x + " Z: " + z);

                if (blocksPerSecondTimer.hasPassed(1000)) {
                    blocksPerSecondTimer.reset();
                    if (lastSecondPos != null) {
                        blocksPerSecondCounter++;
                        blocksPerSecond += PlayerUtils.distanceTo(lastSecondPos);
                    }

                    lastSecondPos = getPlayerPos();
                }

                int seconds = (int)(PlayerUtils.distanceTo(goal) / (blocksPerSecond / blocksPerSecondCounter));
                int h = seconds / 3600;
                int m = (seconds % 3600) / 60;
                int s = seconds % 60;
                TimerUtils infoTimer = new TimerUtils();

                if (debug.get() && infoTimer.hasPassed(1000)) {
                    info("Estimated time arrival in " + h + " hour "+ m + " minute " + s + " second");
                    infoTimer.reset();
                }
                Time = (h + "h, " + m + "m, " + s + "s");

                if (flyMode.get() == FlightMode.Firework) {
                    if (debug.get()) info("Estimated fireworks needed: " + (int) (seconds / fireworkDelay.get()));
                    Fireworks = String.valueOf(Math.round(seconds / fireworkDelay.get()));
                }
            }

            if (flyMode.get() == FlightMode.Firework) {
                Vec3d vec = new Vec3d(path.get(path.size() - 1).add((int) 0.5, (int) 0.5, (int) 0.5).getX(), path.get(path.size() - 1).add((int) 0.5, (int) 0.5, (int) 0.5).getY(), path.get(path.size() - 1).add((int) 0.5, (int) 0.5, (int) 0.5).getZ());
                mc.player.setYaw((float) Rotations.getYaw(vec));
                mc.player.setPitch((float) Rotations.getPitch(vec));
                Status = "Flying";
            } else if (flyMode.get() == FlightMode.Control) {
                EFly.toggle(true);

                BlockPos next = null;
                if (path.size() > 1) {
                    next = path.get(path.size() - 2);
                }
                EFly.setMotion(path.get(path.size() - 1), next, previous);
                Status = "Flying";
            }
        }
    }

    public void generatePath() {
        BlockPos[] positions = {
            new BlockPos(1, 0, 0), new BlockPos(-1, 0, 0), new BlockPos(0, 0, 1), new BlockPos(0, 0, -1),
            new BlockPos(1, 0, 1), new BlockPos(-1, 0, -1), new BlockPos(-1, 0, 1), new BlockPos(1, 0, -1),
            new BlockPos(0, -1, 0), new BlockPos(0, 1, 0)};

        ArrayList<BlockPos> checkPositions = new ArrayList<BlockPos>();

        if (botPathMode.get() == PathMode.Highway) {
            BlockPos[] list = {new BlockPos(1, 0, 0), new BlockPos(-1, 0, 0), new BlockPos(0, 0, 1), new BlockPos(0, 0, -1),
                new BlockPos(1, 0, 1), new BlockPos(-1, 0, -1), new BlockPos(-1, 0, 1), new BlockPos(1, 0, -1)};
            checkPositions = new ArrayList<BlockPos>(Arrays.asList(list));
        } else if (botPathMode.get() == PathMode.Overworld) {
            int radius = 3;
            for (int x = (-radius); x < radius; x++) {
                for (int z = (-radius); z < radius; z++) {
                    for (int y = (radius); y > -radius; y--) {
                        checkPositions.add(new BlockPos(x, y, z));
                    }
                }
            }
        } else if (botPathMode.get() == PathMode.Tunnel) {
            positions = new BlockPos[]{new BlockPos(1, 0, 0), new BlockPos(-1, 0, 0), new BlockPos(0, 0, 1), new BlockPos(0, 0, -1)};
            checkPositions = new ArrayList<BlockPos>(List.of(new BlockPos(0, -1, 0)));
        }

        if (path == null || path.size() == 0 || isNextPathTooFar() || mc.player.isOnGround()) {
            BlockPos start;
            if (botPathMode.get() == PathMode.Overworld) {
                start = getPlayerPos().add(0, 4, 0);
            } else if (Math.abs(jumpY - mc.player.getY()) <= 2) {
                start = new BlockPos((int) mc.player.getX(), (int) (jumpY + 1), (int) mc.player.getZ());
            } else {
                start = getPlayerPos().add(0, 1, 0);
            }

            if (isNextPathTooFar()) {
                start = getPlayerPos();
            }

            path = AStar.generatePath(start, goal, positions, checkPositions, 500);
        } else {
            ArrayList<BlockPos> temp = AStar.generatePath(path.get(0), goal, positions, checkPositions, 500);
            try {
                temp.addAll(path);
            } catch (NullPointerException ignored) {

            }

            path = temp;
        }
    }

    private class StaticGroundListener {
        @EventHandler
        private void chestSwapGroundListener(PlayerMoveEvent event) {
            if (mc.player != null && mc.player.isOnGround()) {
                if (mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() == Items.ELYTRA) {
                    Modules.get().get(ChestSwap.class).swap();
                    disableGroundListener();
                }
            }
        }
    }

    private final StaticGroundListener staticGroundListener = new StaticGroundListener();

    protected void enableGroundListener() {
        EVENT_BUS.subscribe(staticGroundListener);
    }

    protected void disableGroundListener() {
        EVENT_BUS.unsubscribe(staticGroundListener);
    }

    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        isRunning = false;
    }

    public void useBaritone() {
        EFly.toggle(false);

        int y = (int)(jumpY - mc.player.getY());
        int x = 0;
        int z = 0;

        int blocks = walkDist.get();
        switch (direction) {
            case ZM: z = -blocks;
            case XM: x = -blocks;
            case XP: x = blocks;
            case ZP: z = blocks;
            case XP_ZP: x = blocks; z = blocks;
            case XM_ZM: x = -blocks; z = -blocks;
            case XP_ZM: x = blocks; z = -blocks;
            case XM_ZP: x = -blocks; z = blocks;
        }

        walkTo(getPlayerPos().add(x, y, z), true);
        sleep(5000);
        sleepUntil(() -> !BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().isPathing(), 120000);
        BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().forceCancel();
    }

    private void clickOnFirework() {
        if (mc.player.getMainHandStack().getItem() != Items.FIREWORK_ROCKET) {
            FindItemResult result = InvUtils.findInHotbar(Items.FIREWORK_ROCKET);
            if (result.slot() != -1) {
                InvUtils.swap(result.slot(), false);
            }
        }

        //Click
        useItem();
        fireworkTimer.reset();
    }

    public BlockPos generateGoalFromDirection(DirectionUtils direction, int up) {
        // since we call mc.player.getX/Y/Z multiple times we should just have them as variables
        // and use those
        double x = mc.player.getX();
        double y = mc.player.getY();
        double z = mc.player.getZ();
        if (direction == DirectionUtils.ZM) {
            return new BlockPos(0, (int) (y + up), (int) (z - 30000000));
        } else if (direction == DirectionUtils.ZP) {
            return new BlockPos(0, (int) (y + up), (int) (z + 30000000));
        } else if (direction == DirectionUtils.XM) {
            return new BlockPos((int) (x - 30000000), (int) (y + up), 0);
        } else if (direction == DirectionUtils.XP) {
            return new BlockPos((int) (x + 30000000), (int) (y + up), 0);
        } else if (direction == DirectionUtils.XP_ZP) {
            return new BlockPos((int) (x + 30000000), (int) (y + up), (int) (z + 30000000));
        } else if (direction == DirectionUtils.XM_ZM) {
            return new BlockPos((int) (x - 30000000), (int) (y + up), (int) (z - 30000000));
        } else if (direction == DirectionUtils.XP_ZM) {
            return new BlockPos((int) (x + 30000000), (int) (y + up), (int) (z - 30000000));
        } else {
            return new BlockPos((int) (x - 30000000), (int) (y + up), (int) (z + 30000000));
        }
    }

    private BlockPos getPlayerPos() {
        return new BlockPos((int) mc.player.getX(), (int) mc.player.getY(), (int) mc.player.getZ());
    }

    private void walkTo(BlockPos goal, boolean sleepUntilDone) {
        BaritoneAPI.getProvider().getPrimaryBaritone().getCommandManager().execute("goto " + goal.getX() + " " + goal.getY() + " " + goal.getZ());

        if (sleepUntilDone) {
            sleepUntil(() -> BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().isPathing(), 100);
            sleepUntil(() -> !BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior().isPathing(), -1);
        }
    }

    private boolean isNextPathTooFar() {
        try {
            return distance(getPlayerPos(), path.get(path.size() - 1)) > 15;
        } catch (Exception e) {
            return false;
        }
    }

    private void stopEating(int slot) {
        InvUtils.swap(slot, false);
        mc.options.useKey.setPressed(false);
    }

    public static void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (Exception ignored) {}
    }

    public static void sleepUntil(BooleanSupplier condition, int timeout) {
        long startTime = System.currentTimeMillis();
        while(true) {
            if (condition.getAsBoolean()) {
                break;
            } else if (timeout != -1 && System.currentTimeMillis() - startTime >= timeout) {
                break;
            }

            sleep(10);
        }
    }

    private void onModeChange(FlightMode flightMode) {
        Fireworks = null;
        Time = null;
    }

    @Override
    public String getInfoString() {
        return Status;
    }

    @EventHandler
    private void render3DEvent(Render3DEvent event) {
        renderBlocks.sort(Comparator.comparingInt(o -> -o.ticks));
        renderBlocks.add(renderBlockPool.get().set(goal));
        renderBlocks.forEach(renderBlock -> renderBlock.render(event, sideColor.get(), lineColor.get(), shapeMode.get()));
    }

    public class RenderBlock {
        public BlockPos.Mutable pos = new BlockPos.Mutable();
        public int timer;
        public int ticks;

        public RenderBlock set(BlockPos blockPos) {
            pos.set(blockPos);
            timer = 0;
            ticks = 8;

            return this;
        }

        public void tick() {
            timer++;
            ticks--;
        }

        public void render(Render3DEvent event, Color sides, Color lines, ShapeMode shapeMode) {
            int preSideA = sides.a;
            int preLineA = lines.a;

            sides.a *= (double) ticks / 8;
            lines.a *= (double) ticks / 8;

            //Path
            if (render.get()) {
                if (path != null) {
                    try {
                        for (BlockPos pos : path) {
                            if (last != null) {
                                event.renderer.line(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, last.getX() + 0.5, last.getY() + 0.5, last.getZ() + 0.5, pathColor.get());
                            }

                            last = pos;
                        }
                    } catch (Exception exception) {
                        last = null;
                    }
                }

                if (renderGoal.get()) {
                    event.renderer.box(pos.getX() + 1, pos.getY() + 4, pos.getZ() + 1, pos.getX(), pos.getY(), pos.getZ(), sides, lines, shapeMode, 0);
                }
            }

            sides.a = preSideA;
            lines.a = preLineA;
        }
    }

    public enum PathMode {
        Highway, Overworld, Tunnel
    }

    public enum TakeOff {
        SlowFly, PacketFly, Normal
    }

    public enum FlightMode {
        Control, Firework
    }
}
