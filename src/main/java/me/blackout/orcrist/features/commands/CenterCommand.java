package me.blackout.orcrist.features.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import net.minecraft.command.CommandSource;

import static com.mojang.brigadier.Command.SINGLE_SUCCESS;

public class CenterCommand extends Command {
    public CenterCommand() {
        super("center", "Centers player.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(context -> {
            PlayerUtils.centerPlayer();
            return SINGLE_SUCCESS;
        });
    }
}
