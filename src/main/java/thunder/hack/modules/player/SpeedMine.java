package thunder.hack.modules.player;

import com.google.common.eventbus.Subscribe;
import net.minecraft.block.AirBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayPongC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.s2c.query.QueryPongS2CPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import thunder.hack.core.PlayerManager;
import thunder.hack.events.impl.EventAttackBlock;
import thunder.hack.events.impl.EventSync;
import thunder.hack.events.impl.PacketEvent;
import thunder.hack.events.impl.Render3DEvent;
import thunder.hack.injection.accesors.IInteractionManager;
import thunder.hack.modules.Module;
import thunder.hack.setting.Setting;
import net.minecraft.item.AxeItem;
import net.minecraft.item.PickaxeItem;
import net.minecraft.item.ShovelItem;
import thunder.hack.utility.math.MathUtil;
import thunder.hack.utility.render.Render3DEngine;

import java.awt.*;

public class SpeedMine extends Module {
    public SpeedMine() {
        super("SpeedMine", "SpeedMine", Category.PLAYER);
    }
    public static final Setting<Mode> mode = new Setting("Mode", Mode.Packet);
    public final Setting<Float> startDmg = new Setting("StartDmg", 0f, 0f, 1f);
    public final Setting<Float> finishDmg = new Setting("FinishDmg", 1f, 0f, 1f);

    private final Setting<Float> range = new Setting<>("Range", 4.2f, 3.0f, 10.0f);
    public Setting<Boolean> rotate = new Setting<>("Rotate", false);
    public Setting<Boolean> render = new Setting<>("Render", false);

    public Setting<Integer> breakAttemps = new Setting("BreakAttemps", 10, 1, 50, v -> mode.getValue() == Mode.Packet);


    public static BlockPos minePosition;
    private Direction mineFacing;
    private int mineBreaks;
    public static float progress;


    @Override
    public void onUpdate() {
        if (!mc.player.getAbilities().creativeMode) {
            if (minePosition != null) {
                if (mineBreaks >= breakAttemps.getValue() || mc.player.squaredDistanceTo(minePosition.toCenterPos()) > range.getPow2Value()) {
                    minePosition = null;
                    mineFacing = null;
                    progress = 0;
                    mineBreaks = 0;
                }
            }
            if (mode.getValue() == Mode.Damage) {
                if (((IInteractionManager) mc.interactionManager).getCurBlockDamageMP() < startDmg.getValue())
                    ((IInteractionManager) mc.interactionManager).setCurBlockDamageMP(1f);
                if (((IInteractionManager) mc.interactionManager).getCurBlockDamageMP() >= finishDmg.getValue())
                    ((IInteractionManager) mc.interactionManager).setCurBlockDamageMP(1f);
            } else if (mode.getValue() == Mode.Packet) {
                if (minePosition != null && !mc.world.isAir(minePosition)) {
                    int swapSlot = getTool(minePosition);
                    if (swapSlot == -1) return;
                  /*
                    if(progress == 0){
                        if(swapSlot < 9){
                            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 30, swapSlot, SlotActionType.SWAP, mc.player);
                            mc.player.networkHandler.sendPacket(new CloseHandledScreenC2SPacket(mc.player.currentScreenHandler.syncId));
                        }
                        mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, swapSlot, mc.player.getInventory().selectedSlot, SlotActionType.SWAP, mc.player);
                        mc.player.networkHandler.sendPacket(new CloseHandledScreenC2SPacket(mc.player.currentScreenHandler.syncId));
                        mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, swapSlot, mc.player.getInventory().selectedSlot, SlotActionType.SWAP, mc.player);
                        mc.player.networkHandler.sendPacket(new CloseHandledScreenC2SPacket(mc.player.currentScreenHandler.syncId));
                    }

                     */
                    if (progress >= 1) {
                        if(swapSlot < 9){
                            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 30, swapSlot, SlotActionType.SWAP, mc.player);
                            mc.player.networkHandler.sendPacket(new CloseHandledScreenC2SPacket(mc.player.currentScreenHandler.syncId));
                        }
                        mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 30, mc.player.getInventory().selectedSlot, SlotActionType.SWAP, mc.player);
                        mc.player.networkHandler.sendPacket(new CloseHandledScreenC2SPacket(mc.player.currentScreenHandler.syncId));

                        mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, minePosition, mineFacing));
                        mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, minePosition, Direction.UP));
                        mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, minePosition, mineFacing));
                        mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, minePosition, mineFacing));

                        mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 30, mc.player.getInventory().selectedSlot, SlotActionType.SWAP, mc.player);
                        mc.player.networkHandler.sendPacket(new CloseHandledScreenC2SPacket(mc.player.currentScreenHandler.syncId));

                        mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 30, swapSlot, SlotActionType.SWAP, mc.player);
                        mc.player.networkHandler.sendPacket(new CloseHandledScreenC2SPacket(mc.player.currentScreenHandler.syncId));

                        progress = 0;
                        mineBreaks++;
                    }
                    progress += getBlockStrength(mc.world.getBlockState(minePosition), minePosition);
                } else {
                    progress = 0;
                }
            }
        }
    }

    public float getBlockStrength(BlockState state, BlockPos position) {
        float hardness = state.getHardness(mc.world, position);
        if (hardness < 0) {
            return 0;
        }
        if (!canBreak(position)) {
            return getDigSpeed(state,position) / hardness / 100F;
        } else {
            return getDigSpeed(state,position) / hardness / 30F;
        }
    }

    public float getDestroySpeed(BlockPos position,BlockState state) {
        float destroySpeed = 1;
        if (mc.player.getInventory().getStack(getTool(position)) != null && !mc.player.getInventory().getStack(getTool(position)).isEmpty()) {
            destroySpeed *= mc.player.getInventory().getStack(getTool(position)).getMiningSpeedMultiplier(state);
        }
        return destroySpeed;
    }

    public float getDigSpeed(BlockState state, BlockPos position) {
        float digSpeed = getDestroySpeed(position,state);
        if (digSpeed > 1) {
            ItemStack itemstack = mc.player.getInventory().getStack(getTool(position));
            int efficiencyModifier = EnchantmentHelper.getLevel(Enchantments.EFFICIENCY, itemstack);
            if (efficiencyModifier > 0 && !itemstack.isEmpty()) {
                digSpeed += StrictMath.pow(efficiencyModifier, 2) + 1;
            }
        }
        if (mc.player.hasStatusEffect(StatusEffects.HASTE)) {
            digSpeed *= 1 + (mc.player.getStatusEffect(StatusEffects.HASTE).getAmplifier() + 1) * 0.2F;
        }
        if (mc.player.hasStatusEffect(StatusEffects.MINING_FATIGUE)) {
            float fatigueScale;
            switch (mc.player.getStatusEffect(StatusEffects.MINING_FATIGUE).getAmplifier()) {
                case 0:
                    fatigueScale = 0.3F;
                    break;
                case 1:
                    fatigueScale = 0.09F;
                    break;
                case 2:
                    fatigueScale = 0.0027F;
                    break;
                case 3:
                default:
                    fatigueScale = 8.1E-4F;
            }

            digSpeed *= fatigueScale;
        }
        if (mc.player.isSubmergedInWater() && !EnchantmentHelper.hasAquaAffinity(mc.player)) {
            digSpeed /= 5;
        }
        if (!mc.player.isOnGround()) {
            digSpeed /= 5;
        }
        return (digSpeed < 0 ? 0 : digSpeed);
    }

    @Subscribe
    public void onPacketSend(PacketEvent.SendPost e){
        if(e.getPacket() instanceof UpdateSelectedSlotC2SPacket){
            progress = 0;
        }
    }

    @Override
    public void onDisable() {
        minePosition = null;
        mineFacing = null;
        progress = 0;
        mineBreaks = 0;
    }

    @Subscribe
    public void onRender3D(Render3DEvent e) {
        if (mode.getValue() == Mode.Packet) {
            if (minePosition != null && !mc.world.isAir(minePosition)) {
                Box shrunkMineBox = new Box(minePosition.getX(),minePosition.getY(), minePosition.getZ(), minePosition.getX(),minePosition.getY(), minePosition.getZ());
                float noom = MathUtil.clamp(progress, 0f, 1f);

                Render3DEngine.drawFilledBox(
                        e.getMatrixStack(),
                        shrunkMineBox.shrink(noom,noom,noom).offset(0.5 + noom * 0.5,0.5 + noom * 0.5,0.5 + noom * 0.5),
                        progress >= 0.95 ? new Color(47, 255, 0, 120) : new Color(255, 0, 0, 120)
                );

                Render3DEngine.drawBoxOutline(
                        shrunkMineBox.shrink(noom,noom,noom).offset(0.5 + noom * 0.5,0.5 + noom * 0.5,0.5 + noom * 0.5),
                        progress >= 0.95 ? new Color(47, 255, 0, 200) : new Color(255, 0, 0, 200),
                        2f
                );
            }
        }
    }

    @Subscribe
    public void onAttackBlock(EventAttackBlock event) {
        if (canBreak(event.getBlockPos()) && !mc.player.getAbilities().creativeMode) {
            if (mode.getValue() == Mode.Packet) {
                if (!event.getBlockPos().equals(minePosition)) {
                    minePosition = event.getBlockPos();
                    mineFacing = event.getEnumFacing();
                    progress = 0;
                    mineBreaks = 0;
                    if (minePosition != null && mineFacing != null) {
                        mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, minePosition, mineFacing));
                        mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, minePosition, mineFacing));
                    }
                }
            }
        }
    }

    @Subscribe
    public void onEntitySync(EventSync event) {
        if (rotate.getValue()) {
            if (progress > 0.95) {
                if (minePosition != null) {
                    float[] angle = PlayerManager.calcAngle(mc.player.getEyePos(), minePosition.toCenterPos());
                    mc.player.setYaw(angle[0]);
                    mc.player.setPitch(angle[1]);
                }
            }
        }
    }

    private int getTool(final BlockPos pos) {
        int index = -1;
        float CurrentFastest = 1.0f;
        if (mc.world.getBlockState(pos).getBlock() instanceof AirBlock) return -1;

        for (int i = 9; i < 45; ++i) {
            final ItemStack stack = mc.player.getInventory().getStack(i >= 36 ? i - 36 : i);
            if (stack != ItemStack.EMPTY) {
                if (!(mc.player.getInventory().getStack(i >= 36 ? i - 36 : i).getMaxDamage() - mc.player.getInventory().getStack(i >= 36 ? i - 36 : i).getDamage() > 10))
                    continue;
                final float digSpeed = EnchantmentHelper.getLevel(Enchantments.EFFICIENCY, stack);
                final float destroySpeed = stack.getMiningSpeedMultiplier(mc.world.getBlockState(pos));
                if (digSpeed + destroySpeed > CurrentFastest) {
                    CurrentFastest = digSpeed + destroySpeed;
                    index = i;
                }
            }
        }
        return index >= 36 ? index - 36 : index;
    }

    private boolean canBreak(BlockPos pos) {
        final BlockState blockState = mc.world.getBlockState(pos);
        final Block block = blockState.getBlock();
        return block.getHardness() != -1;
    }

    public enum Mode {
        Packet, Damage
    }
}