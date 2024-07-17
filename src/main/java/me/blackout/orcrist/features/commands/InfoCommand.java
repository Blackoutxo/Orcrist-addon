package me.blackout.orcrist.features.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.blackout.orcrist.utils.misc.Maths;
import me.blackout.orcrist.utils.player.Stats;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.command.CommandSource;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class InfoCommand extends Command {
    public InfoCommand() {
        super("info", "Send current information.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(context -> {
            info("-------Stats-------");
            info("     PlayTime: " + Maths.timeElapsed(Stats.playtime()));
            info("    KillStreak: " + Stats.killstreak);
            info("    HighScore: " + Stats.highscore);
            info("     Deaths: " + Stats.death);
            info("      Kills: " + Stats.kills);
            info("       KD: " + Stats.getKD());
            return SINGLE_SUCCESS;
        });
    }
}
