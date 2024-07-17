package me.blackout.orcrist.features.module.world;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import me.blackout.orcrist.OrcristAddon;
import me.blackout.orcrist.utils.misc.PacketManager;
import me.blackout.orcrist.utils.misc.Placeholders;
import me.blackout.orcrist.utils.player.ArmorUtils;
import me.blackout.orcrist.utils.player.CombatHelper;
import meteordevelopment.meteorclient.events.entity.EntityAddedEvent;
import meteordevelopment.meteorclient.events.entity.EntityRemovedEvent;
import meteordevelopment.meteorclient.events.game.GameJoinedEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friend;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Categories;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.entity.fakeplayer.FakePlayerEntity;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.CardinalDirection;
import meteordevelopment.meteorclient.utils.world.Dir;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.item.*;
import net.minecraft.network.packet.s2c.play.BlockBreakingProgressS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import java.util.*;

import static meteordevelopment.meteorclient.utils.player.ChatUtils.formatCoords;

public class NotifierPlus extends Module {
    private final SettingGroup sgArmor = settings.createGroup("Armor");
    private final SettingGroup sgSurround = settings.createGroup("Surround");
    private final SettingGroup sgTotemPops = settings.createGroup("Totem Pops");
    private final SettingGroup sgVisualRange = settings.createGroup("Visual Range");
    private final SettingGroup sgPearl = settings.createGroup("Pearl");
    private final SettingGroup sgOthers = settings.createGroup("Others");

    // Armor
    private final Setting<Boolean> armor = sgArmor.add(new BoolSetting.Builder().name("armor").description("Alerts you when your armor's durablity is low.").defaultValue(true).build());
    private final Setting<Boolean> alertFriend = sgArmor.add(new BoolSetting.Builder().name("friend").description("Alerts your friend when their armor durability is low.").defaultValue(true).build());
    private final Setting<String> msgFormat = sgArmor.add(new StringSetting.Builder().name("msg-format").description("Which messaging format to use for send message to your friend.").defaultValue("/msg").visible(alertFriend::get).build());
    private final Setting<Double> durability = sgArmor.add(new DoubleSetting.Builder().name("durability").description("The durability the armor should be at to warn you.").defaultValue(20).sliderRange(1, 100).build());

    // Surround
    private final Setting<Boolean> surround = sgSurround.add(new BoolSetting.Builder().name("surround").description("Alerts you when someone is breaking your surround block.").defaultValue(true).build());
    private final Setting<Boolean> name = sgSurround.add(new BoolSetting.Builder().name("name").description("Shows the name of player that is breaking your surround.").defaultValue(true).build());
    private final Setting<Boolean> annoy = sgSurround.add(new BoolSetting.Builder().name("annoy").description("Messages player who is breaking your surround.").defaultValue(false).build());
    private final Setting<String> dmFormat = sgSurround.add(new StringSetting.Builder().name("msg-format").description("Which messaging format to use for send message to your friend.").defaultValue("/msg").visible(annoy::get).build());
    private final Setting<String> message = sgSurround.add(new StringSetting.Builder().name("message").description("Which message to send to the player who is breaking your surround.").defaultValue("Hey Dude, Stop breaking my surround!").visible(annoy::get).build());

    // Totem Pops
    private final Setting<Boolean> totemPops = sgTotemPops.add(new BoolSetting.Builder().name("totem-pops").description("Notifies you when a player pops a totem.").defaultValue(true).build());
    private final Setting<Boolean> totemsIgnoreOwn = sgTotemPops.add(new BoolSetting.Builder().name("ignore-own").description("Ignores your own totem pops.").defaultValue(false).build());
    private final Setting<Boolean> totemsIgnoreFriends = sgTotemPops.add(new BoolSetting.Builder().name("ignore-friends").description("Ignores friends totem pops.").defaultValue(false).build());
    private final Setting<Boolean> totemsIgnoreOthers = sgTotemPops.add(new BoolSetting.Builder().name("ignore-others").description("Ignores other players totem pops.").defaultValue(false).build());

    // Visual Range
    private final Setting<Boolean> visualRange = sgVisualRange.add(new BoolSetting.Builder().name("visual-range").description("Notifies you when an entity enters your render distance.").defaultValue(false).build());
    private final Setting<Event> event = sgVisualRange.add(new EnumSetting.Builder<Event>().name("event").description("When to log the entities.").defaultValue(Event.Both).build());
    private final Setting<Set<EntityType<?>>> entities = sgVisualRange.add(new EntityTypeListSetting.Builder().name("entities").description("Which entities to notify about.").defaultValue(EntityType.PLAYER).build());
    private final Setting<Boolean> visualRangeIgnoreFriends = sgVisualRange.add(new BoolSetting.Builder().name("ignore-friends").description("Ignores friends.").defaultValue(true).build());
    private final Setting<Boolean> visualRangeIgnoreFakes = sgVisualRange.add(new BoolSetting.Builder().name("ignore-fake-players").description("Ignores fake players.").defaultValue(true).build());
    private final Setting<Boolean> visualMakeSound = sgVisualRange.add(new BoolSetting.Builder().name("sound").description("Emits a sound effect on enter / leave").defaultValue(true).build());

    // Pearl
    private final Setting<Boolean> pearl = sgPearl.add(new BoolSetting.Builder().name("pearl").description("Notifies you when a player is teleported using an ender pearl.").defaultValue(true).build());
    private final Setting<Boolean> pearlIgnoreOwn = sgPearl.add(new BoolSetting.Builder().name("ignore-own").description("Ignores your own pearls.").defaultValue(false).build());
    private final Setting<Boolean> pearlIgnoreFriends = sgPearl.add(new BoolSetting.Builder().name("ignore-friends").description("Ignores friends pearls.").defaultValue(false).build());

    // Others
    private final Setting<Boolean> burrow = sgOthers.add(new BoolSetting.Builder().name("burrow").description("Sends notifications if players are burrowed.").defaultValue(true).build());
    private final Setting<Boolean> renderBurrow = sgOthers.add(new BoolSetting.Builder().name("render").description("Renders the players that are burrowed.").defaultValue(true).build());
    private final Setting<ShapeMode> shapeMode = sgOthers.add(new EnumSetting.Builder<ShapeMode>().name("shape-mode").description("How the shapes are rendered.").defaultValue(ShapeMode.Both).visible(renderBurrow::get).build());
    private final Setting<SettingColor> sideColor = sgOthers.add(new ColorSetting.Builder().name("side-color").description("The side color of the target block rendering.").defaultValue(new SettingColor(197, 137, 232, 10)).visible(renderBurrow::get).build());
    private final Setting<SettingColor> lineColor = sgOthers.add(new ColorSetting.Builder().name("line-color").description("The line color of the target block rendering.").defaultValue(new SettingColor(197, 137, 232)).visible(renderBurrow::get).build());
    private final Setting<Boolean> bed = sgOthers.add(new BoolSetting.Builder().name("bed").description("Send notifications if players are bed fagging.").defaultValue(true).build());

    // Fields
    private final Object2IntMap<UUID> totemPopMap = new Object2IntOpenHashMap<>();
    private final Object2IntMap<UUID> chatIdMap = new Object2IntOpenHashMap<>();
    private final Map<Integer, Vec3d> pearlStartPosMap = new HashMap<>();

    private boolean alertedHelm, alertedChest, alertedBurrow;
    private boolean alertedLegs, alertedBoots;

    public List<PlayerEntity> bedFags;

    private BlockPos prevBreakPos;

    private final Random random = new Random();

    public NotifierPlus() {
        super(OrcristAddon.World, "notifier+", "Notifies you of different events.");
    }

    // Visual Range

    @EventHandler
    private void onEntityAdded(EntityAddedEvent event) {
        if (!event.entity.getUuid().equals(mc.player.getUuid()) && entities.get().contains(event.entity.getType()) && visualRange.get() && this.event.get() != Event.Despawn) {
            if (event.entity instanceof PlayerEntity) {
                if ((!visualRangeIgnoreFriends.get() || !Friends.get().isFriend(((PlayerEntity) event.entity))) && (!visualRangeIgnoreFakes.get() || !(event.entity instanceof FakePlayerEntity))) {
                    ChatUtils.sendMsg(event.entity.getId() + 100, Formatting.GRAY, "(highlight)%s(default) has entered your visual range!", event.entity.getName().getString());

                    if (visualMakeSound.get())
                        mc.world.playSoundFromEntity(mc.player, mc.player, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.AMBIENT, 3.0F, 1.0F);
                }
            } else {
                MutableText text = Text.literal(event.entity.getType().getName().getString()).formatted(Formatting.WHITE);
                text.append(Text.literal(" has spawned at ").formatted(Formatting.GRAY));
                text.append(formatCoords(event.entity.getPos()));
                text.append(Text.literal(".").formatted(Formatting.GRAY));
                info(text);
            }
        }

        if (pearl.get()) {
            if (event.entity instanceof EnderPearlEntity pearl) {
                pearlStartPosMap.put(pearl.getId(), new Vec3d(pearl.getX(), pearl.getY(), pearl.getZ()));
            }
        }
    }

    @EventHandler
    private void onEntityRemoved(EntityRemovedEvent event) {
        if (!event.entity.getUuid().equals(mc.player.getUuid()) && entities.get().contains(event.entity.getType()) && visualRange.get() && this.event.get() != Event.Spawn) {
            if (event.entity instanceof PlayerEntity) {
                if ((!visualRangeIgnoreFriends.get() || !Friends.get().isFriend(((PlayerEntity) event.entity))) && (!visualRangeIgnoreFakes.get() || !(event.entity instanceof FakePlayerEntity))) {
                    ChatUtils.sendMsg(event.entity.getId() + 100, Formatting.GRAY, "(highlight)%s(default) has left your visual range!", event.entity.getName().getString());

                    if (visualMakeSound.get())
                        mc.world.playSoundFromEntity(mc.player, mc.player, SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.AMBIENT, 3.0F, 1.0F);
                }
            } else {
                MutableText text = Text.literal(event.entity.getType().getName().getString()).formatted(Formatting.WHITE);
                text.append(Text.literal(" has despawned at ").formatted(Formatting.GRAY));
                text.append(formatCoords(event.entity.getPos()));
                text.append(Text.literal(".").formatted(Formatting.GRAY));
                info(text);
            }
        }

        if (pearl.get()) {
            Entity e = event.entity;
            int i = e.getId();
            if (pearlStartPosMap.containsKey(i)) {
                EnderPearlEntity pearl = (EnderPearlEntity) e;
                if (pearl.getOwner() != null && pearl.getOwner() instanceof PlayerEntity p) {
                    double d = pearlStartPosMap.get(i).distanceTo(e.getPos());
                    if ((!Friends.get().isFriend(p) || !pearlIgnoreFriends.get()) && (!p.equals(mc.player) || !pearlIgnoreOwn.get())) {
                        info("(highlight)%s's(default) pearl landed at %d, %d, %d (highlight)(%.1fm away, travelled %.1fm)(default).", pearl.getOwner().getName().getString(), pearl.getBlockPos().getX(), pearl.getBlockPos().getY(), pearl.getBlockPos().getZ(), pearl.distanceTo(mc.player), d);
                    }
                }
                pearlStartPosMap.remove(i);
            }
        }
    }

    // Totem Pops & Armor

    @Override
    public void onActivate() {
        // Armor
        alertedHelm = false;
        alertedChest = false;
        alertedLegs = false;
        alertedBoots = false;

        // Burrow
        alertedBurrow = false;

        if (alertFriend.get() && Friends.get().isFriend(mc.player)) info("Remove yourself from friends list for alerting friends to work.");

        // Totem
        totemPopMap.clear();
        chatIdMap.clear();
        pearlStartPosMap.clear();
    }

    @EventHandler
    private void onGameJoin(GameJoinedEvent event) {
        bedFags.clear();
        totemPopMap.clear();
        chatIdMap.clear();
        pearlStartPosMap.clear();
    }

    @EventHandler
    private void onReceivePacket(PacketEvent.Receive event) {
        // Surround
        if (surround.get()) alertSurround(event);

        // Totem Pops
        if (!totemPops.get()) return;
        if (!(event.packet instanceof EntityStatusS2CPacket p)) return;
        if (p.getStatus() != 35) return;

        Entity entity = p.getEntity(mc.world);

        if (!(entity instanceof PlayerEntity)) return;

        if ((entity.equals(mc.player) && totemsIgnoreOwn.get())
            || (Friends.get().isFriend(((PlayerEntity) entity)) && totemsIgnoreOthers.get()) || (!Friends.get().isFriend(((PlayerEntity) entity)) && totemsIgnoreFriends.get())
            ) return;

        synchronized (totemPopMap) {
            int pops = totemPopMap.getOrDefault(entity.getUuid(), 0);
            totemPopMap.put(entity.getUuid(), ++pops);

            ChatUtils.sendMsg(getChatId(entity), Formatting.GRAY, "(highlight)%s (default)popped (highlight)%d (default)%s.", entity.getName().getString(), pops, pops == 1 ? "totem" : "totems");
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        // Bed
        if (bed.get()) alertBed();

        // Burrow
        if (burrow.get()) alertBurrow();

        // Armor
        if (armor.get()) alertArmor();

        // Totem Pops
        if (totemPops.get()) {
            synchronized (totemPopMap) {
                for (PlayerEntity player : mc.world.getPlayers()) {
                    if (!totemPopMap.containsKey(player.getUuid())) continue;

                    if (player.deathTime > 0 || player.getHealth() <= 0) {
                        int pops = totemPopMap.removeInt(player.getUuid());

                        ChatUtils.sendMsg(getChatId(player), Formatting.GRAY, "(highlight)%s (default)died after popping (highlight)%d (default)%s.", player.getName().getString(), pops, pops == 1 ? "totem" : "totems");
                        chatIdMap.removeInt(player.getUuid());
                    }
                }
            }
        }
    }

    private int getChatId(Entity entity) {
        return chatIdMap.computeIfAbsent(entity.getUuid(), value -> random.nextInt());
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (!renderBurrow.get() && !burrow.get()) return;

        for (PlayerEntity entity : mc.world.getPlayers()) if (CombatHelper.isBurrowed(entity, false))
        event.renderer.box(entity.getBlockPos(), sideColor.get(), lineColor.get(), shapeMode.get(), 0);
    }

    // Armor
    private void alertArmor() {
        for (PlayerEntity player : mc.world.getPlayers()) {

            Iterable<ItemStack> armorPieces = mc.player.getArmorItems();

            for (ItemStack armorPiece : armorPieces) {

                if (ArmorUtils.checkThreshold(armorPiece, durability.get())) {
                    if (ArmorUtils.isHelm(armorPiece) && !alertedHelm) {
                        sendNotification(player, armorPiece.getItem(), alertFriend.get());
                        alertedHelm = true;
                    }

                    if (ArmorUtils.isChest(armorPiece) && !alertedChest) {
                        sendNotification(player, armorPiece.getItem(), alertFriend.get());
                        alertedChest = true;
                    }

                    if (ArmorUtils.isLegs(armorPiece) && !alertedLegs) {
                        sendNotification(player, armorPiece.getItem(), alertFriend.get());
                        alertedLegs = true;
                    }

                    if (ArmorUtils.isBoots(armorPiece) && !alertedBoots) {
                        sendNotification(player, armorPiece.getItem(), alertFriend.get());
                        alertedBoots = true;
                    }
                }

                if (!ArmorUtils.checkThreshold(armorPiece, durability.get())) {
                    if (ArmorUtils.isHelm(armorPiece) && alertedHelm) alertedHelm = false;
                    if (ArmorUtils.isChest(armorPiece) && alertedChest) alertedChest = false;
                    if (ArmorUtils.isLegs(armorPiece) && alertedLegs) alertedLegs = false;
                    if (ArmorUtils.isBoots(armorPiece) && alertedBoots) alertedBoots = false;
                }
            }
        }
    }

    private void sendNotification(PlayerEntity player, Item item, boolean friend) {
        if (!(item instanceof ArmorItem)) return;

        // Grammar
        String grammar = (item.getName().getString().endsWith("s") ? " are" : " is");

        // Friend
        if (friend && Friends.get().isFriend(player)) CombatHelper.sendDM("Your " + item.getName().getString() + grammar + " low!", msgFormat.get(), player == mc.player ? null : player);

        // Player
        if (player == mc.player) warning("Your " + item.getName().getString() + grammar + " low!");
    }

    // Burrow
    private void alertBurrow() {
        for (PlayerEntity entity : mc.world.getPlayers()) {
            if (CombatHelper.isBurrowed(entity, false) && !alertedBurrow) {
                info(entity.getName().getString() + " is burrowed.");
                alertedBurrow = true;
            } else alertedBurrow = false;
        }
    }

    // Bed
    private void alertBed() {
        for (PlayerEntity entity : mc.world.getPlayers()) {
            if (entity.getHandItems() instanceof BedItem && mc.world.getDimension().bedWorks() && !bedFags.contains(entity)) {
                info(entity.getName().getString() + " has bed.");
                bedFags.add(entity);
                break;
            }
        }
    }

    // Surround
    private void alertSurround(PacketEvent.Receive event) {
        if (!(event.packet instanceof BlockBreakingProgressS2CPacket bbpp) || mc.world == null || mc.player == null || !CombatHelper.isInHole(mc.player, false)) return;

        BlockPos bbp = bbpp.getPos();

        if(bbp.equals(prevBreakPos) && bbpp.getProgress() > 0) return;

        PlayerEntity breakingPlayer = (PlayerEntity) mc.world.getEntityById(bbpp.getEntityId());
        BlockPos playerBlockPos = mc.player.getBlockPos();

        if(Objects.equals(breakingPlayer, mc.player)) return;

        if (annoy.get()) CombatHelper.sendDM(message.get(), dmFormat.get(), breakingPlayer);

        if(bbp.equals(playerBlockPos.north())) warning(name.get() ? breakingPlayer.getName().getString() + " is breaking your north surround block!" : "Surround is being broken from north!");
        if (bbp.equals(playerBlockPos.east())) warning(name.get() ? breakingPlayer.getName().getString() + " is breaking your east surround block!" : "Surround is being broken from east!");
        if (bbp.equals(playerBlockPos.south())) warning(name.get() ? breakingPlayer.getName().getString() + " is breaking your south surround block!" : "Surround is being broken from south!");
        if (bbp.equals(playerBlockPos.west())) warning(name.get() ? breakingPlayer.getName().getString() + " is breaking your west surround block!" : "Surround is being broken from west!");

        prevBreakPos = bbp;
    }

    public enum Event {
        Spawn,
        Despawn,
        Both
    }
}
