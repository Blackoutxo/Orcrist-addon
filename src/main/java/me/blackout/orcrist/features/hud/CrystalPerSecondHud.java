package me.blackout.orcrist.features.hud;

import me.blackout.orcrist.OrcristAddon;
import me.blackout.orcrist.utils.player.Stats;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.systems.hud.elements.TextHud;

public class CrystalPerSecondHud extends HudElement {
    public static final HudElementInfo<CrystalPerSecondHud> INFO = new HudElementInfo<>(OrcristAddon.HUD_GROUP, "Crystal Per Second", "Shows your crystal per second rate.", CrystalPerSecondHud::new);
    public CrystalPerSecondHud() {
        super(INFO);
    }

    @Override
    public void render(HudRenderer renderer) {
        setSize(renderer.textWidth("CP/s :", true), renderer.textHeight(true));

        renderer.text("CP/s : ", x, y, TextHud.getSectionColor(0), true);
        renderer.text(String.valueOf(Stats.crystalsPerSec), x + renderer.textWidth("CP/s : "), y, TextHud.getSectionColor(1), true);
    }
}
