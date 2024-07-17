package me.blackout.orcrist.features.hud;

import me.blackout.orcrist.OrcristAddon;
import meteordevelopment.meteorclient.renderer.Mesh;
import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.systems.hud.elements.CombatHud;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;

import static me.blackout.orcrist.utils.player.Stats.messages;

public class NotificationHud extends HudElement {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgColors = settings.createGroup("Colors");

    // General
    private final Setting<Boolean> background = sgGeneral.add(new BoolSetting.Builder().name("background").description("Displays background of notification.").defaultValue(false).build());
    private final Setting<Boolean> side = sgGeneral.add(new BoolSetting.Builder().name("side-background").description("Displays side background of notification.").defaultValue(false).visible(background :: get).build());

    // Colors
    private final Setting<SettingColor> textColor = sgColors.add(new ColorSetting.Builder().name("text-color").description("Color for the text,").defaultValue(new SettingColor(255, 255, 255, 255)).build());
    private final Setting<SettingColor> backgroundColor = sgColors.add(new ColorSetting.Builder().name("background-color").description(".").defaultValue(new SettingColor(0, 0, 0, 255)).visible(background :: get).build());
    private final Setting<SettingColor> sideColor = sgColors.add(new ColorSetting.Builder().name("side-color").description(".").defaultValue(new SettingColor(0, 0, 0, 255)).visible(() -> side.get() && background.get()).build());

    private int removeTimer;

    public static final HudElementInfo<NotificationHud> INFO = new HudElementInfo<>(OrcristAddon.HUD_GROUP, "Notification", "Shows notification in hud.", NotificationHud::new);

    public NotificationHud() {
        super(INFO);
    }

    @Override
    public void tick(HudRenderer renderer) {
        double width = 0;
        double height = 0;
        removeMessage();
        int i = 0;

        if (messages.isEmpty()) {
            String t = "Notifications";
            width = Math.max(width, renderer.textWidth(t));
            height += renderer.textHeight();
        } else {
            for (String mes : messages) {
                width = Math.max(width, renderer.textWidth(mes));
                height += renderer.textHeight();
                if (i > 0) height += 2;
                i++;
            }
        }

        box.setSize(width, height);
    }

    @Override
    public void render(HudRenderer renderer) {
        double x = box.x;
        double y = box.y;
        removeMessage();

        int w = getWidth();
        int h = getHeight();

        if (isInEditor()) {
            renderer.text("Notifications", x, y, textColor.get(), true);
            if (side.get() && background.get()) renderer.quad(x - 5, y, w + 10, TextRenderer.get().getHeight(), sideColor.get());
            if (background.get()) renderer.quad(x, y, w, TextRenderer.get().getHeight(), backgroundColor.get());
            Renderer2D.COLOR.render(null);
            return;
        }

        int i = 0;
        if (messages.isEmpty()) {
            String t = "";
            renderer.text(t, x + renderer.textWidth(t), y, textColor.get(), true);
        } else {
            for (String mes: messages) {
                if (!messages.isEmpty()){
                    Renderer2D.COLOR.begin();
                    if (side.get() && background.get()) renderer.quad(x - 5, y, TextRenderer.get().getWidth(mes) + 10, h, sideColor.get());
                    if (background.get()) renderer.quad(x, y, TextRenderer.get().getWidth(mes), h, backgroundColor.get());
                    Renderer2D.COLOR.render(null);
                }

                renderer.text(mes, x + renderer.textWidth(mes), y, textColor.get(), true);
                y += renderer.textHeight();
                if (i > 0) y += 2;
                i++;
            }
        }
    }

    private void removeMessage() {
        if (removeTimer >= 75 && !messages.isEmpty()) {
            messages.remove(0);
            removeTimer = 0;
        } else removeTimer++;
    }
}
