package me.blackout.orcrist.features.hud;

import me.blackout.orcrist.OrcristAddon;
import me.blackout.orcrist.utils.player.Stats;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.systems.hud.elements.TextHud;

public class AverageSpeedHud extends HudElement {
    public static final HudElementInfo<AverageSpeedHud> INFO = new HudElementInfo<>(OrcristAddon.HUD_GROUP, "Average Speed", "Shows your average speed.", AverageSpeedHud::new);
    public AverageSpeedHud() {
        super(INFO);
    }

    @Override
    public void render(HudRenderer renderer) {
        setSize(renderer.textWidth("Speed :", true), renderer.textHeight(true));

        renderer.text("Speed : ", x, y, TextHud.getSectionColor(0), true);
        renderer.text(Stats.getAverageSpeed(), x + renderer.textWidth("Speed : "), y, TextHud.getSectionColor(1), true);
    }
}
