package me.blackout.orcrist.features.module.misc;

import me.blackout.orcrist.OrcristAddon;
import me.blackout.orcrist.utils.player.CombatHelper;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.AutoRespawn;
import meteordevelopment.meteorclient.systems.modules.render.WaypointsModule;
import meteordevelopment.meteorclient.utils.world.TickRate;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.DeathScreen;

import java.util.List;
import java.util.Random;

public class AutoRespawnPlus extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgExcuse = settings.createGroup("Auto Cope");

    // General
    private final Setting<Boolean> rekit = sgGeneral.add(new BoolSetting.Builder().name("re-kit").description("Automatically send command to rekit.").defaultValue(false).build());
    private final Setting<String> name = sgGeneral.add(new StringSetting.Builder().name("name").description("Name of the kit.").defaultValue("/kit Blackout180").build());

    // Auto Cope
    private final Setting<Boolean> autoExcuse = sgExcuse.add(new BoolSetting.Builder().name("auto-cope").description("Automatically says an excuse when you die.").defaultValue(false).build());
    private final Setting<Boolean> randomize = sgExcuse.add(new BoolSetting.Builder().name("randomize").description("Randomizes the excuse message.").defaultValue(false).build());
    private final Setting<List<String>> excuses = sgExcuse.add(new StringListSetting.Builder().name("excuses").description("Messages to send as an excuse.").defaultValue("High Ping", "Game lagged").build());

    private boolean shouldRekit, canExcuse;
    private int excuseWait = 50;
    private int rekitWait = 50;
    private int messageI = 0;

    public AutoRespawnPlus() {
        super(OrcristAddon.Misc, "auto-respawn+", "Automatically presses respawn button for you.");
    }

    @EventHandler
    private void onOpenScreen(OpenScreenEvent event) {
        if (!(event.screen instanceof DeathScreen)) return;

        Modules.get().get(WaypointsModule.class).addDeath(mc.player.getPos());
        mc.player.requestRespawn();

        if (rekit.get()) shouldRekit = true;
        if (autoExcuse.get()) canExcuse = true;

        event.cancel();
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (TickRate.INSTANCE.getTimeSinceLastTick() >= 0.8) return;

        if (shouldRekit && rekitWait <= 1) {

            info("Rekitting with kit " + name.get());
            mc.player.networkHandler.sendChatCommand("kit " + name.get());

            shouldRekit = false;
            rekitWait = 50;
            return;

        } else  rekitWait--;

        if (canExcuse && excuseWait <= 1) {
            String excuseMessage = getExcuseMessage();
            mc.player.networkHandler.sendChatCommand(excuseMessage);
            canExcuse = false;
            excuseWait = 50;
        } else  excuseWait--;
    }

    private String getExcuseMessage() {
        String excuseMessage;

        if (excuses.get().isEmpty()) {

            error("Your excuse message list is empty!");
            return "Lag";

        } else {
            if (randomize.get()) excuseMessage = excuses.get().get(new Random().nextInt(excuses.get().size()));

            else {
                if (messageI >= excuses.get().size()) messageI = 0;
                int i = messageI++;
                excuseMessage = excuses.get().get(i);
            }
        }

        return excuseMessage;
    }
}
