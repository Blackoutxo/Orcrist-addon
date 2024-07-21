package me.blackout.orcrist.features.module.render;

import me.blackout.orcrist.OrcristAddon;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BeaconBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BeaconBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LightningEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.util.math.BlockPos;

public class KillEffects extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    // General

    private final Setting<Integer> range = sgGeneral.add(new IntSetting.Builder().name("range").description("How far away the lightning is allowed to spawn from you.").defaultValue(16).sliderRange(0,256).build());
    private final Setting<Boolean> avoidSelf = sgGeneral.add(new BoolSetting.Builder().name("avoid-self").description("Will not render your own deaths.").defaultValue(true).build());

    public KillEffects() {
        super(OrcristAddon.Render, "kill-effects", "Spawns a lightning where a player dies.");
    }


    @EventHandler
    private void onReceivePacket(PacketEvent.Receive event) {
        if (!(event.packet instanceof EntityStatusS2CPacket packet)) return;
        if (packet.getStatus() != 3) return;

        Entity player = packet.getEntity(mc.world);

        if (player == mc.player && avoidSelf.get()) return;
        if (mc.player.distanceTo(player) > range.get()) return;

        double playerX = player.getX();
        double playerY = player.getY();
        double playerZ = player.getZ();

        spawnLightning(playerX, playerY, playerZ);
    }

    private void spawnLightning(double x, double y, double z) {
        LightningEntity lightning = new LightningEntity(EntityType.LIGHTNING_BOLT, mc.world);

        lightning.updatePosition(x, y, z);
        lightning.refreshPositionAfterTeleport(x, y, z);
        mc.world.addEntity(lightning);
    }
}
