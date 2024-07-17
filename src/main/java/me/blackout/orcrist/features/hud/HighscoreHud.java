package me.blackout.orcrist.features.hud;

import me.blackout.orcrist.OrcristAddon;
import me.blackout.orcrist.utils.player.Stats;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.systems.hud.elements.TextHud;

public class HighscoreHud extends HudElement {

    public static final HudElementInfo<HighscoreHud> INFO = new HudElementInfo<>(OrcristAddon.HUD_GROUP, "Highscore", "Shows your highscore.", HighscoreHud::new);

    public HighscoreHud() {
            super(INFO);
    }

    @Override
    public void render(HudRenderer renderer) {
        setSize(renderer.textWidth("HighScore :", true), renderer.textHeight(true));

        renderer.text("HighScore : ", x, y, TextHud.getSectionColor(0), true);
        renderer.text(String.valueOf(Stats.highscore), x + renderer.textWidth("HighScore : "), y, TextHud.getSectionColor(1), true);
    }
}
