package me.blackout.orcrist.features.hud;

import me.blackout.orcrist.OrcristAddon;
import me.blackout.orcrist.utils.misc.Maths;
import me.blackout.orcrist.utils.player.Stats;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.systems.hud.elements.TextHud;
import meteordevelopment.meteorclient.utils.render.color.Color;

public class DeathHud extends HudElement {
    public static final HudElementInfo<DeathHud> INFO = new HudElementInfo<>(OrcristAddon.HUD_GROUP, "Death", "Shows your total amount of deaths.", DeathHud::new);
    public DeathHud() {
        super(INFO);
    }

    @Override
    public void render(HudRenderer renderer) {
        setSize(renderer.textWidth("Death : ", true), renderer.textHeight(true));

        renderer.text("Death : ", x, y, TextHud.getSectionColor(0), true);
        renderer.text(String.valueOf(Stats.death), x + renderer.textWidth("Death : "), y, TextHud.getSectionColor(1), true);
    }
}
