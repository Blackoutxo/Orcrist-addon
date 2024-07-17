package me.blackout.orcrist.features.module.combat;

import me.blackout.orcrist.OrcristAddon;
import me.blackout.orcrist.utils.misc.PacketManager;
import me.blackout.orcrist.utils.player.ArmorUtils;
import me.blackout.orcrist.utils.player.CombatHelper;
import me.blackout.orcrist.utils.player.ItemHelper;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.combat.KillAura;
import meteordevelopment.meteorclient.systems.modules.combat.Offhand;
import meteordevelopment.meteorclient.utils.player.*;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

public class AutoXp extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    // ------------------ General ------------------ //

    private final Setting<Double> enableAt = sgGeneral.add(new DoubleSetting.Builder().name("durability").description("What durability to enable at.").defaultValue(20).min(1).sliderMin(1).sliderMax(100).max(100).build());
    private final Setting<Double> minHealth = sgGeneral.add(new DoubleSetting.Builder().name("min-health").description("Min health for repairing.").defaultValue(10).min(0).sliderMax(36).max(36).build());
    private final Setting<Boolean> moduleControl = sgGeneral.add(new BoolSetting.Builder().name("pause-modules").description("Disable combat modules while repairing armor.").defaultValue(true).build());
    private final Setting<Boolean> onlyInHole = sgGeneral.add(new BoolSetting.Builder().name("only-in-hole").description("Only throw XP while in a hole.").defaultValue(false).build());
    private final Setting<Boolean> silent = sgGeneral.add(new BoolSetting.Builder().name("silent").description("Allows you to use other hotbar slots while throwing XP.").defaultValue(false).build());
    private final Setting<Boolean> refill = sgGeneral.add(new BoolSetting.Builder().name("refill").description("Moves XP from your inventory to your hotbar when you run out.").defaultValue(false).build());
    private final Setting<Boolean> offhand = sgGeneral.add(new BoolSetting.Builder().name("offhand").description("Uses your offhand for XP.").defaultValue(false).build());
    private final Setting<Integer> refillSlot = sgGeneral.add(new IntSetting.Builder().name("refill-slot").description("Which slot to refill.").defaultValue(1).min(1).sliderMin(1).max(9).sliderMax(9).visible(refill::get).build());
    private final Setting<Boolean> lookDown = sgGeneral.add(new BoolSetting.Builder().name("look-down").description("Throws the XP at your feet.").defaultValue(true).build());

    private boolean alerted, toggledOffhand;
    private int slotRefill;

    public AutoXp(){
        super(OrcristAddon.Combat, "auto-xp", "Automatically throws experience bottle to repair armor.");
    }

    @Override
    public void onActivate() {
        if (moduleControl.get()) {
            AutoCrystal ca = Modules.get().get(AutoCrystal.class);
            KillAura ka = Modules.get().get(KillAura.class);
            KillAuraPlus kaP = Modules.get().get(KillAuraPlus.class);
            BedBomb ba = Modules.get().get(BedBomb.class);
            Offhand offhandClass = Modules.get().get(Offhand.class);
            if (ca.isActive()) ca.toggle();
            if (ka.isActive()) ka.toggle();
            if (ba.isActive()) ba.toggle();
            if (kaP.isActive()) kaP.toggle();
            if (offhandClass.isActive() && offhand.get()) {
                toggledOffhand = true;
                offhandClass.toggle();
            }
        }
        alerted = false;
        slotRefill = refillSlot.get() - 1;
    }

    @Override
    public void onDeactivate() {
        Offhand offhand = Modules.get().get(Offhand.class);
        if (moduleControl.get() && toggledOffhand && !offhand.isActive()) offhand.toggle();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        assert mc.player != null;

        if (CombatHelper.getTotalHealth(mc.player) <= minHealth.get()) {
            error("Your health is too low!");
            toggle();
            return;
        }

        if (onlyInHole.get() && !PlayerUtils.isInHole(true)) {
            error("You're not in a hole!");
            toggle();
            return;
        }

        if (refillSlotEmpty(false) && !offhand.get()) {
            if (refill.get()) {
                FindItemResult invXP = InvUtils.find(Items.EXPERIENCE_BOTTLE);
                if (invXP.found()) {
                    InvUtils.move().from(invXP.slot()).toHotbar(slotRefill);
                } else {
                    error("You're out of experience bottle! disabling...");
                    toggle();
                    return;
                }
            } else {
                error("No experience bottle in hotbar!");
                toggle();
                return;
            }
        }

        if (refillSlotEmpty(true) && offhand.get()) {
            FindItemResult invXP = InvUtils.find(Items.EXPERIENCE_BOTTLE);
            if (invXP.found()) {
                InvUtils.move().from(invXP.slot()).toOffhand();
            } else {
                error("You're out of Experience bottle!");
                toggle();
                return;
            }
        }

        boolean needsRepair = shouldRepair();
        if (!needsRepair) {
            if (alerted) info("Finished repair.");
            toggle();
            return;
        }
        if (!alerted) {
            info("Repairing armor to " + enableAt.get() + "%%");
            alerted = true;
        }
        if (lookDown.get()) {
            Rotations.rotate(mc.player.getYaw(), 90, 50, this::throwXP);
        } else {
            throwXP();
        }
    }

    private void throwXP() {
        int lastSlot = mc.player.getInventory().selectedSlot;
        if (offhand.get()) {
            PacketManager.interactItem(Hand.OFF_HAND);
        } else {
            PacketManager.updateSlot(slotRefill);
            PacketManager.interactItem(Hand.MAIN_HAND);
            if (silent.get() && lastSlot != -1) PacketManager.updateSlot(lastSlot);
        }
    }

    private boolean shouldRepair() {
        for (int i = 0; i < 4; i++) if (ArmorUtils.checkThreshold(ArmorUtils.getArmor(i), enableAt.get())) return true;
        return false;
    }

    private boolean refillSlotEmpty(boolean offhand) {
        if (offhand) return ItemHelper.getItemFromSlot(SlotUtils.OFFHAND) != Items.EXPERIENCE_BOTTLE;
        return ItemHelper.getItemFromSlot(slotRefill) != Items.EXPERIENCE_BOTTLE;
    }
}
