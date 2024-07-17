//package me.blackout.orcrist.features.module.combat;
//
//import me.blackout.orcrist.OrcristAddon;
//import me.blackout.orcrist.utils.experimental.Origin;
//import me.blackout.orcrist.utils.player.DamageCalculator;
//import me.blackout.orcrist.utils.world.BlockHelper;
//import meteordevelopment.meteorclient.events.packets.PacketEvent;
//import meteordevelopment.meteorclient.events.render.Render3DEvent;
//import meteordevelopment.meteorclient.events.world.TickEvent;
//import meteordevelopment.meteorclient.renderer.ShapeMode;
//import meteordevelopment.meteorclient.settings.*;
//import meteordevelopment.meteorclient.systems.friends.Friends;
//import meteordevelopment.meteorclient.systems.modules.Module;
//import meteordevelopment.meteorclient.utils.Utils;
//import meteordevelopment.meteorclient.utils.entity.EntityUtils;
//import meteordevelopment.meteorclient.utils.player.FindItemResult;
//import meteordevelopment.meteorclient.utils.player.InvUtils;
//import meteordevelopment.meteorclient.utils.player.PlayerUtils;
//import meteordevelopment.meteorclient.utils.player.Rotations;
//import meteordevelopment.meteorclient.utils.render.color.Color;
//import meteordevelopment.meteorclient.utils.world.BlockUtils;
//import meteordevelopment.orbit.EventHandler;
//import net.minecraft.block.Block;
//import net.minecraft.block.BlockState;
//import net.minecraft.block.Blocks;
//import net.minecraft.block.PistonBlock;
//import net.minecraft.entity.Entity;
//import net.minecraft.entity.LivingEntity;
//import net.minecraft.entity.decoration.EndCrystalEntity;
//import net.minecraft.entity.player.PlayerEntity;
//import net.minecraft.item.Item;
//import net.minecraft.item.Items;
//import net.minecraft.network.packet.Packet;
//import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
//import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
//import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
//import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
//import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
//import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
//import net.minecraft.util.Hand;
//import net.minecraft.util.math.BlockPos;
//import net.minecraft.util.math.Box;
//import net.minecraft.util.math.Direction;
//import net.minecraft.util.math.Vec3d;
//
//import java.util.Comparator;
//import java.util.List;
//import java.util.function.Predicate;
//import java.util.stream.Collectors;
//
//public class PistonAura extends Module {
//
//    private final SettingGroup sgGeneral = settings.getDefaultGroup();
//    private final SettingGroup sgToggles = settings.createGroup("Toggles");
//    //private final SettingGroup sgSwitch = settings.createGroup("Switch");
//
//    private final Setting<PistonAura.DamageCalc> damageCalc = sgGeneral.add(new EnumSetting.Builder<PistonAura.DamageCalc>().name("damage-calc-position").description( "Where to check for crystal damage").defaultValue( PistonAura.DamageCalc.PLACEPOS).build());
//    private final Setting<Integer> targetRange = sgGeneral.add(new IntSetting.Builder().name("target-range").description("Target range").defaultValue(3).sliderRange(1, 6).build());
//    private final Setting<Double> minimalDamage = sgGeneral.add(new DoubleSetting.Builder().name("minimal-damage").description("Minimal damage for the target to be valid.").defaultValue(12).sliderRange(0, 100).build());
//    private final Setting<Integer> actionInterval = sgGeneral.add(new IntSetting.Builder().name("action-interval").description("delay between actions").defaultValue(0).sliderRange(0, 10).build());
//    private final Setting<Integer> switchDelay = sgGeneral.add(new IntSetting.Builder().name("delay-between-switch").description("Delay between swapping inventory slots.").defaultValue(3).sliderRange(0, 10).build());
//    private final Setting<Integer> blockingBreakDelay = sgGeneral.add(new IntSetting.Builder().name("blocking-break-delay").description("Time in ticks for when to break a blocking crystal.").defaultValue(5).sliderRange(0, 10).build());
//
//    private final Setting<Boolean> swing = sgToggles.add(new BoolSetting.Builder().name("swing").description("swings hand").defaultValue(true).build());
//    private final Setting<Boolean> disableWhenNone = sgToggles.add(new BoolSetting.Builder().name("disable-when-none").description("Disables when the module out of resources.").defaultValue(true).build());
//    private final Setting<Boolean> strict = sgToggles.add(new BoolSetting.Builder().name("strict").description("Strict mode").defaultValue(false).build());
//    private final Setting<Boolean> antiSuicide = sgToggles.add(new BoolSetting.Builder().name("anti-suicide").description("Prevents you from dying. (doesn't seem to work)").defaultValue(false).build());
//    private final Setting<Boolean> mine = sgToggles.add(new BoolSetting.Builder().name("mine").description("Mines redstone blocks.").defaultValue(false).build());
//    private final Setting<Boolean> torchSupport = sgToggles.add(new BoolSetting.Builder().name("torch-support").description("Whether to place support block for redstone torches.").defaultValue(true).build());
//    private final Setting<Boolean> crystalSupport = sgToggles.add(new BoolSetting.Builder().name("crystal-support").description("Whether to place support blocks for end crystals.").defaultValue(true).build());
//    private final Setting<Boolean> debugRender = sgToggles.add(new BoolSetting.Builder().name("render-place-positions").description("Whether to render placement positions.").defaultValue(true).build());
//    private final Setting<Boolean> pauseOnEat = sgToggles.add(new BoolSetting.Builder().name("pause-on-eat").description("Pauses while eating.").defaultValue(true).build());
//    private final Setting<Boolean> pauseOnDrink = sgToggles.add(new BoolSetting.Builder().name("pause-on-drink").description("Pauses while eating.").defaultValue(true).build());
//    private final Setting<Boolean> pauseOnMine = sgToggles.add(new BoolSetting.Builder().name("pause-on-mine").description("Pauses while eating.").defaultValue(false).build());
//    private final Setting<Boolean> debugPrint = sgToggles.add(new BoolSetting.Builder().name("debug-print").description("Pauses while eating.").defaultValue(true).build());
//
//    private PistonAura.Stage stage;
//   private Runnable postAction;
//   private PlayerEntity target;
//   private BlockPos facePos;
//   private Direction faceOffset;
//   private BlockPos crystalPos;
//   private EndCrystalEntity crystal;
//   private BlockPos pistonPos;
//   private BlockPos torchPos;
//   private BlockPos currentMining;
//   private boolean skipPiston;
//   private boolean canSupport;
//   private boolean canCrystalSupport;
//   private boolean hasRotated;
//   private boolean changePickItem;
//   private boolean mining;
//   private int miningTicks;
//   private int tickCounter;
//   private int delayAfterSwitch;
//
//   public PistonAura() {
//      super(OrcristAddon.Combat, "piston-aura", "Pushes end crystals into the enemy using pistons");
//   }
//
//   @Override
//   public void onActivate() {
//      if (Utils.canUpdate()) {
//         resetAll();
//      }
//   }
//
//   @Override
//   public void onDeactivate() {
//      resetAll();
//   }
//
//   @EventHandler
//   public void onRender3D(Render3DEvent event) {
//      if (debugRender.get()) {
//         if (facePos != null) {
//            event.renderer.box(facePos, Color.WHITE, Color.WHITE, ShapeMode.Lines, 0);
//         }
//
//         if (crystalPos != null) {
//            event.renderer.box(crystalPos, Color.WHITE, Color.RED, ShapeMode.Lines, 0);
//         }
//
//         if (pistonPos != null) {
//            event.renderer.box(pistonPos, Color.WHITE, new Color(0, 176, 255), ShapeMode.Lines, 0);
//         }
//
//         if (torchPos != null) {
//            event.renderer.box(torchPos, Color.WHITE, new Color(255, 72, 0), ShapeMode.Lines, 0);
//         }
//      }
//   }
//
//   @EventHandler
//   public void onTickPre(TickEvent.Pre event) {
//      if (disableWhenNone.get()) {
//
//          boolean redstoneSlot = InvUtils.findInHotbar(Items.REDSTONE_BLOCK, Items.REDSTONE_TORCH).found();
//         boolean pistonSlot = InvUtils.findInHotbar(Items.PISTON, Items.STICKY_PISTON).found();
//         boolean crystalSlot = InvUtils.findInHotbar(Items.END_CRYSTAL).found();
//
//         if (!redstoneSlot || !pistonSlot || !crystalSlot) {
//            info("Out of materials.");
//            toggle();
//            return;
//         }
//      }
//
//      if (!PlayerUtils.shouldPause(pauseOnMine.get(), pauseOnEat.get(), pauseOnDrink.get())) {
//         if (torchPos != null && crystalPos == null && !mining) {
//            mine(torchPos, false);
//         }
//
//         if (miningTicks < 10 && mining) {
//            ++miningTicks;
//         }
//
//         if (miningTicks >= 10) {
//            mine(currentMining, true);
//            miningTicks = 0;
//         }
//
//         if (!mining) {
//            if (tickCounter < actionInterval.get()) {
//               ++tickCounter;
//            }
//
//            if (tickCounter < actionInterval.get()) {
//               return;
//            }
//
//            if (postAction == null) {
//               handleAction();
//            }
//         }
//      }
//   }
//
//   @EventHandler
//   public void onTickPost(TickEvent.Post event) {
//      if (postAction != null && !mining) {
//         if (stage != PistonAura.Stage.SEARCHING || pistonPos == null || faceOffset == null) {
//            tickCounter = 0;
//            postAction.run();
//            postAction = null;
//            handleAction();
//         } else if (!hasRotated) {
//            float yaw = getRotationYaw(faceOffset.getOpposite());
//            Rotations.rotate(yaw, 0.0, () -> hasRotated = true);
//         } else {
//            tickCounter = 0;
//            postAction.run();
//            postAction = null;
//            handleAction();
//         }
//      } else {
//         if (torchPos != null && mc.world.getBlockState(torchPos).isAir()) {
//            mining = false;
//         }
//      }
//   }
//
//   @EventHandler
//   public void onReceivePacket(PacketEvent.Receive event) {
//      Packet deadEntity = event.packet;
//      if (deadEntity instanceof EntityStatusS2CPacket packet && target != null && packet.getStatus() == 3) {
//         Entity deadEntityx = packet.getEntity(mc.world);
//         if (deadEntityx instanceof PlayerEntity && target.equals(deadEntityx)) {
//            stage = PistonAura.Stage.SEARCHING;
//         }
//      }
//
//      deadEntity = event.packet;
//      if (deadEntity instanceof BlockUpdateS2CPacket packet
//         && torchPos != null
//         && packet.getPos().equals(torchPos)
//         && packet.getState().isAir()) {
//         miningTicks = 0;
//         currentMining = null;
//         mining = false;
//         torchPos = null;
//      }
//   }
//
//   @EventHandler
//   private void onSendPacket(PacketEvent.Send event) {
//      if (event.packet instanceof UpdateSelectedSlotC2SPacket) delayAfterSwitch = switchDelay.get();
//   }
//
//   private void handleAction() {
//       if (!strict.get() || !(mc.player.getVelocity().length() > 0.08)) {
//
//           int prevSlot = mc.player.getInventory().selectedSlot;
//
//           switch(stage) {
//            case SEARCHING:
//               for(PlayerEntity candidate : getTargets()) {
//                  if (evaluateTarget(candidate)) {
//                     if (debugPrint.get()) info("found target");
//
//                     target = candidate;
//                     if (skipPiston) {
//                        stage = PistonAura.Stage.CRYSTAL;
//                        skipPiston = false;
//                        return;
//                     }
//
//                     FindItemResult fir = InvUtils.findInHotbar(Items.PISTON, Items.STICKY_PISTON);
//
//                     if (!mine.get() && torchPos != null && mc.world.getBlockState(torchPos).emitsRedstonePower()) return;
//
//                     //int prevSlot = mc.player.getInventory().selectedSlot;
//                     boolean changeItem = prevSlot != fir.slot();
//
//                     if (changeItem) InvUtils.swap(fir.slot(), false);
//
//                     postAction = () -> {
//                        int yaw = getRotationYaw(faceOffset.getOpposite());
//                        Rotations.rotate(yaw, 0.0, () -> {
//                           BlockUtils.place(pistonPos, fir, false, 0, swing.get(), false);
//                           hasRotated = false;
//                        });
//                        stage = PistonAura.Stage.CRYSTAL;
//                     };
//                     return;
//                  }
//               }
//               break;
//            case CRYSTAL:
//               crystal = getCrystalAtPos(crystalPos);
//               if (crystal != null && pistonPos != null) {
//                  stage = PistonAura.Stage.REDSTONE;
//                  return;
//               }
//
//               if (!canPlaceCrystal(crystalPos.down(), canCrystalSupport)) {
//                  stage = PistonAura.Stage.SEARCHING;
//                  return;
//               }
//
//               FindItemResult fir = InvUtils.findInHotbar(Items.END_CRYSTAL);
//               FindItemResult crystalSupportM;
//               if (canCrystalSupport) crystalSupportM = InvUtils.findInHotbar(Items.OBSIDIAN);
//               else {
//                   crystalSupportM = null;
//               }
//
//               boolean changeItem = prevSlot != fir.slot();
//               if (changeItem) {
//                  InvUtils.swap(fir.slot(), false);
//               }
//
//               postAction = () -> {
//                  if (canCrystalSupport && crystalPos != null) {
//                    // assert crystalSupportM  != null;
//
//                     BlockUtils.place(crystalPos.down(), crystalSupportM, false, 0, swing.get(), false);
//                     canCrystalSupport = false;
//                  }
//
//                  BlockUtils.place(crystalPos, fir, false, 0, swing.get(), false);
//                  stage = PistonAura.Stage.REDSTONE;
//               };
//               break;
//
//            case REDSTONE:
//               if (facePos == null || torchPos == null || !mc.world.getBlockState(torchPos).isReplaceable()) {
//                  stage = PistonAura.Stage.SEARCHING;
//                  return;
//               }
//
//               if (canCrystalSupport && getBlock(crystalPos.down()) != Blocks.OBSIDIAN
//                  || getCrystalAtPos(crystalPos) == null) {
//                  stage = PistonAura.Stage.CRYSTAL;
//                  return;
//               }
//
//               FindItemResult RB = InvUtils.findInHotbar(Items.REDSTONE_BLOCK, Items.TORCH);
//               FindItemResult supportBlock;
//
//               if (canSupport) {
//                  supportBlock = InvUtils.findInHotbar(itemStack -> Block.getBlockFromItem(itemStack.getItem()).getBlastResistance() > 600.0F);
//               } else {
//                   supportBlock = null;
//               }
//
//                boolean changeST = prevSlot != RB.slot();
//
//               if (changeST) {
//                  InvUtils.swap(RB.slot(), false);
//               }
//
//               postAction = () -> {
//                  if (canSupport && torchPos != null) {
//                     assert supportBlock != null;
//
//                     BlockUtils.place(torchPos.down(), supportBlock, false, 0, swing.get(), false);
//                     canSupport = false;
//                  }
//
//                  BlockUtils.place(torchPos, RB, false, 0, swing.get(), false);
//                  stage = PistonAura.Stage.BREAKING;
//               };
//               break;
//            case BREAKING:
//               if (!delayCheck()) {
//                  return;
//               }
//
//               EndCrystalEntity crystalAtPos = getCrystalAtPos(crystalPos);
//               crystal = crystalAtPos == null ? getCrystalAtPos(facePos) : crystalAtPos;
//               if (crystal == null) {
//                  return;
//               }
//
//               if (crystalPos != null) {
//                  if (!(getBlock(pistonPos) instanceof PistonBlock)) {
//                     stage = PistonAura.Stage.SEARCHING;
//                  }
//
//                  if (crystal.age > blockingBreakDelay.get()) {
//                     stage = PistonAura.Stage.SEARCHING;
//                  }
//
//                  boolean blastResistantAtFace = getBlock(facePos).getBlastResistance() > 600.0F;
//                  double offsetForBlastResistant = blastResistantAtFace ? 0.0 : 0.5;
//                  double damage = damageCalc.get() == PistonAura.DamageCalc.PLACEPOS
//                     ? DamageCalculator.crystalDamage(
//                        target,
//                        new Vec3d(
//                           (double)facePos.getX() + 0.5,
//                           (double)facePos.getY() + offsetForBlastResistant,
//                           (double)facePos.getZ() + 0.5
//                        ),
//                        null,
//                        facePos
//                      , true
//                     )
//                     : DamageCalculator.crystalDamage(
//                        target, crystal.getPos().add(0.0, blastResistantAtFace ? -0.5 : 0.0, 0.0)
//                      , true
//                      , facePos
//                      , true
//                     );
//                  if (debugPrint.get()) {
//                     info("Damage: " + damage);
//                  }
//
//                  if (damage < minimalDamage.get() && !pistonHeadBlocking(pistonPos)) {
//                     return;
//                  }
//
//                  postAction = () -> {
//                     breakCrystal(crystal);
//                     if (mine.get() && torchPos != null && torchPos.equals(pistonPos.down())) {
//                        mine(torchPos, false);
//                     }
//
//                     resetStage();
//                  };
//               } else if (pistonPos != null && pistonHeadBlocking(pistonPos)
//                  || crystalPos != null && !mc.world.getBlockState(crystalPos).isReplaceable()) {
//                  postAction = () -> {
//                     if (mine.get() && torchPos != null && torchPos.equals(pistonPos.down())) {
//                        mine(torchPos, false);
//                     }
//
//                     resetStage();
//                  };
//               }
//         }
//      }
//   }
//
//   private boolean pistonHeadBlocking(BlockPos pos) {
//      for(Direction direction : Direction.Type.HORIZONTAL) {
//         if (getBlock(pos.offset(direction)) == Blocks.PISTON_HEAD) {
//            return true;
//         }
//      }
//
//      return false;
//   }
//
//   private boolean evaluateTarget(PlayerEntity candidate) {
//      BlockPos tempFacePos = new BlockPos((int) candidate.getPos().x, (int) candidate.getPos().y, (int) candidate.getPos().z).up();
//      if (evaluateTarget(tempFacePos, candidate)) {
//         return true;
//      } else {
//         return evaluateTarget(tempFacePos.up(), candidate) ? true : evaluateTarget(tempFacePos.up(2), candidate);
//      }
//   }
//
//   private boolean evaluateTarget(BlockPos tempFacePos, PlayerEntity candidate) {
//      BlockPos tempCrystalPos = null;
//      BlockPos tempPistonPos = null;
//      BlockPos tempTorchPos = null;
//      Direction offset = null;
//      List<EndCrystalEntity> crystalList = mc.world.getEntitiesByClass(EndCrystalEntity.class, new Box(tempFacePos).contract(0.2), Entity::isAlive);
//      EndCrystalEntity blockingCrystal = null;
//
//      for(EndCrystalEntity crystal : crystalList) {
//         if (crystal.age > blockingBreakDelay.get()) {
//            blockingCrystal = crystal;
//            break;
//         }
//      }
//
//      if (blockingCrystal != null) {
//         if (debugPrint.get()) {
//            info("breaking due to delay", new Object[0]);
//         }
//
//         if (delayCheck()) {
//            breakCrystal(blockingCrystal);
//         }
//
//         return false;
//      } else {
//         skipPiston = false;
//         canSupport = false;
//         canCrystalSupport = false;
//
//         for(Direction faceOffset : Direction.Type.HORIZONTAL) {
//            BlockPos potentialCrystal = tempFacePos.offset(faceOffset);
//            BlockState potCrystalState = mc.world.getBlockState(potentialCrystal);
//            if (!EntityUtils.intersectsWithEntity(new Box(potentialCrystal), Entity::isLiving)) {
//               FindItemResult supportBlock = InvUtils.findInHotbar(new Item[]{Items.OBSIDIAN});
//               if (!supportBlock.found() || !crystalSupport.get()) {
//                  continue;
//               }
//
//               if (potCrystalState.isReplaceable() || potCrystalState.isAir()) {
//                  canCrystalSupport = true;
//               }
//            }
//
//            if (canPlaceCrystal(potentialCrystal.down(), canCrystalSupport)) {
//               boolean blastResistantAtFace = getBlock(tempFacePos).getBlastResistance() > 600.0F;
//               double offsetForBlastResistant = blastResistantAtFace ? 0.0 : 0.5;
//               Vec3d calculatedCrystalPos = new Vec3d(
//                  (double)tempFacePos.getX() + 0.5,
//                  (double)tempFacePos.getY() + offsetForBlastResistant,
//                  (double)tempFacePos.getZ() + 0.5
//               );
//               float damage = (float) DamageCalculator.crystalDamage(candidate, calculatedCrystalPos, true, null, true);
//               if (!((double)damage < minimalDamage.get())) {
//                  if (antiSuicide.get()) {
//                     float selfDamage = (float) DamageCalculator.crystalDamage(mc.player, Vec3d.ofCenter(potentialCrystal));
//                     if (selfDamage >= EntityUtils.getTotalHealth(mc.player)) {
//                        continue;
//                     }
//                  }
//
//                  BlockPos potentialPiston = tempFacePos.offset(faceOffset, 2);
//                  BlockState pistonState = mc.world.getBlockState(potentialPiston);
//                  skipPiston = getBlock(potentialPiston) instanceof PistonBlock;
//                  if (!BlockHelper.outOfPlaceRange(potentialPiston, Origin.VANILLA, mc.player.getBlockInteractionRange())
//                     && (pistonState.isAir() || pistonState.isReplaceable() || skipPiston)
//                     && (!pistonState.isAir() || !EntityUtils.intersectsWithEntity(new Box(potentialPiston), Entity::isLiving))) {
//                     Item redstone = null;
//                     FindItemResult firT = InvUtils.findInHotbar(Items.REDSTONE_TORCH);
//                     FindItemResult firB = InvUtils.findInHotbar(Items.REDSTONE_BLOCK);
//                     if (firT.found() && firB.found()) {
//                        redstone = firT.slot() > firB.slot() ? Items.REDSTONE_BLOCK : Items.REDSTONE_TORCH;
//                     }
//
//                     if (firT.found() && !firB.found()) {
//                        redstone = Items.REDSTONE_TORCH;
//                     }
//
//                     BlockPos[] places = new BlockPos[mine.get() ? 2 : 1];
//                     places[0] = potentialPiston.offset(faceOffset);
//                     if (mine.get()) {
//                        places[1] = potentialPiston.offset(Direction.DOWN);
//                     }
//
//                     BlockPos[] var24 = places;
//                     int var25 = places.length;
//                     int var26 = 0;
//
//                     while(var26 < var25) {
//                        BlockPos potentialRedstone;
//                        label184: {
//                           potentialRedstone = var24[var26];
//                           if (!BlockHelper.outOfPlaceRange(potentialRedstone, Origin.VANILLA, mc.player.getBlockInteractionRange())) {
//                              BlockState state = mc.world.getBlockState(potentialRedstone);
//                              if ((
//                                    state.isAir()
//                                       || state.isReplaceable()
//                                          && state.emitsRedstonePower()
//                                          && !pistonState.isAir()
//                                          && mining
//                                          && potentialRedstone.equals(torchPos)
//                                 )
//                                 && !EntityUtils.intersectsWithEntity(new Box(potentialRedstone), Entity::isLiving)) {
//                                 if (potentialRedstone != places[0] || redstone == null || redstone != Items.REDSTONE_TORCH) {
//                                    break label184;
//                                 }
//
//                                 FindItemResult supportBlock = InvUtils.findInHotbar(
//                                    itemStack -> Block.getBlockFromItem(itemStack.getItem()).getBlastResistance() > 600.0F
//                                 );
//                                 if (supportBlock.found() && torchSupport.get()) {
//                                    BlockPos downPos = potentialRedstone.down();
//                                    if (!BlockHelper.outOfPlaceRange(downPos, Origin.VANILLA, mc.player.getBlockInteractionRange())) {
//                                       BlockState supportState = mc.world.getBlockState(downPos);
//                                       if ((supportState.isAir()
//                                           || supportState.isFullCube(mc.world, downPos)
//                                           && !(supportState.getBlock().getBlastResistance() <= 600.0F))
//                                           && !EntityUtils.intersectsWithEntity(new Box(downPos), Entity::isLiving)) {
//                                           canSupport = true;
//                                          break label184;
//                                       }
//                                    }
//                                 }
//                              }
//                           }
//
//                           ++var26;
//                           continue;
//                        }
//
//                        tempTorchPos = potentialRedstone;
//                        break;
//                     }
//
//                     if (tempTorchPos != null) {
//                        tempCrystalPos = potentialCrystal;
//                        tempPistonPos = potentialPiston;
//                        offset = faceOffset;
//                        break;
//                     }
//                  }
//               }
//            }
//         }
//
//         if (tempCrystalPos != null) {
//            faceOffset = offset;
//            facePos = tempFacePos;
//            crystalPos = tempCrystalPos;
//            crystal = getCrystalAtPos(crystalPos);
//            pistonPos = tempPistonPos;
//            torchPos = tempTorchPos;
//            return true;
//         } else {
//            return false;
//         }
//      }
//   }
//
//   private EndCrystalEntity getCrystalAtPos(BlockPos pos) {
//      Vec3d middlePos = Vec3d.ofCenter(pos);
//      List<EndCrystalEntity> crystalList = mc.world.getEntitiesByClass(EndCrystalEntity.class, new Box(pos).contract(0.5), Entity::isAlive);
//      if (crystalList.isEmpty()) {
//         return null;
//      } else if (crystalList.size() == 1) {
//         return crystalList.get(0);
//      } else {
//         EndCrystalEntity nearestCrystal = null;
//
//         for(EndCrystalEntity crystal : crystalList) {
//            if (nearestCrystal == null) {
//               nearestCrystal = crystal;
//            }
//
//            if (crystal.squaredDistanceTo(middlePos) < nearestCrystal.squaredDistanceTo(middlePos)) {
//               nearestCrystal = crystal;
//            }
//         }
//
//         return nearestCrystal;
//      }
//   }
//
//   private boolean canPlaceCrystal(BlockPos blockPos, boolean support) {
//      BlockState blockState = mc.world.getBlockState(blockPos);
//      BlockPos blockPosUp = blockPos.up();
//      if (!blockState.isOf(Blocks.BEDROCK) && !blockState.isOf(Blocks.OBSIDIAN) && !support) {
//         return false;
//      } else if (!mc.world.getBlockState(blockPosUp).isAir()) {
//         return false;
//      } else {
//         int x = blockPosUp.getX();
//         int y = blockPosUp.getY();
//         int z = blockPosUp.getZ();
//         return
//             mc.world
//            .getOtherEntities(null, new Box(x, y, z, x + 1, y + 2, z + 1))
//            .isEmpty();
//      }
//   }
//
//   private List<PlayerEntity> getTargets() {
//      List<PlayerEntity> players = mc
//         .world
//         .getEntitiesByClass(
//            PlayerEntity.class,
//            new Box(mc.player.getBlockPos()).expand((double)((Integer)targetRange.get()).intValue()),
//            Predicate.not(PlayerEntity::isMainPlayer)
//         );
//      return players.isEmpty()
//         ? players
//         : players.stream()
//            .filter(LivingEntity::isAlive)
//            .filter(playerEntity -> Friends.get().shouldAttack(playerEntity))
//            .sorted(Comparator.comparing(e -> mc.player.distanceTo(e)))
//            .collect(Collectors.toList());
//   }
//
//   private Block getBlock(BlockPos bp) {
//      return mc.world.getBlockState(bp).getBlock();
//   }
//
//   private void breakCrystal(Entity crystal) {
//      if (crystal != null) {
//         Hand hand = mc.player.getOffHandStack().getItem() == Items.END_CRYSTAL ? Hand.OFF_HAND : Hand.MAIN_HAND;
//         mc.player.networkHandler.sendPacket(PlayerInteractEntityC2SPacket.attack(crystal, mc.player.isSneaking()));
//         if (swing.get()) {
//            mc.player.swingHand(hand);
//         } else {
//            mc.player.networkHandler.sendPacket(new HandSwingC2SPacket(hand));
//         }
//      }
//   }
//
//   private boolean delayCheck() {
//      if (delayAfterSwitch > 0) {
//         --delayAfterSwitch;
//         return false;
//      } else {
//         return true;
//      }
//   }
//
//   private int getRotationYaw(Direction dir) {
//      return switch(dir) {
//         case EAST -> 90;
//         case SOUTH -> 180;
//         case WEST -> -90;
//         default -> 0;
//      };
//   }
//
//   private void mine(BlockPos blockPos, boolean override) {
//      if (blockPos != null) {
//         BlockState state = mc.world.getBlockState(blockPos);
//         if (!mining && getBlock(blockPos).getHardness() >= 0.0F && !state.isAir() || override) {
//            FindItemResult pickaxe = InvUtils.findInHotbar(new Item[]{Items.DIAMOND_PICKAXE, Items.NETHERITE_PICKAXE});
//            int pickPrevSlot = mc.player.getInventory().selectedSlot;
//            changePickItem = pickaxe.slot() != pickPrevSlot;
//            if (changePickItem) {
//               InvUtils.swap(pickaxe.slot(), false);
//            }
//
//            mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, blockPos, Direction.WEST));
//            mc.player.swingHand(Hand.MAIN_HAND);
//            mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, blockPos, Direction.WEST));
//            mining = true;
//            currentMining = blockPos;
//            if (mc.interactionManager.getCurrentGameMode().isCreative() && blockPos.equals(torchPos)) {
//               mining = false;
//               torchPos = null;
//            }
//         }
//      }
//   }
//
//   private void resetAll() {
//      stage = PistonAura.Stage.SEARCHING;
//      postAction = null;
//      target = null;
//      facePos = null;
//      faceOffset = null;
//      crystalPos = null;
//      crystal = null;
//      skipPiston = false;
//      pistonPos = null;
//      torchPos = null;
//      hasRotated = false;
//      changePickItem = false;
//      mining = false;
//      canSupport = false;
//      canCrystalSupport = false;
//      currentMining = null;
//      miningTicks = 0;
//      tickCounter = 0;
//      delayAfterSwitch = 0;
//   }
//
//   private void resetStage() {
//      faceOffset = null;
//      facePos = null;
//      crystalPos = null;
//      pistonPos = null;
//      target = null;
//      stage = PistonAura.Stage.SEARCHING;
//   }
//
//   public enum DamageCalc {
//      PLACEPOS,
//      CRYSTALPOS;
//   }
//
//   private enum Stage {
//      SEARCHING,
//      CRYSTAL,
//      REDSTONE,
//      BREAKING;
//   }
//}
