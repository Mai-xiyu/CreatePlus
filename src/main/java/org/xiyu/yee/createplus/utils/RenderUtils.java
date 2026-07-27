package org.xiyu.yee.createplus.utils;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.List;

public final class RenderUtils {

    private RenderUtils() {
    }

    private static Font font() {
        return Minecraft.getInstance().font;
    }

    // ==================================================================
    // 颜色工具
    // ==================================================================

    /** 把颜色的 alpha 乘上一个系数，用于统一控制 HUD 透明度。 */
    public static int withAlpha(int argb, float multiplier) {
        int alpha = (argb >>> 24) & 0xFF;
        int scaled = (int) Math.max(0, Math.min(255, alpha * multiplier));
        return (scaled << 24) | (argb & 0x00FFFFFF);
    }

    /** 用给定的绝对 alpha（0~255）替换颜色的 alpha 通道。 */
    public static int setAlpha(int argb, int alpha) {
        return ((alpha & 0xFF) << 24) | (argb & 0x00FFFFFF);
    }

    /** 在两个 ARGB 颜色之间线性插值。 */
    public static int lerpColor(int from, int to, float t) {
        float f = Math.max(0.0F, Math.min(1.0F, t));
        int a = (int) (((from >>> 24) & 0xFF) + (((to >>> 24) & 0xFF) - ((from >>> 24) & 0xFF)) * f);
        int r = (int) (((from >> 16) & 0xFF) + (((to >> 16) & 0xFF) - ((from >> 16) & 0xFF)) * f);
        int g = (int) (((from >> 8) & 0xFF) + (((to >> 8) & 0xFF) - ((from >> 8) & 0xFF)) * f);
        int b = (int) ((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * f);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    // ==================================================================
    // 2D / GUI
    // ==================================================================

    public static void drawRect(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        graphics.fill(x, y, x + width, y + height, color);
    }

    /**
     * 真正的圆角矩形。
     *
     * 逐行按圆方程计算内缩量，比以前那个"用三个矩形拼一下"的占位实现要像样得多。
     */
    public static void drawRoundedRect(GuiGraphics graphics, int x, int y, int width, int height,
                                       int radius, int color) {
        if (width <= 0 || height <= 0) return;

        int r = Math.min(radius, Math.min(width, height) / 2);
        if (r <= 0) {
            drawRect(graphics, x, y, width, height, color);
            return;
        }

        graphics.fill(x, y + r, x + width, y + height - r, color);

        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5D;
            int inset = r - (int) Math.round(Math.sqrt((double) r * r - dy * dy));
            graphics.fill(x + inset, y + i, x + width - inset, y + i + 1, color);
            graphics.fill(x + inset, y + height - i - 1, x + width - inset, y + height - i, color);
        }
    }

    public static void drawBorder(GuiGraphics graphics, int x, int y, int width, int height,
                                  int thickness, int color) {
        drawRect(graphics, x, y, width, thickness, color);
        drawRect(graphics, x, y + height - thickness, width, thickness, color);
        drawRect(graphics, x, y + thickness, thickness, height - thickness * 2, color);
        drawRect(graphics, x + width - thickness, y + thickness, thickness, height - thickness * 2, color);
    }

    /** 竖直方向渐变。 */
    public static void drawGradientRect(GuiGraphics graphics, int x, int y, int width, int height,
                                        int topColor, int bottomColor) {
        graphics.fillGradient(x, y, x + width, y + height, topColor, bottomColor);
    }

    public static void drawText(GuiGraphics graphics, String text, int x, int y, int color, boolean shadow) {
        graphics.drawString(font(), text, x, y, color, shadow);
    }

    public static void drawText(GuiGraphics graphics, Component text, int x, int y, int color, boolean shadow) {
        graphics.drawString(font(), text, x, y, color, shadow);
    }

    public static void drawCenteredText(GuiGraphics graphics, String text, int centerX, int y, int color) {
        graphics.drawCenteredString(font(), text, centerX, y, color);
    }

    public static void drawRightAlignedText(GuiGraphics graphics, String text, int rightX, int y,
                                            int color, boolean shadow) {
        graphics.drawString(font(), text, rightX - font().width(text), y, color, shadow);
    }

    /** 文字超出宽度时截断并加省略号。 */
    public static void drawClippedText(GuiGraphics graphics, String text, int x, int y,
                                       int maxWidth, int color, boolean shadow) {
        Font f = font();
        String out = text;
        if (f.width(out) > maxWidth) {
            out = f.plainSubstrByWidth(out, Math.max(0, maxWidth - f.width("..."))) + "...";
        }
        graphics.drawString(f, out, x, y, color, shadow);
    }

    /**
     * 自动换行绘制多行文本，同时支持字符串里的 \n。
     *
     * @return 实际绘制的行数
     */
    public static int drawWrappedText(GuiGraphics graphics, String text, int x, int y,
                                      int maxWidth, int lineHeight, int maxLines, int color) {
        Font f = font();
        int drawn = 0;
        for (String paragraph : text.split("\n")) {
            List<FormattedCharSequence> lines = f.split(Component.literal(paragraph), maxWidth);
            for (FormattedCharSequence line : lines) {
                if (drawn >= maxLines) return drawn;
                graphics.drawString(f, line, x, y + drawn * lineHeight, color, false);
                drawn++;
            }
        }
        return drawn;
    }

    /** 计算 {@link #drawWrappedText} 会用掉多少行，用于提前算好面板高度。 */
    public static int countWrappedLines(String text, int maxWidth) {
        Font f = font();
        int total = 0;
        for (String paragraph : text.split("\n")) {
            total += Math.max(1, f.split(Component.literal(paragraph), maxWidth).size());
        }
        return total;
    }

    /** 开启裁剪区域，坐标是 GUI 缩放后的逻辑像素。 */
    public static void beginClip(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.enableScissor(x, y, x + width, y + height);
    }

    public static void endClip(GuiGraphics graphics) {
        graphics.disableScissor();
    }

    /** 画一个开关样式的指示灯。 */
    public static void drawToggleDot(GuiGraphics graphics, int x, int y, int size,
                                     boolean on, int onColor, int offColor) {
        drawRoundedRect(graphics, x, y, size, size, size / 2, on ? onColor : offColor);
    }

    /** 画一个水平滑块条。 */
    public static void drawSlider(GuiGraphics graphics, int x, int y, int width, int height,
                                  double fraction, int trackColor, int fillColor, int knobColor) {
        int f = (int) Math.round(Math.max(0.0D, Math.min(1.0D, fraction)) * width);
        drawRoundedRect(graphics, x, y, width, height, height / 2, trackColor);
        if (f > 0) {
            drawRoundedRect(graphics, x, y, Math.max(height, f), height, height / 2, fillColor);
        }
        int knobX = x + Math.max(0, Math.min(width - height, f - height / 2));
        drawRoundedRect(graphics, knobX, y - 1, height + 2, height + 2, (height + 2) / 2, knobColor);
    }

    // ==================================================================
    // 3D / 世界
    // ==================================================================

    private static MultiBufferSource.BufferSource buffers() {
        return Minecraft.getInstance().renderBuffers().bufferSource();
    }

    /**
     * 在世界坐标上画一个线框盒子。
     *
     * 会自己处理相机偏移 —— 旧代码直接把世界坐标喂给 renderLineBox，
     * 结果盒子画在了离玩家很远的地方。
     */
    public static void drawWorldBox(PoseStack poseStack, Camera camera, AABB box,
                                    float red, float green, float blue, float alpha) {
        Vec3 cam = camera.getPosition();
        poseStack.pushPose();
        poseStack.translate(-cam.x, -cam.y, -cam.z);

        VertexConsumer builder = buffers().getBuffer(RenderType.lines());
        LevelRenderer.renderLineBox(poseStack, builder,
            box.minX, box.minY, box.minZ,
            box.maxX, box.maxY, box.maxZ,
            red, green, blue, alpha);

        poseStack.popPose();
        buffers().endBatch(RenderType.lines());
    }

    /** 在世界坐标上画一个半透明填充盒子（类似 Litematica 的选区高亮）。 */
    public static void drawWorldFilledBox(PoseStack poseStack, Camera camera, AABB box,
                                          float red, float green, float blue, float alpha) {
        Vec3 cam = camera.getPosition();
        poseStack.pushPose();
        poseStack.translate(-cam.x, -cam.y, -cam.z);

        VertexConsumer builder = buffers().getBuffer(RenderType.debugFilledBox());
        Matrix4f matrix = poseStack.last().pose();
        float x1 = (float) box.minX, y1 = (float) box.minY, z1 = (float) box.minZ;
        float x2 = (float) box.maxX, y2 = (float) box.maxY, z2 = (float) box.maxZ;

        // 下、上
        quad(builder, matrix, x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2, red, green, blue, alpha);
        quad(builder, matrix, x1, y2, z1, x1, y2, z2, x2, y2, z2, x2, y2, z1, red, green, blue, alpha);
        // 北、南
        quad(builder, matrix, x1, y1, z1, x1, y2, z1, x2, y2, z1, x2, y1, z1, red, green, blue, alpha);
        quad(builder, matrix, x1, y1, z2, x2, y1, z2, x2, y2, z2, x1, y2, z2, red, green, blue, alpha);
        // 西、东
        quad(builder, matrix, x1, y1, z1, x1, y1, z2, x1, y2, z2, x1, y2, z1, red, green, blue, alpha);
        quad(builder, matrix, x2, y1, z1, x2, y2, z1, x2, y2, z2, x2, y1, z2, red, green, blue, alpha);

        poseStack.popPose();
        buffers().endBatch(RenderType.debugFilledBox());
    }

    private static void quad(VertexConsumer builder, Matrix4f matrix,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz,
                             float r, float g, float b, float a) {
        builder.vertex(matrix, ax, ay, az).color(r, g, b, a).endVertex();
        builder.vertex(matrix, bx, by, bz).color(r, g, b, a).endVertex();
        builder.vertex(matrix, cx, cy, cz).color(r, g, b, a).endVertex();
        builder.vertex(matrix, dx, dy, dz).color(r, g, b, a).endVertex();
    }

    /** 画一条世界坐标下的线段。 */
    public static void drawWorldLine(PoseStack poseStack, Camera camera,
                                     double x1, double y1, double z1,
                                     double x2, double y2, double z2,
                                     float red, float green, float blue, float alpha) {
        Vec3 cam = camera.getPosition();
        poseStack.pushPose();
        poseStack.translate(-cam.x, -cam.y, -cam.z);

        VertexConsumer builder = buffers().getBuffer(RenderType.lines());
        line(poseStack, builder, x1, y1, z1, x2, y2, z2, red, green, blue, alpha);

        poseStack.popPose();
        buffers().endBatch(RenderType.lines());
    }

    /**
     * 批量画线。
     *
     * 一次 push/endBatch 里可以画任意多条线段 —— 区块边界、光照覆盖这类
     * 功能每帧要画几百条，逐条调用 {@link #drawWorldLine} 会产生同样多的
     * 提交批次，白白吃掉帧数。
     */
    public static final class LineBatch implements AutoCloseable {
        private final PoseStack poseStack;
        private final VertexConsumer builder;

        private LineBatch(PoseStack poseStack, Camera camera) {
            this.poseStack = poseStack;
            Vec3 cam = camera.getPosition();
            poseStack.pushPose();
            poseStack.translate(-cam.x, -cam.y, -cam.z);
            this.builder = buffers().getBuffer(RenderType.lines());
        }

        public LineBatch line(double x1, double y1, double z1,
                              double x2, double y2, double z2,
                              float red, float green, float blue, float alpha) {
            RenderUtils.line(poseStack, builder, x1, y1, z1, x2, y2, z2, red, green, blue, alpha);
            return this;
        }

        public LineBatch box(AABB box, float red, float green, float blue, float alpha) {
            LevelRenderer.renderLineBox(poseStack, builder,
                box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ,
                red, green, blue, alpha);
            return this;
        }

        /**
         * 画单位方块某个面的四条边 —— 投影式叠加层的轮廓就是这样拼出来的。
         */
        public LineBatch blockFaceEdges(int x, int y, int z, Direction face, float grow,
                                        float red, float green, float blue, float alpha) {
            float x1 = x - grow, y1 = y - grow, z1 = z - grow;
            float x2 = x + 1 + grow, y2 = y + 1 + grow, z2 = z + 1 + grow;
            switch (face) {
                case DOWN -> {
                    line(x1, y1, z1, x2, y1, z1, red, green, blue, alpha);
                    line(x2, y1, z1, x2, y1, z2, red, green, blue, alpha);
                    line(x2, y1, z2, x1, y1, z2, red, green, blue, alpha);
                    line(x1, y1, z2, x1, y1, z1, red, green, blue, alpha);
                }
                case UP -> {
                    line(x1, y2, z1, x2, y2, z1, red, green, blue, alpha);
                    line(x2, y2, z1, x2, y2, z2, red, green, blue, alpha);
                    line(x2, y2, z2, x1, y2, z2, red, green, blue, alpha);
                    line(x1, y2, z2, x1, y2, z1, red, green, blue, alpha);
                }
                case NORTH -> {
                    line(x1, y1, z1, x2, y1, z1, red, green, blue, alpha);
                    line(x2, y1, z1, x2, y2, z1, red, green, blue, alpha);
                    line(x2, y2, z1, x1, y2, z1, red, green, blue, alpha);
                    line(x1, y2, z1, x1, y1, z1, red, green, blue, alpha);
                }
                case SOUTH -> {
                    line(x1, y1, z2, x2, y1, z2, red, green, blue, alpha);
                    line(x2, y1, z2, x2, y2, z2, red, green, blue, alpha);
                    line(x2, y2, z2, x1, y2, z2, red, green, blue, alpha);
                    line(x1, y2, z2, x1, y1, z2, red, green, blue, alpha);
                }
                case WEST -> {
                    line(x1, y1, z1, x1, y1, z2, red, green, blue, alpha);
                    line(x1, y1, z2, x1, y2, z2, red, green, blue, alpha);
                    line(x1, y2, z2, x1, y2, z1, red, green, blue, alpha);
                    line(x1, y2, z1, x1, y1, z1, red, green, blue, alpha);
                }
                case EAST -> {
                    line(x2, y1, z1, x2, y1, z2, red, green, blue, alpha);
                    line(x2, y1, z2, x2, y2, z2, red, green, blue, alpha);
                    line(x2, y2, z2, x2, y2, z1, red, green, blue, alpha);
                    line(x2, y2, z1, x2, y1, z1, red, green, blue, alpha);
                }
            }
            return this;
        }

        @Override
        public void close() {
            poseStack.popPose();
            buffers().endBatch(RenderType.lines());
        }
    }

    public static LineBatch beginWorldLines(PoseStack poseStack, Camera camera) {
        return new LineBatch(poseStack, camera);
    }

    /**
     * 批量画半透明面。
     *
     * 全息预览动辄上万个体素的外表面，逐个调 {@link #drawWorldFilledBox} 会提交
     * 上万个批次，帧数直接归零。这里整批只提交一次。
     */
    public static final class QuadBatch implements AutoCloseable {
        private final PoseStack poseStack;
        private final VertexConsumer builder;

        private QuadBatch(PoseStack poseStack, Camera camera) {
            this.poseStack = poseStack;
            Vec3 cam = camera.getPosition();
            poseStack.pushPose();
            poseStack.translate(-cam.x, -cam.y, -cam.z);
            this.builder = buffers().getBuffer(RenderType.debugFilledBox());
        }

        /**
         * 画一个单位方块的某一个面。
         *
         * @param face 面朝向，决定画在方块的哪一侧
         * @param grow 向外撑开的距离，用来避开和真实方块表面的 z-fighting
         */
        public QuadBatch blockFace(int x, int y, int z, Direction face, float grow,
                                   float red, float green, float blue, float alpha) {
            Matrix4f matrix = poseStack.last().pose();
            float x1 = x - grow, y1 = y - grow, z1 = z - grow;
            float x2 = x + 1 + grow, y2 = y + 1 + grow, z2 = z + 1 + grow;

            switch (face) {
                case DOWN -> quad(builder, matrix, x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2,
                    red, green, blue, alpha);
                case UP -> quad(builder, matrix, x1, y2, z1, x1, y2, z2, x2, y2, z2, x2, y2, z1,
                    red, green, blue, alpha);
                case NORTH -> quad(builder, matrix, x1, y1, z1, x1, y2, z1, x2, y2, z1, x2, y1, z1,
                    red, green, blue, alpha);
                case SOUTH -> quad(builder, matrix, x1, y1, z2, x2, y1, z2, x2, y2, z2, x1, y2, z2,
                    red, green, blue, alpha);
                case WEST -> quad(builder, matrix, x1, y1, z1, x1, y1, z2, x1, y2, z2, x1, y2, z1,
                    red, green, blue, alpha);
                case EAST -> quad(builder, matrix, x2, y1, z1, x2, y2, z1, x2, y2, z2, x2, y1, z2,
                    red, green, blue, alpha);
            }
            return this;
        }

        @Override
        public void close() {
            poseStack.popPose();
            buffers().endBatch(RenderType.debugFilledBox());
        }
    }

    public static QuadBatch beginWorldQuads(PoseStack poseStack, Camera camera) {
        return new QuadBatch(poseStack, camera);
    }

    /** 在盒子的 8 个角上画十字标记，方便看清选区端点。 */
    public static void drawWorldCornerMarkers(PoseStack poseStack, Camera camera, AABB box,
                                              float size, float red, float green, float blue, float alpha) {
        Vec3 cam = camera.getPosition();
        poseStack.pushPose();
        poseStack.translate(-cam.x, -cam.y, -cam.z);

        VertexConsumer builder = buffers().getBuffer(RenderType.lines());
        double[] xs = {box.minX, box.maxX};
        double[] ys = {box.minY, box.maxY};
        double[] zs = {box.minZ, box.maxZ};
        for (double x : xs) {
            for (double y : ys) {
                for (double z : zs) {
                    line(poseStack, builder, x - size, y, z, x + size, y, z, red, green, blue, alpha);
                    line(poseStack, builder, x, y - size, z, x, y + size, z, red, green, blue, alpha);
                    line(poseStack, builder, x, y, z - size, x, y, z + size, red, green, blue, alpha);
                }
            }
        }

        poseStack.popPose();
        buffers().endBatch(RenderType.lines());
    }

    /** 在世界某个位置画朝向玩家的文字标签。 */
    public static void drawWorldLabel(PoseStack poseStack, Camera camera, String text,
                                      double x, double y, double z, int color) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.font == null) return;

        Vec3 cam = camera.getPosition();
        poseStack.pushPose();
        poseStack.translate(x - cam.x, y - cam.y, z - cam.z);
        poseStack.mulPose(camera.rotation());
        poseStack.scale(-0.025F, -0.025F, 0.025F);

        MultiBufferSource.BufferSource source = buffers();
        mc.font.drawInBatch(
            Component.literal(text),
            -mc.font.width(text) / 2.0F,
            0.0F,
            color,
            false,
            poseStack.last().pose(),
            source,
            Font.DisplayMode.NORMAL,
            0,
            15728880);
        source.endBatch();

        poseStack.popPose();
    }

    private static void line(PoseStack poseStack, VertexConsumer builder,
                             double x1, double y1, double z1,
                             double x2, double y2, double z2,
                             float red, float green, float blue, float alpha) {
        Matrix4f matrix = poseStack.last().pose();
        float nx = (float) (x2 - x1);
        float ny = (float) (y2 - y1);
        float nz = (float) (z2 - z1);
        float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len > 0.0F) {
            nx /= len;
            ny /= len;
            nz /= len;
        } else {
            nx = 1.0F;
            ny = 0.0F;
            nz = 0.0F;
        }
        builder.vertex(matrix, (float) x1, (float) y1, (float) z1)
            .color(red, green, blue, alpha).normal(nx, ny, nz).endVertex();
        builder.vertex(matrix, (float) x2, (float) y2, (float) z2)
            .color(red, green, blue, alpha).normal(nx, ny, nz).endVertex();
    }
}
