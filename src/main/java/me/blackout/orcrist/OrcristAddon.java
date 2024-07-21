package me.blackout.orcrist;

import me.blackout.orcrist.features.commands.BedFagsCommand;
import me.blackout.orcrist.features.commands.CenterCommand;
import me.blackout.orcrist.features.commands.InfoCommand;
import me.blackout.orcrist.features.hud.*;
import me.blackout.orcrist.features.module.combat.*;
import com.mojang.logging.LogUtils;
import me.blackout.orcrist.features.module.misc.AutoRespawnPlus;
import me.blackout.orcrist.features.module.misc.BetterChatPlus;
import me.blackout.orcrist.features.module.misc.DiscordRPC;
import me.blackout.orcrist.features.module.misc.PingSpoof;
import me.blackout.orcrist.features.module.movement.elytrabot.ElytraBot;
import me.blackout.orcrist.features.module.movement.speed.SpeedPlus;
import me.blackout.orcrist.features.module.render.KillEffects;
import me.blackout.orcrist.features.module.world.AutoTunnel;
import me.blackout.orcrist.features.module.world.BetterHighwayBuilder;
import me.blackout.orcrist.features.module.world.NotifierPlus;
import me.blackout.orcrist.utils.player.Stats;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.commands.Commands;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudGroup;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.item.Items;
import org.slf4j.Logger;

public class OrcristAddon extends MeteorAddon {
    public static final Logger LOG = LogUtils.getLogger();
    public static final String Version = "0.1.0";
    public static final Category Combat = new Category("Combat+", Items.TOTEM_OF_UNDYING.getDefaultStack());
    public static final Category Render = new Category("Render+", Items.ENDER_PEARL.getDefaultStack());
    public static final Category Misc = new Category("Misc+", Items.GRASS_BLOCK.getDefaultStack());
    public static final Category World = new Category("World+", Items.DEEPSLATE_BRICKS.getDefaultStack());
    public static final Category Movement = new Category("Movement+", Items.NETHERITE_BOOTS.getDefaultStack());
    public static final HudGroup HUD_GROUP = new HudGroup("Orcrist Hud");

    @Override
    public void onInitialize() {
        LOG.info("Initializing Orcrist Addon");

        // Modules
        initCombat();
        initMisc();
        initMovement();
        initRender();
        initWorld();

        // Commands
        Commands.add(new BedFagsCommand());
        Commands.add(new CenterCommand());
        Commands.add(new InfoCommand());

        // HUD
        initHUD();

        // Set Playtime
        Stats.time = System.currentTimeMillis();
    }

    // Modules

    private void initCombat() {
        Modules.get().add(new AutoCityPlus());
        Modules.get().add(new AutoCrystal());
        Modules.get().add(new AutoLogPlus());
        Modules.get().add(new AutoTrapPlus());
        Modules.get().add(new AutoXp());
        Modules.get().add(new BedBomb());
        Modules.get().add(new CevBreaker());
        Modules.get().add(new Elevate());
        Modules.get().add(new KillAuraPlus());
        //Modules.get().add(new PistonAura());
        Modules.get().add(new PistonCrystal());
        Modules.get().add(new SelfTrapPlus());
        Modules.get().add(new SurroundPlus());
    }

    private void initMisc() {
        Modules.get().add(new AutoRespawnPlus());
        Modules.get().add(new BetterChatPlus());
        Modules.get().add(new DiscordRPC());
        Modules.get().add(new PingSpoof());
    }

    private void initMovement() {
        Modules.get().add(new ElytraBot());
        //Modules.get().add(new SpeedPlus());
    }

    private void initRender() {
        Modules.get().add(new KillEffects());
    }

    private void initWorld() {
        Modules.get().add(new AutoTunnel());
        Modules.get().add(new BetterHighwayBuilder());
        Modules.get().add(new NotifierPlus());
    }

    // HUD
    private void initHUD() {
        Hud.get().register(AverageSpeedHud.INFO);
        Hud.get().register(CrystalPerSecondHud.INFO);
        Hud.get().register(DeathHud.INFO);
        Hud.get().register(HighscoreHud.INFO);
        Hud.get().register(KDRHud.INFO);
        Hud.get().register(KillsHud.INFO);
        Hud.get().register(KillStreakHud.INFO);
        Hud.get().register(LogoHud.INFO);
        //Hud.get().register(NotificationHud.INFO);
        Hud.get().register(PlayTimeHud.INFO);
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(Combat);
        Modules.registerCategory(Render);
        Modules.registerCategory(Misc);
        Modules.registerCategory(World);
        Modules.registerCategory(Movement);
    }

    @Override
    public String getPackage() {
        return "me.blackout.orcrist";
    }
}
