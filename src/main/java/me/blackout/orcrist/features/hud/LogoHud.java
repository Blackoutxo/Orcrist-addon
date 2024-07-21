package me.blackout.orcrist.features.hud;

import me.blackout.orcrist.OrcristAddon;
import meteordevelopment.meteorclient.renderer.GL;
import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class LogoHud extends HudElement {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder().name("Scale").description("Modify the size of the logo.").defaultValue(1.5).min(0).onChanged(aDouble -> calculateSize()).sliderRange(0, 10).build());
    private final Setting<SettingColor> color = sgGeneral.add(new ColorSetting.Builder().name("Color").description("").defaultValue(new SettingColor(255, 255, 255, 255)).build());

    private Identifier LOGO = Identifier.of("orcrist", "logo.png");

    public static final HudElementInfo<LogoHud> INFO = new HudElementInfo<>(OrcristAddon.HUD_GROUP, "LogoHud", "Shows logo of orcrist.", LogoHud::new);

    public LogoHud() {
        super(INFO);

        calculateSize();
    }

    @Override
    public void render(HudRenderer renderer) {
        MatrixStack matrixStack = new MatrixStack();

        GL.bindTexture(LOGO);

        Renderer2D.TEXTURE.begin();
        Renderer2D.TEXTURE.texQuad(x, y, scale.get() * 128, scale.get() * 128, color.get());
        Renderer2D.TEXTURE.render(matrixStack);
    }

    private void calculateSize() {
        setSize(scale.get() * 128, scale.get() * 128);
    }
}
