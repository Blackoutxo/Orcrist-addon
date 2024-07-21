package me.blackout.orcrist.features.module.misc;

import me.blackout.orcrist.OrcristAddon;
import me.blackout.orcrist.utils.misc.ColorUtils;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.events.game.SendMessageEvent;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.mixin.ChatHudAccessor;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.config.Config;
import meteordevelopment.meteorclient.systems.friends.Friend;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.NameProtect;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.starscript.compiler.Expr;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

public class BetterChatPlus extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgPrefix = settings.createGroup("Prefix");

    // General
    private final Setting<Highlight> highlight = sgGeneral.add(new EnumSetting.Builder<Highlight>().name("highlight").description("Who to highlight. Friends, yourself or both.").defaultValue(Highlight.Both).build());
    private final Setting<SettingColor> sColor = sgGeneral.add(new ColorSetting.Builder().name("color").description("Which color to highlight yourself with.").defaultValue(new SettingColor(0, 255, 0, 255)).visible(() -> highlight.get() == Highlight.Self || highlight.get() == Highlight.Both).build());
    private final Setting<Boolean> appendEmoji  =sgGeneral.add(new BoolSetting.Builder().name("emoji").description("Send emoji in chat, example - :skull:").defaultValue(true).build());
    private final Setting<Boolean> mentionAlert = sgGeneral.add(new BoolSetting.Builder().name("mention-alert").description("Plays a sound when your name is mentioned in chat.").defaultValue(true).build());

    // Prefix
    private final Setting<String> customPrefix = sgPrefix.add(new StringSetting.Builder().name("custom-prefix").description("Orcrist's custom prefix.").defaultValue("Orcrist").onChanged(this::changePrefix).build());
    private final Setting<Boolean> overrideMeteor = sgPrefix.add(new BoolSetting.Builder().name("override-meteor").description("Will always show orcrist's prefix.").onChanged(this::changePrefix).build());
    private final Setting<Boolean> rainbow = sgPrefix.add(new BoolSetting.Builder() .name("rainbow-prefix").description( "Enables a proper rainbow prefix.").defaultValue(true).build());
    private final Setting<Boolean> synchro = sgPrefix.add(new BoolSetting.Builder().name("synchronized").description("Synchronizes the words.").defaultValue(true).visible(rainbow::get).build());
    private final Setting<Double> rainbowSpeed = sgPrefix.add(new DoubleSetting.Builder().name("rainbow-speed").description( "Rainbow speed for the prefix.").defaultValue(0.0035).visible(rainbow::get).sliderRange(0.0035, 0.1).build());
    private final Setting<Double> rainbowLineSpread = sgPrefix.add(new DoubleSetting.Builder().name("rainbow-line-spread").description( "Rainbow spread for the prefix per line.").defaultValue(0.02).sliderMax(0.05).visible(rainbow::get).build());
    private final Setting<Double> rainbowWordSpread = sgPrefix.add(new DoubleSetting.Builder().name("rainbow-word-spread").description( "Rainbow spread for the prefix inside word.").defaultValue(0.02).sliderMax(0.1).visible(rainbow::get).build());
    private final Setting<Integer> maxMessages = sgPrefix.add(new IntSetting.Builder().name("max-messages").description( "How many lines of chat to scan for rainbow.").defaultValue(50).visible(rainbow::get).sliderMax(100).build());
    private final Setting<Boolean> meteor = sgPrefix.add(new BoolSetting.Builder().name("apply-to-meteor").description( "Enables the rainbow also for the meteor prefix.").defaultValue(false).visible(rainbow::get).build());
//    private final Setting<Boolean> otherAddons = sgPrefix.add(new BoolSetting.Builder().name("apply-to-other-addons").description( "Enables the rainbow also for other addons.").defaultValue(false).visible(rainbow::get).build());
//    private final Setting<List<String>> addons = sgPrefix.add(new StringListSetting.Builder().name("addons").description("list of addons to apply the rainbow to").defaultValue("[BlackOut]").visible(() -> rainbow.get() && otherAddons.get()).build());

    // Fields
    private final Color rgb = new Color(255, 255, 255);
    private double rainbowHue1;
    private double rainbowHue2;

    public BetterChatPlus() {
        super(OrcristAddon.Misc, "better-chat+", "Additional features that aren't in better chat.");
    }

    @EventHandler
    private synchronized void onRender(Render2DEvent event) {
        if (rainbow.get()) {
            rainbowHue1 += rainbowSpeed.get() * 0.45992073;
            if (rainbowHue1 > 1.0) {
                --rainbowHue1;
            } else if (rainbowHue1 < -1.0) {
                ++rainbowHue1;
            }


            rainbowHue2 = rainbowHue1;
            List<ChatHudLine.Visible> visible = ((ChatHudAccessor) mc.inGameHud.getChatHud()).getVisibleMessages();

            for(int index = Math.min(maxMessages.get(), visible.size() - 1); index > -1; --index) {
                ChatHudLine.Visible line = visible.get(index);
                OrderedText content = line.content();
                if (content == null) {
                    return;
                }

                MutableText parsed = Text.literal("");
                int totalChars = 0;

                content.accept((i, style, codePoint) -> {
                    parsed.append(Text.literal(new String(Character.toChars(codePoint))).setStyle(style));
                    return true;
                });

                String aPrefix = "[" + customPrefix.get() + "]";
                int aIndex = parsed.getString().indexOf(aPrefix) - (parsed.getString().length() - parsed.getSiblings().size());
                if (aIndex > -1) {
                    parsed.getSiblings().subList(aIndex, Math.min(aIndex + aPrefix.length(), parsed.getString().length() - 1)).clear();
                    parsed.getSiblings().add(aIndex, applyRgb(aPrefix));
                    totalChars += aPrefix.length();
                }

                if (meteor.get()) {
                    String mPrefix = "[Meteor]";
                    int mIndex = parsed.getString().indexOf(mPrefix) - (parsed.getString().length() - parsed.getSiblings().size());
                    if (mIndex > -1) {
                        parsed.getSiblings().subList(mIndex, Math.min(mIndex + mPrefix.length(), parsed.getString().length() - 1)).clear();
                        parsed.getSiblings().add(mIndex, applyRgb(mPrefix));
                        totalChars += mPrefix.length();
                    }
                }

                rainbowHue2 -= rainbowLineSpread.get();

                if (synchro.get()) rainbowHue2 += rainbowWordSpread.get() * totalChars;

                ((ChatHudAccessor) mc.inGameHud.getChatHud()).getVisibleMessages().set(index, new ChatHudLine.Visible(line.addedTime(), parsed.asOrderedText(), null, true));
            }
        }
    }

    @EventHandler
    private void onMessageReceive(ReceiveMessageEvent event) {
        Text message = event.getMessage();
        MutableText parsed = Text.literal("");

        String selfName = (Modules.get().get(NameProtect.class)).getName(mc.player.getName().getString());

        if (highlight.get() == Highlight.Self || highlight.get() == Highlight.Both) {
            int nameIndex = parsed.getString().indexOf(selfName);
            if (nameIndex > -1) {
                parsed.getSiblings().subList(nameIndex, nameIndex + selfName.length()).clear();
                parsed.getSiblings().add(nameIndex, ColorUtils.coloredText(selfName, sColor.get()));
            }
        }

        if (mentionAlert.get() && message.contains(Text.of(selfName))) mc.world.playSound(mc.player.getX(), mc.player.getY(), mc.player.getZ(), SoundEvents.BLOCK_DISPENSER_LAUNCH, SoundCategory.PLAYERS, 1, 1.2f, false);

        if (highlight.get() == Highlight.Friends || highlight.get() == Highlight.Both) {
            for(Friend friend : Friends.get()) {
                if (!friend.name.equals(mc.player.getName().getString()) || highlight.get() != Highlight.Self) {
                    int nameIndex = parsed.getString().indexOf(friend.name) - (parsed.getString().length() - parsed.getSiblings().size());
                    if (nameIndex > -1) {
                        parsed.getSiblings().subList(nameIndex, nameIndex + friend.name.length()).clear();
                        parsed.getSiblings().add(nameIndex, ColorUtils.coloredText(friend.name, Config.get().friendColor.get()));
                        break;
                    }
                }
            }
        }

        message.asOrderedText().accept((i, style, codePoint) -> {
            parsed.append(Text.literal(new String(Character.toChars(codePoint))).setStyle(style));
            return true;
        });

        event.setMessage(message);
    }

    @EventHandler
    private void onMessageSend(SendMessageEvent event) {
        String message = event.message;
        if (appendEmoji.get()) message = apply(message);
        event.message = message;
    }

    private MutableText applyRgb(String text) {
        MutableText prefix = Text.literal("");

        for(int i = 0; i < text.length(); ++i) {
            int c = java.awt.Color.HSBtoRGB((float) rainbowHue2, 1.0F, 1.0F);
            rgb.r = Color.toRGBAR(c);
            rgb.g = Color.toRGBAG(c);
            rgb.b = Color.toRGBAB(c);
            prefix.append(ColorUtils.coloredText(text.substring(i, i + 1), rgb));
            rainbowHue2 -= rainbowWordSpread.get();
        }

        return prefix;
    }

    private void changePrefix(boolean bool) {
        changePrefix("");
    }

    private void changePrefix(SettingColor color) {
        changePrefix("");
    }

    private void changePrefix(String string) {
        ChatUtils.registerCustomPrefix("me.blackout.orcrist", this::prefix);

        if (overrideMeteor.get()) {
            ChatUtils.registerCustomPrefix("meteordevelopment", this::prefix);
        } else {
            ChatUtils.registerCustomPrefix("meteordevelopment", this::customMeteorPrefix);
        }
    }

//    private void changeMeteorPrefix() {
//        ChatUtils.registerCustomPrefix("meteordevelopment", this::customMeteorPrefix);
//    }

    private MutableText prefix() {
        MutableText prefix = ColorUtils.coloredText("[", color.set(128, 128, 128, 255));
        prefix.append(ColorUtils.coloredText(customPrefix.get(), color.set(160,107,244, 255)));
        prefix.append(ColorUtils.coloredText("]", color.set(128, 128, 128, 255)));

        return prefix.append(" ");
    }

    private MutableText customMeteorPrefix() {
        MutableText prefix = ColorUtils.coloredText("[", color.set(128, 128, 128, 255));
        prefix.append(ColorUtils.coloredText("Meteor", MeteorClient.ADDON.color));
        prefix.append(ColorUtils.coloredText("]", color.set(128, 128, 128, 255)));
        return prefix.append(" ");
    }

    public static String apply(String msg) {
        if (msg.contains(":skull:")) msg = msg.replace(":skull:", "☠");
        if (msg.contains(":heart:")) msg = msg.replace(":heart:", "❤‍");
        if (msg.contains("fire:")) msg = msg.replace(":fire:", "🔥");
        if (msg.contains(":tm:")) msg = msg.replace(":tm:", "™");
        if (msg.contains(":pick:")) msg = msg.replace(":pick:", "⛏");
        if (msg.contains(":axe:")) msg = msg.replace(":axe:", "🪓");
        if (msg.contains(":lightning:")) msg = msg.replace(":lightning:", "⚡");
        if (msg.contains(":snowflake:")) msg = msg.replace(":snowflake:", "❆");
        if (msg.contains(":star:")) msg = msg.replace(":star:", "☆");
        if (msg.contains(":gear:")) msg = msg.replace(":gear:", "⚙️");
        if (msg.contains(":diamond:")) msg = msg.replace(":diamond", "💎");
        if (msg.contains(":dagger")) msg = msg.replace(":dagger", "🗡️");

        return msg;
    }

    public enum Highlight {
        Friends,
        Self,
        Both
    }
}
