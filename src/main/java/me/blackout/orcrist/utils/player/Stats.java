package me.blackout.orcrist.utils.player;

import me.blackout.orcrist.features.hud.KillsHud;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.utils.PreInit;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.CommandExecutionC2SPacket;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class Stats {
    public static int kills, killstreak, death, highscore;
    public static double avgSpeed;
    public static long time;

    public static ArrayList<String> messages = new ArrayList<>();
    public static ArrayList<Double> speed = new ArrayList<>();

    private static String previousServer, currentServer;

    private static boolean sentChatPacket;

    private static int ticksPassed;
    public static int crystalsPerSec;
    public static int first;

    @PreInit
    public static void init() {
        MeteorClient.EVENT_BUS.subscribe(Stats.class);
    }

    // Average Speed
    @EventHandler
    private static void onTick(TickEvent.Pre event) {
        if (mc.player == null) return;
        speed.add(Utils.getPlayerSpeed().horizontalLength());

        // Clear list and calculate avg speed
        if (speed.size() > 500) speed.remove(0);
        if (speed.size() == 500) avgSpeed = getAverageSpeed(speed);
    }

    // Typical Status
    @EventHandler
    private static void onScreen(OpenScreenEvent event) {
        if (!(event.screen instanceof DeathScreen)) return;

        killstreak = 0;
        highscore = killstreak;
        if (!sentChatPacket) death++; else sentChatPacket = false;
    }

    @EventHandler
    private static void onGameJoin(GameJoinedEvent event) {
        currentServer = Utils.getWorldName();

        if (!currentServer.equals(previousServer)) {
            highscore = 0;
            killstreak = 0;
            death = 0;
            kills = 0;
        }
    }

    @EventHandler
    private static void onGameLeft(GameLeftEvent event) {
        previousServer = currentServer;
    }

    @EventHandler
    private static void onPacketSent(PacketEvent.Sent event) {
        if (!(event.packet instanceof CommandExecutionC2SPacket cmd)) return;
        if (new KillsHud().countSlashKill.get()) return;

        sentChatPacket = cmd.command().contains("kill");
    }

    // Message Receive
    @EventHandler(priority = EventPriority.HIGHEST)
    private static void onMessageRecieve(ReceiveMessageEvent event) {
        String msg = event.getMessage().getString();
        int byIndex = msg.indexOf("by");
        if (byIndex != -1) {
            int nameIndex = msg.indexOf(mc.player.getName().getString());
            boolean canDo = false;

            if (nameIndex < byIndex && nameIndex != -1) return;
            if (nameIndex > byIndex) canDo = true;
            if (canDo) {
                killstreak++;
                kills++;
            }
        }
    }

    // Crystals per Second
    @EventHandler
    private static void onPreTick(TickEvent.Pre event) {
        if (!Utils.canUpdate()) return;

        if (ticksPassed < 21) ticksPassed++; else ticksPassed = 0;
        if (ticksPassed == 1) first = InvUtils.find(Items.END_CRYSTAL).count();

        if (ticksPassed == 21) {
            int second = InvUtils.find(Items.END_CRYSTAL).count();
            int difference = -(second - first);
            crystalsPerSec = Math.max(0, difference);
        }
    }

    // Others

    public static String getAverageSpeed() {
        DecimalFormat df = new DecimalFormat("#.##");
        df.setRoundingMode(RoundingMode.CEILING);
        return df.format(getAverageSpeed(speed));
    }

    private static double getAverageSpeed(List<Double> velocities) {
        if (velocities.isEmpty()) return 0;

        double sum = 0;
        for (Double velocity : velocities) sum += velocity;

        return sum / velocities.size();
    }

    public static long playtime() {
        return time;
    }

    public static String getKD() {
        if (death == 0) return String.valueOf(Stats.kills); //make sure we don't try to divide by 0
        Double rawKD = (double) (Stats.kills / death);
        DecimalFormat df = new DecimalFormat("#.##");
        df.setRoundingMode(RoundingMode.CEILING);
        return df.format(rawKD);
    }
}
