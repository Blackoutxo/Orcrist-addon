package me.blackout.orcrist.utils.misc;

import me.blackout.orcrist.features.module.world.AutoTunnel;
import me.blackout.orcrist.utils.player.Stats;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import net.minecraft.SharedConstants;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class Placeholders {
    public static String apply(String m) {
        if (m.contains("{highscore}")) m = m.replace("{highscore}", String.valueOf(Stats.highscore));
        if (m.contains("{distance}")) m = m.replace("{distance}", String.valueOf(AutoTunnel.distance));
        if (m.contains("{killstreak}")) m = m.replace("{killstreak}", String.valueOf(Stats.killstreak));
        if (m.contains("{kills}")) m = m.replace("{kills}", String.valueOf(Stats.kills));
        if (m.contains("{deaths}")) m = m.replace("{deaths}", String.valueOf(Stats.death));
        if (m.contains("{server}")) m = m.replace("{server}", Utils.getWorldName());
        if (m.contains("{version}")) m = m.replace("{version}", SharedConstants.getGameVersion().getName());
        if (m.contains("{username}")) m = m.replace("{username}", mc.getSession().getUsername());
        if (m.contains("{hp}")) m = m.replace("{hp}", String.valueOf(Math.rint(PlayerUtils.getTotalHealth())));
        return m;
    }
}
