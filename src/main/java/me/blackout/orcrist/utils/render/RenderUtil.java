package me.blackout.orcrist.utils.render;

import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.utils.PostInit;
import meteordevelopment.meteorclient.utils.misc.Pool;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class RenderUtil {
    public static Vec3d center;

    private static final Pool<RenderBlock> renderBlockPool = new Pool<>(RenderBlock::new);
    private static final List<RenderBlock> renderBlocks = new ArrayList<>();

    @PostInit
    public static void init() {
        MeteorClient.EVENT_BUS.subscribe(RenderUtil.class);
    }

    public static void renderTickingBlockGRD(BlockPos blockPos, Color sideBottom, Color sideTop, Color lineTop, Color lineBottom, ShapeMode mode, int excludeDir, int duration, boolean fade) {
        // Ensure there aren't multiple fading blocks in one pos
        Iterator<RenderBlock> iterator = renderBlocks.iterator();
        while (iterator.hasNext()) {
            RenderBlock next = iterator.next();
            if (next.pos.equals(blockPos)) {
                iterator.remove();
                renderBlockPool.free(next);
            }
        }
        renderBlocks.add(renderBlockPool.get().set(blockPos, sideBottom, sideTop , lineTop, lineBottom, mode, excludeDir, duration, fade, true));
    }

    public static void renderTickingBlock(BlockPos blockPos, Color sideColor, Color lineColor, ShapeMode shapeMode, int excludeDir, int duration, double heightamt, boolean fade, boolean shrink, boolean height) {
        // Ensure there aren't multiple fading blocks in one pos
        Iterator<RenderBlock> iterator = renderBlocks.iterator();
        while (iterator.hasNext()) {
            RenderBlock next = iterator.next();
            if (next.pos.equals(blockPos)) {
                iterator.remove();
                renderBlockPool.free(next);
            }
        }
        renderBlocks.add(renderBlockPool.get().set(blockPos, sideColor, lineColor, shapeMode, excludeDir, duration, heightamt, fade, shrink, height));
    }

    @EventHandler
    private static void onTick(TickEvent.Pre event) {
        if (renderBlocks.isEmpty()) return;

        renderBlocks.forEach(RenderBlock::tick);

        Iterator<RenderBlock> iterator = renderBlocks.iterator();
        while (iterator.hasNext()) {
            RenderBlock next = iterator.next();
            if (next.ticks <= 0) {
                iterator.remove();
                renderBlockPool.free(next);
            }
        }
    }

    @EventHandler
    private static void onRender(Render3DEvent event) {
        renderBlocks.forEach(block -> block.render(event));
    }

    public static class RenderBlock {
        public BlockPos.Mutable pos = new BlockPos.Mutable();

        public Color sideColor, lineColor;
        public Color sideBottom, sideTop, lineBottom, lineTop;
        public ShapeMode shapeMode;
        public int excludeDir;

        public double heightAmt;
        public int ticks, duration;
        public boolean fade, shrink, gradient, height;

        public RenderBlock set(BlockPos blockPos, Color sideColor, Color lineColor, ShapeMode shapeMode, int excludeDir, int duration, double heightAmt, boolean fade, boolean shrink, boolean height) {
            pos.set(blockPos);
            this.sideColor = sideColor;
            this.lineColor = lineColor;
            this.shapeMode = shapeMode;
            this.excludeDir = excludeDir;
            this.heightAmt = heightAmt;
            this.height = height;
            this.fade = fade;
            this.shrink = shrink;
            this.ticks = duration;
            this.duration = duration;

            return this;
        }

        public RenderBlock set(BlockPos blockPos, Color sideBottom, Color sideTop, Color lineTop, Color lineBottom, ShapeMode shapeMode, int excludeDir, int duration, boolean fade, boolean gradient) {
            pos.set(blockPos);
            this.sideBottom = sideBottom;
            this.sideTop = sideTop;
            this.lineTop = lineTop;
            this.lineBottom = lineBottom;
            this.shapeMode = shapeMode;
            this.excludeDir = excludeDir;
            this.gradient = gradient;
            this.fade = fade;
            this.ticks = duration;
            this.duration = duration;

            return this;
        }

        public void tick() {
            ticks--;
        }

        public void render(Render3DEvent event) {
            int preSideA = sideColor.a;
            int preLineA = lineColor.a;
            double x1 = pos.getX(), y1 = pos.getY(), z1 = pos.getZ(),
                   x2 = pos.getX() + 1, y2 = pos.getY() + 1, z2 = pos.getZ() + 1;

            double d = (double) (ticks - event.tickDelta) / duration;

            if (fade) {
                sideColor.a = (int) (sideColor.a * d);
                lineColor.a = (int) (lineColor.a * d);
            }

            if (shrink) {
                x1 += d; y1 += d; z1 += d;
                x2 -= d; y2 -= d; z2 -= d;
            }

            if (height) event.renderer.box(x1 + 1, y1 + 1, z1 + 1, x1, y1 + heightAmt, z1, sideColor, lineColor, shapeMode, 0);

            if (gradient) {
                if (shapeMode.sides()) {
                    event.renderer.gradientQuadVertical(x1, y1, z1, x1 + 1, y1 + 1, z1, sideTop, sideBottom);
                    event.renderer.gradientQuadVertical(x1, y1, z1, x1, y1 + 1, z1 + 1, sideTop, sideBottom);
                    event.renderer.gradientQuadVertical(x1 + 1, y1, z1 + 1, x1 + 1, y1 + 1, z1, sideTop, sideBottom);
                    event.renderer.gradientQuadVertical(x1 + 1, y1, z1 + 1, x1, y1 + 1, z1 + 1, sideTop, sideBottom);
                    event.renderer.quadHorizontal(x1, y1 + 1, z1, x1 + 1, z1 + 1, sideTop);
                    event.renderer.quadHorizontal(x1, y1, z1, x1 + 1, z1 + 1, sideBottom);
                }

                if (shapeMode.lines()) {
                    //sides
                    event.renderer.gradientQuadVertical(x1, y1, z1, x1, y1 + 1, z1 + 0.02, lineTop, lineBottom);
                    event.renderer.gradientQuadVertical(x1, y1, z1, x1 + 0.02, y1 + 1, z1, lineTop, lineBottom);
                    event.renderer.gradientQuadVertical(x1 + 1, y1, z1, x1 + 1, y1 + 1, z1 + 0.02, lineTop, lineBottom);
                    event.renderer.gradientQuadVertical(x1 + 1, y1, z1, x1 + 0.98, y1 + 1, z1, lineTop, lineBottom);
                    event.renderer.gradientQuadVertical(x1, y1, z1 + 1, x1, y1 + 1, z1 + 0.98, lineTop, lineBottom);
                    event.renderer.gradientQuadVertical(x1, y1, z1 + 1, x1 + 0.02, y1 + 1, z1 + 1, lineTop, lineBottom);
                    event.renderer.gradientQuadVertical(x1 + 1, y1, z1 + 1, x1 + 1, y1 + 1, z1 + 0.98, lineTop, lineBottom);
                    event.renderer.gradientQuadVertical(x1 + 1, y1, z1 + 1, x1 + 0.98, y1 + 1, z1 + 1, lineTop, lineBottom);

                    //up
                    event.renderer.gradientQuadVertical(x1, y1 + 1, z1, x1 + 1, y1 + 0.98, z1, lineTop, lineTop);
                    event.renderer.quadHorizontal(x1, y1 + 1, z1, x1 + 1, z1 + 0.02, lineTop);
                    event.renderer.gradientQuadVertical(x1, y1 + 1, z1, x1, y1 + 0.98, z1 + 1, lineTop, lineTop);
                    event.renderer.quadHorizontal(x1, y1 + 1, z1, x1 + 0.02, z1 + 1, lineTop);
                    event.renderer.gradientQuadVertical(x1, y1 + 1, z1 + 1, x1 + 1, y1 + 0.98, z1 + 1, lineTop, lineTop);
                    event.renderer.quadHorizontal(x1, y1 + 1, z1 + 1, x1 + 1, z1 + 0.98, lineTop);
                    event.renderer.gradientQuadVertical(x1 + 1, y1 + 1, z1, x1 + 1, y1 + 0.98, z1 + 1, lineTop, lineTop);
                    event.renderer.quadHorizontal(x1 + 1, y1 + 1, z1, x1 + 0.98, z1 + 1, lineTop);

                    //down
                    event.renderer.gradientQuadVertical(x1, y1, z1, x1 + 1, y1 + 0.02, z1, lineBottom, lineBottom);
                    event.renderer.quadHorizontal(x1, y1, z1, x1 + 1, z1 + 0.02, lineBottom);
                    event.renderer.gradientQuadVertical(x1, y1, z1, x1, y1 + 0.02, z1 + 1, lineBottom, lineBottom);
                    event.renderer.quadHorizontal(x1, y1, z1, x1 + 0.02, z1 + 1, lineBottom);
                    event.renderer.gradientQuadVertical(x1, y1, z1 + 1, x1 + 1, y1 + 0.02, z1 + 1, lineBottom, lineBottom);
                    event.renderer.quadHorizontal(x1, y1, z1 + 1, x1 + 1, z1 + 0.98, lineBottom);
                    event.renderer.gradientQuadVertical(x1 + 1, y1, z1, x1 + 1, y1 + 0.02, z1 + 1, lineBottom, lineBottom);
                    event.renderer.quadHorizontal(x1 + 1, y1, z1, x1 + 0.98, z1 + 1, lineBottom);
                }
            }

            if (!gradient && !height) event.renderer.box(x1, y1, z1, x2, y2, z2, sideColor, lineColor, shapeMode, excludeDir);

            sideColor.a = preSideA;
            lineColor.a = preLineA;
        }
    }
}

