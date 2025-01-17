package thunder.hack.modules.misc;

import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import thunder.hack.core.ModuleManager;
import thunder.hack.events.impl.EventAttackBlock;
import thunder.hack.events.impl.EventSync;
import thunder.hack.modules.Module;
import thunder.hack.modules.client.HudEditor;
import thunder.hack.modules.client.MainSettings;
import thunder.hack.modules.player.SpeedMine;
import thunder.hack.setting.Setting;
import thunder.hack.setting.impl.ColorSetting;
import thunder.hack.utility.player.InteractionUtility;
import thunder.hack.utility.render.Render2DEngine;
import thunder.hack.utility.render.Render3DEngine;

import java.awt.*;

public class Nuker extends Module {
    public Nuker() {
        super("Nuker", Category.MISC);
    }

    private Block nukerTargetBlock;
    private BlockPosWithRotation nukerTargetBlockpos;
    private final Setting<Blocks> blocks = new Setting<>("Blocks", Blocks.Select);
    private Setting<Boolean> flatten = new Setting<>("Flatten", false);
    private Setting<Float> range = new Setting<>("Range", 4.2f, 0f, 5f);
    private final Setting<Mode> colorMode = new Setting<>("ColorMode", Mode.Sync);
    public final Setting<ColorSetting> color = new Setting<>("Color", new ColorSetting(0x2250b4b4), v -> colorMode.getValue() == Mode.Custom);

    private enum Mode {
        Custom, Sync
    }

    private enum Blocks {
        Select, All
    }

    @EventHandler
    public void onBlockInteract(EventAttackBlock e) {
        if (mc.world.isAir(e.getBlockPos())) return;
        if (blocks.getValue().equals(Blocks.Select) && nukerTargetBlock != mc.world.getBlockState(e.getBlockPos()).getBlock()) {
            nukerTargetBlock = mc.world.getBlockState(e.getBlockPos()).getBlock();
            sendMessage(MainSettings.isRu() ? "Выбран блок: " + Formatting.AQUA + nukerTargetBlock.getName().getString() : "Selected block: " + Formatting.AQUA + nukerTargetBlock.getName().getString());
        }
    }

    @EventHandler
    public void onSync(EventSync e) {
        if (nukerTargetBlockpos != null) {
            if ((mc.world.getBlockState(nukerTargetBlockpos.bp).getBlock() != nukerTargetBlock && blocks.getValue().equals(Blocks.Select)) || mc.player.squaredDistanceTo(nukerTargetBlockpos.bp.toCenterPos()) > range.getPow2Value() || mc.world.isAir(nukerTargetBlockpos.bp))
                nukerTargetBlockpos = null;
        }

        if (nukerTargetBlockpos == null || mc.options.attackKey.isPressed()) return;

        float[] angle = InteractionUtility.calculateAngle(nukerTargetBlockpos.vec3d);
        mc.player.setYaw(angle[0]);
        mc.player.setPitch(angle[1]);

        if (ModuleManager.speedMine.isEnabled()) {
            if (SpeedMine.minePosition != nukerTargetBlockpos.bp) {
                mc.interactionManager.attackBlock(nukerTargetBlockpos.bp, nukerTargetBlockpos.dir);
                mc.player.swingHand(Hand.MAIN_HAND);
            }
        } else {
            mc.interactionManager.updateBlockBreakingProgress(nukerTargetBlockpos.bp, nukerTargetBlockpos.dir);
            mc.player.swingHand(Hand.MAIN_HAND);
        }
    }

    public void onRender3D(MatrixStack stack) {
        if (nukerTargetBlockpos != null && nukerTargetBlockpos.bp != null) {
            Color color1 = colorMode.getValue() == Mode.Sync ? HudEditor.getColor(1) : color.getValue().getColorObject();
            Render3DEngine.drawBoxOutline(new Box(nukerTargetBlockpos.bp), color1, 2);
            Render3DEngine.drawFilledBox(stack, new Box(nukerTargetBlockpos.bp), Render2DEngine.injectAlpha(color1, 100));
        }
    }

    @Override
    public void onThread() {
        if ((nukerTargetBlock != null || blocks.getValue().equals(Blocks.All)) && !mc.options.attackKey.isPressed() && nukerTargetBlockpos == null) {
            nukerTargetBlockpos = getNukerBlockPos();
        }
    }

    public BlockPosWithRotation getNukerBlockPos() {

        int startY = flatten.getValue() ? (int) mc.player.getY() : (int) (mc.player.getY() - (range.getValue() + 1));

        for (int x = (int) (mc.player.getX() - (range.getValue() + 1)); x < mc.player.getX() + (range.getValue() + 1); x++)
            for (int y = startY; y < mc.player.getY() + (range.getValue() + 1); y++)
                for (int z = (int) (mc.player.getZ() - (range.getValue() + 1)); z < mc.player.getZ() + (range.getValue() + 1); z++) {
                    BlockPos bp = BlockPos.ofFloored(x, y, z);
                    if (mc.player.squaredDistanceTo(bp.toCenterPos()) > range.getPow2Value()) continue;
                    if (mc.world.getBlockState(bp).getBlock() == nukerTargetBlock || blocks.getValue().equals(Blocks.All)) {
                        for (float x1 = 0f; x1 <= 1f; x1 += 0.05f) {
                            for (float y1 = 0f; y1 <= 1; y1 += 0.05f) {
                                for (float z1 = 0f; z1 <= 1; z1 += 0.05f) {
                                    Vec3d p = new Vec3d(bp.getX() + x1, bp.getY() + y1, bp.getZ() + z1);
                                    BlockHitResult bhr = mc.world.raycast(new RaycastContext(InteractionUtility.getEyesPos(mc.player), p, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, mc.player));
                                    if (bhr != null && bhr.getType() == HitResult.Type.BLOCK && bhr.getBlockPos().equals(bp)) {
                                        return new BlockPosWithRotation(bp, p, bhr.getSide());
                                    }
                                }
                            }
                        }
                    }
                }
        return null;
    }

    public record BlockPosWithRotation(BlockPos bp, Vec3d vec3d, Direction dir) {
    }
}
