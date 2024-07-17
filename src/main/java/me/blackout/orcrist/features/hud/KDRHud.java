package me.blackout.orcrist.features.hud;

import me.blackout.orcrist.OrcristAddon;
import me.blackout.orcrist.utils.player.Stats;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.systems.hud.elements.TextHud;

public class KDRHud extends HudElement {
    public static final HudElementInfo<KDRHud> INFO = new HudElementInfo<>(OrcristAddon.HUD_GROUP, "Kill Death Ratio", "Shows your kill to death ratio.", KDRHud::new);
    public KDRHud() {
        super(INFO);
    }

    @Override
    public void render(HudRenderer renderer) {
        setSize(renderer.textWidth("KDR :", true), renderer.textHeight(true));

        renderer.text("KDR : ", x, y, TextHud.getSectionColor(0), true);
        renderer.text(String.valueOf(Stats.getKD()), x + renderer.textWidth("KDR : "), y, TextHud.getSectionColor(1), true);
    }
}
