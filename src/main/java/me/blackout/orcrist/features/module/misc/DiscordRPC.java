package me.blackout.orcrist.features.module.misc;

import me.blackout.orcrist.OrcristAddon;
import me.blackout.orcrist.utils.misc.Placeholders;
import meteordevelopment.discordipc.DiscordIPC;
import meteordevelopment.discordipc.RichPresence;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.WidgetScreen;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.MeteorStarscript;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.starscript.Script;
import meteordevelopment.starscript.compiler.Compiler;
import meteordevelopment.starscript.compiler.Parser;
import meteordevelopment.starscript.utils.StarscriptError;
import net.minecraft.client.gui.screen.*;
import net.minecraft.client.gui.screen.multiplayer.AddServerScreen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.screen.multiplayer.DirectConnectScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.*;
import net.minecraft.client.gui.screen.pack.PackScreen;
import net.minecraft.client.gui.screen.world.*;
import net.minecraft.client.realms.gui.screen.RealmsScreen;

import java.util.ArrayList;
import java.util.List;

public class DiscordRPC extends Module {

    public enum SelectMode {
        Random,
        Sequential
    }

    private final SettingGroup sgLine1 = settings.createGroup("Line 1");
    private final SettingGroup sgLine2 = settings.createGroup("Line 2");

    // Line 1
    private final Setting<List<String>> line1Strings = sgLine1.add(new StringListSetting.Builder().name("line-1-messages").description("Messages used for the first line.").defaultValue("{player}", "Currently playing on {server}").build());
    private final Setting<Integer> line1UpdateDelay = sgLine1.add(new IntSetting.Builder().name("line-1-update-delay").description("How fast to update the first line in ticks.").defaultValue(200).min(10).sliderRange(10, 200).build());
    private final Setting<SelectMode> line1SelectMode = sgLine1.add(new EnumSetting.Builder<SelectMode>().name("line-1-select-mode").description("How to select messages for the first line.").defaultValue(SelectMode.Sequential).build());

    // Line 2
    private final Setting<List<String>> line2Strings = sgLine2.add(new StringListSetting.Builder().name("line-2-messages").description("Messages used for the second line.").defaultValue("Orcrist on top!", "{round({server.tps}, 1)} TPS", "Playing on {server.difficulty} difficulty.", "{server.player_count} Players online", "KillStreak: {killstreak}", "Highscore: {highscore}", "Deaths: {death}", "Kills: {kills}").build());
    private final Setting<Integer> line2UpdateDelay = sgLine2.add(new IntSetting.Builder().name("line-2-update-delay").description("How fast to update the second line in ticks.").defaultValue(60).min(10).sliderRange(10, 200).build());
    private final Setting<SelectMode> line2SelectMode = sgLine2.add(new EnumSetting.Builder<SelectMode>().name("line-2-select-mode").description("How to select messages for the second line.").defaultValue(SelectMode.Sequential).build());

    private static final RichPresence rpc = new RichPresence();
    private boolean forceUpdate, lastWasInMainMenu;

    private final List<Script> line1Scripts = new ArrayList<>();
    private int line1Ticks, line1I;

    private final List<Script> line2Scripts = new ArrayList<>();
    private int ticks;
    private int line2Ticks, line2I;

    public DiscordRPC() {
        super(OrcristAddon.Misc,"discord-RPC", "Displays Orcrist as your presence on Discord.");

        runInMainMenu = true;
    }

    @Override
    public void onActivate() {
        DiscordIPC.start(908001433570402336L, null);

        rpc.setStart(System.currentTimeMillis() / 1000L);

        String largeText = "Orcrist 0.1.0";
        rpc.setLargeImage("orcrist", largeText);

        recompileLine1();
        recompileLine2();

        ticks = 0;
        line1Ticks = 0;
        line2Ticks = 0;
        lastWasInMainMenu = false;

        line1I = 0;
        line2I = 0;
    }

    @Override
    public void onDeactivate() {
        DiscordIPC.stop();
    }

    private void recompile(List<String> messages, List<Script> scripts) {
        scripts.clear();

        for (int i = 0; i < messages.size(); i++) {
            Parser.Result result = Parser.parse(messages.get(i));

            if (result.hasErrors()) {
                if (Utils.canUpdate()) {
                    MeteorStarscript.printChatError(i, result.errors.get(0));
                }

                continue;
            }

            scripts.add(Compiler.compile(result));
        }

        forceUpdate = true;
    }

    private void recompileLine1() {
        recompile(line1Strings.get(), line1Scripts);

    }

    private void recompileLine2() {
        recompile(line2Strings.get(), line2Scripts);
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        boolean update = false;

        // Image
        if (Utils.canUpdate()) {
            // Line 1
            if (line1Ticks >= line1UpdateDelay.get() || forceUpdate) {
                if (!line1Scripts.isEmpty()) {
                    int i = Utils.random(0, line1Scripts.size());
                    if (line1SelectMode.get() == SelectMode.Sequential) {
                        if (line1I >= line1Scripts.size()) line1I = 0;
                        i = line1I++;
                    }

                    try {
                        rpc.setDetails(String.valueOf(MeteorStarscript.ss.run(line1Scripts.get(i))));
                        //rpc.setState(Placeholders.apply(line1Scripts.get(i).toString()));
                    } catch (StarscriptError e) {
                        ChatUtils.error("Starscript", e.getMessage());
                    }
                }
                update = true;

                line1Ticks = 0;
            } else line1Ticks++;

            // Line 2
            if (line2Ticks >= line2UpdateDelay.get() || forceUpdate) {
                if (!line2Scripts.isEmpty()) {
                    int i = Utils.random(0, line2Scripts.size());
                    if (line2SelectMode.get() == SelectMode.Sequential) {
                        if (line2I >= line2Scripts.size()) line2I = 0;
                        i = line2I++;
                    }

                    try {
                        rpc.setState(String.valueOf(MeteorStarscript.ss.run(line2Scripts.get(i))));
                        //rpc.setState(Placeholders.apply(line2Scripts.get(i).toString()));
                    } catch (StarscriptError e) {
                        ChatUtils.error("Starscript", e.getMessage());
                    }
                }
                update = true;

                line2Ticks = 0;
            } else line2Ticks++;
        }
        else {
            if (!lastWasInMainMenu) {
                rpc.setDetails("Orcrist 0.1.0");

                if (mc.currentScreen instanceof TitleScreen) rpc.setState("Looking at title screen");
                else if (mc.currentScreen instanceof SelectWorldScreen) rpc.setState("Selecting world");
                else if (mc.currentScreen instanceof CreateWorldScreen || mc.currentScreen instanceof EditGameRulesScreen) rpc.setState("Creating world");
                else if (mc.currentScreen instanceof EditWorldScreen) rpc.setState("Editing world");
                else if (mc.currentScreen instanceof LevelLoadingScreen) rpc.setState("Loading world");
                else if (mc.currentScreen instanceof MultiplayerScreen) rpc.setState("Selecting server");
                else if (mc.currentScreen instanceof AddServerScreen) rpc.setState("Adding server");
                else if (mc.currentScreen instanceof ConnectScreen || mc.currentScreen instanceof DirectConnectScreen) rpc.setState("Connecting to server");
                else if (mc.currentScreen instanceof OptionsScreen || mc.currentScreen instanceof SkinOptionsScreen || mc.currentScreen instanceof SoundOptionsScreen || mc.currentScreen instanceof VideoOptionsScreen || mc.currentScreen instanceof ControlsOptionsScreen || mc.currentScreen instanceof LanguageOptionsScreen || mc.currentScreen instanceof ChatOptionsScreen || mc.currentScreen instanceof PackScreen || mc.currentScreen instanceof AccessibilityOptionsScreen) rpc.setState("Changing options");
                else if (mc.currentScreen instanceof CreditsScreen) rpc.setState("Reading credits");
                else if (mc.currentScreen instanceof RealmsScreen) rpc.setState("Browsing Realms");
                else {
                    String className = mc.currentScreen.getClass().getName();

                    if (className.startsWith("com.terraformersmc.modmenu.gui")) rpc.setState("Browsing mods");
                    else if (className.startsWith("me.jellysquid.mods.sodium.client")) rpc.setState("Changing options");
                    else rpc.setState("In main menu");
                }

                update = true;
            }
        }

        // Update
        if (update) DiscordIPC.setActivity(rpc);
        forceUpdate = false;
        lastWasInMainMenu = !Utils.canUpdate();
    }

    @EventHandler
    private void onOpenScreen(OpenScreenEvent event) {
        if (!Utils.canUpdate()) lastWasInMainMenu = false;
    }
}
