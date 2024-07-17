package me.blackout.orcrist.features.hud;

import me.blackout.orcrist.OrcristAddon;
import me.blackout.orcrist.utils.misc.Maths;
import me.blackout.orcrist.utils.player.Stats;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.systems.hud.elements.TextHud;
import meteordevelopment.meteorclient.utils.render.color.Color;

public class PlayTimeHud extends HudElement {
    //private long time = System.currentTimeMillis();

    public static final HudElementInfo<PlayTimeHud> INFO = new HudElementInfo<>(OrcristAddon.HUD_GROUP, "PlayTime", "Shows your total playtime.", PlayTimeHud::new);

    public PlayTimeHud() {
        super(INFO);
    }

    @Override
    public void render(HudRenderer renderer) {
        setSize(renderer.textWidth("PlayTime :", true), renderer.textHeight(true));

        renderer.text("PlayTime : ", x, y, TextHud.getSectionColor(0), true);
        renderer.text(Maths.timeElapsed(Stats.playtime()), x + renderer.textWidth("PlayTime : "), y, TextHud.getSectionColor(1), true);
    }
}
