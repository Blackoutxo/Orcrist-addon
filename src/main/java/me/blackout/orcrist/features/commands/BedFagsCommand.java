package me.blackout.orcrist.features.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.blackout.orcrist.features.module.world.NotifierPlus;
import me.blackout.orcrist.utils.misc.Maths;
import me.blackout.orcrist.utils.player.Stats;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.player.PlayerEntity;

public class BedFagsCommand extends Command {
    public BedFagsCommand() {
        super("bedfags", "Send current bed faggers.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(context -> {
            NotifierPlus N = Modules.get().get(NotifierPlus.class);

            if (!N.bedFags.isEmpty())
                for (PlayerEntity entity : N.bedFags) info(entity.getName());

            return SINGLE_SUCCESS;
        });
    }
}
