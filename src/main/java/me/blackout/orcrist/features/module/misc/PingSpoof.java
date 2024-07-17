package me.blackout.orcrist.features.module.misc;

import me.blackout.orcrist.OrcristAddon;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.common.KeepAliveC2SPacket;

public class PingSpoof extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> value = sgGeneral.add(new IntSetting.Builder().name("value").description("Amount of ping to be spoofed.").defaultValue(69).sliderMax(20000).sliderMin(-10).build());

    public PingSpoof() {
        super(OrcristAddon.Misc, "ping-spoof", "Will Spoof your ping to the server.");
    }

    SystemTimer timer = new SystemTimer();
    KeepAliveC2SPacket packet = null;

    @EventHandler
    public void onPacketSend(PacketEvent.Send event) {
        if(event.packet instanceof KeepAliveC2SPacket && packet != event.packet && value.get() != 0) {
            packet = (KeepAliveC2SPacket) event.packet;
            event.cancel();
            timer.reset();
        }
    }

    @Override
    public String getInfoString() {
        return value.get() + "ms";
    }

    @EventHandler
    public void onUpdate(Render3DEvent event) {
        if(timer.hasPassed(value.get()) && packet != null) {
            mc.getNetworkHandler().sendPacket(packet);
            packet = null;
        }
    }

    class SystemTimer {
        private long time;

        public SystemTimer() {
            time = System.currentTimeMillis();
        }

        public boolean hasPassed(double ms) {
            return System.currentTimeMillis() - time >= ms;
        }

        public void reset() {
            time = System.currentTimeMillis();
        }

        public long getTime() {
            return time;
        }

        public void setTime(long time) {
            this.time = time;
        }
    }

}
