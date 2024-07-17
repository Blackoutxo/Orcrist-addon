package me.blackout.orcrist.features.module.combat;

import me.blackout.orcrist.OrcristAddon;
import me.blackout.orcrist.utils.player.CombatHelper;
import me.blackout.orcrist.utils.render.RenderUtil;
import me.blackout.orcrist.utils.world.BlockHelper;
import me.blackout.orcrist.utils.world.BlockUtil;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.mixin.WorldRendererAccessor;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.combat.Burrow;
import meteordevelopment.meteorclient.systems.modules.combat.Surround;
import meteordevelopment.meteorclient.utils.entity.DamageUtils;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.meteorclient.utils.world.CardinalDirection;
import meteordevelopment.meteorclient.utils.world.Dir;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.AirBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.BlockBreakingInfo;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.s2c.play.DeathMessageS2CPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.updater.WorldUpdater;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class SurroundPlus extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgPlacement = settings.createGroup("Placement");
    private final SettingGroup sgAutomation = settings.createGroup("Automation");
    private final SettingGroup sgProtect = settings.createGroup("Protect");
    private final SettingGroup sgRender = settings.createGroup("Render");

    // ------------------ General ------------------ //

    private final Setting<List<Block>> blocks = sgGeneral.add(new BlockListSetting.Builder().name("blocks").description("What blocks to use for surround.").defaultValue(Blocks.OBSIDIAN, Blocks.CRYING_OBSIDIAN, Blocks.NETHERITE_BLOCK).filter(this::blockFilter).build());
    private final Setting<Center> center = sgGeneral.add(new EnumSetting.Builder<SurroundPlus.Center>().name("center").description("Teleports you to the center of the block.").defaultValue(Center.OnActivate).build());
    private final Setting<Boolean> onlyOnGround = sgGeneral.add(new BoolSetting.Builder().name("only-on-ground").description("Works only when you are standing on blocks.").defaultValue(true).build());
    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder().name("rotate").description("Automatically faces towards the obsidian being placed.").defaultValue(true).build());
    private final Setting<Boolean> toggleModules = sgGeneral.add(new BoolSetting.Builder().name("toggle-modules").description("Turn off other modules when surround is activated.").defaultValue(false).build());
    private final Setting<Boolean> toggleBack = sgGeneral.add(new BoolSetting.Builder().name("toggle-back-on").description("Turn the other modules back on when surround is deactivated.").defaultValue(false).visible(toggleModules::get).build());
    private final Setting<List<Module>> modules = sgGeneral.add(new ModuleListSetting.Builder().name("modules").description("Which modules to disable on activation.").visible(toggleModules::get).build());

    // ------------------ Placement ------------------ //

    private final Setting<Mode> mode = sgPlacement.add(new EnumSetting.Builder<Mode>().name("mode").description("Which mode to use for surrounding.").defaultValue(Mode.Normal).build());
    private final Setting<Placement> placement = sgPlacement.add(new EnumSetting.Builder<Placement>().name("placement").description("How blocks are placed.").defaultValue(Placement.Instant).build());
    private final Setting<Integer> delay = sgPlacement.add(new IntSetting.Builder().name("blocks-per-tick").description("Delay, in ticks, between block placements.").min(0).defaultValue(0).visible(() -> placement.get() == Placement.Ticked).build());
    private final Setting<Boolean> packetPlace = sgPlacement.add(new BoolSetting.Builder().name("packet-place").description("Will place blocsk using packets.").defaultValue(false).build());
    private final Setting<Boolean> airPlace = sgPlacement.add(new BoolSetting.Builder().name("air-place").description("Places in air.").defaultValue(true).build());
    private final Setting<Boolean> doubleHeight = sgPlacement.add(new BoolSetting.Builder().name("double-height").description("Places obsidian on top of the original surround blocks to prevent people from face-placing you.").defaultValue(false).build());

    // ------------------ Automation ------------------ //

    private final Setting<Boolean> toggleOnYChange = sgAutomation.add(new BoolSetting.Builder().name("toggle-on-y-change").description("Automatically disables when your y level changes (step, jumping, etc).").defaultValue(true).build());
    private final Setting<Boolean> toggleOnComplete = sgAutomation.add(new BoolSetting.Builder().name("toggle-on-complete").description("Toggles off when all blocks are placed.").defaultValue(false).build());
    private final Setting<Boolean> toggleOnDeath = sgAutomation.add(new BoolSetting.Builder().name("toggle-on-death").description("Toggles off when you die.").defaultValue(true).build());
    private final Setting<Boolean> chorus = sgAutomation.add(new BoolSetting.Builder().name("chorus").description("Uses chorus to teleport out of your hole").defaultValue(true).build());
    private final Setting<Boolean> burrow = sgAutomation.add(new BoolSetting.Builder().name("burrow").description("Burrows you when module is toggled on.").defaultValue(false).build());
    private final Setting<Boolean> breakBed = sgAutomation.add(new BoolSetting.Builder().name("break-bed").description("Breaks bed if they are on the surround position.").defaultValue(true).build());

    // ------------------ Protect ------------------ //

    private final Setting<Boolean> protect = sgProtect.add(new BoolSetting.Builder().name("protect").description("Attempts to break crystals around surround positions to prevent surround break.").defaultValue(true).build());
    private final Setting<Boolean> expandOnBreak = sgProtect.add(new BoolSetting.Builder().name("expand").description("Expands your surround when someone tried to break it.").defaultValue(true).build());
    private final Setting<Keybind> widen = sgProtect.add(new KeybindSetting.Builder().name("force-expand").defaultValue(Keybind.none()).build());
    private final Setting<Boolean> breakButton = sgProtect.add(new BoolSetting.Builder().name("break-button").description("Will break the button after anti button placing.").defaultValue(true).build());

    // ------------------ Render ------------------ //

    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder().name("render").defaultValue(true).build());
    private final Setting<Boolean> fade = sgRender.add(new BoolSetting.Builder().name("fade-out").defaultValue(true).build());
    private final Setting<Boolean> swing = sgRender.add(new BoolSetting.Builder().name("swing").defaultValue(true).build());
    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>().name("shape-mode").description("How the shapes are rendered.").defaultValue(ShapeMode.Both).build());
    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder().name("side-color").description("The side color of the target block rendering.").defaultValue(new SettingColor(197, 137, 232, 10)).build());
    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder().name("line-color").description("The line color of the target block rendering.").defaultValue(new SettingColor(197, 137, 232)).build());

    // Fields


    public SurroundPlus() {
        super(OrcristAddon.Combat, "surround+", "Surrounds you in blocks to prevent massive crystal damage.");
    }



    private FindItemResult getInvBlock() {
        return InvUtils.findInHotbar(itemStack -> blocks.get().contains(Block.getBlockFromItem(itemStack.getItem())));
    }

    private boolean blockFilter(Block block) {
        return block == Blocks.OBSIDIAN ||
            block == Blocks.CRYING_OBSIDIAN ||
            block == Blocks.NETHERITE_BLOCK ||
            block == Blocks.ENDER_CHEST ||
            block == Blocks.RESPAWN_ANCHOR;
    }

    public enum Center {
        Never,
        OnActivate,
        Incomplete,
        Always
    }

    public enum BlockType {
        Safe,
        Normal,
        Unsafe
    }

    public enum Mode {
        Normal,
        Russian,
        RussianPlus;

        @Override
        public String toString() {
            return switch (this) {
                case Normal -> "Normal";
                case Russian -> "Russian";
                case RussianPlus -> "Russian+";
            };
        }
    }

    public enum Placement {
        Instant,
        Ticked
    }
}
