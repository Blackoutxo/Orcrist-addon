package me.blackout.orcrist.features.hud;

import me.blackout.orcrist.OrcristAddon;
import me.blackout.orcrist.utils.player.Stats;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.systems.hud.elements.TextHud;

public class KillStreakHud extends HudElement {
    public static final HudElementInfo<KillStreakHud> INFO = new HudElementInfo<>(OrcristAddon.HUD_GROUP, "KillStreak", "Shows your current killstreak.", KillStreakHud::new);
    public KillStreakHud() {
        super(INFO);
    }

    @Override
    public void render(HudRenderer renderer) {
        setSize(renderer.textWidth("KillStreak :", true), renderer.textHeight(true));

        renderer.text("KillStreak : ", x, y, TextHud.getSectionColor(0), true);
        renderer.text(String.valueOf(Stats.killstreak), x + renderer.textWidth("KillStreak : "), y, TextHud.getSectionColor(1), true);
    }
}
