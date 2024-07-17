package me.blackout.orcrist.features.hud;

import me.blackout.orcrist.OrcristAddon;
import me.blackout.orcrist.utils.player.Stats;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.Settings;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.systems.hud.elements.TextHud;

public class KillsHud extends HudElement {
    private SettingGroup sgGeneral = settings.getDefaultGroup();

    // General
    public final Setting<Boolean> countSlashKill = sgGeneral.add(new BoolSetting.Builder().name("/kill").description("Whether to count /kill or not.").defaultValue(true).build());

    public static final HudElementInfo<KillsHud> INFO = new HudElementInfo<>(OrcristAddon.HUD_GROUP, "Kills", "Shows your total amount of deaths.", KillsHud::new);

    public KillsHud() {
        super(INFO);
    }

    @Override
    public void render(HudRenderer renderer) {
        setSize(renderer.textWidth("Kills :", true), renderer.textHeight(true));

        renderer.text("Kills : ", x, y, TextHud.getSectionColor(0), true);
        renderer.text(String.valueOf(Stats.kills), x + renderer.textWidth("Kills : "), y, TextHud.getSectionColor(1), true);
    }
}
