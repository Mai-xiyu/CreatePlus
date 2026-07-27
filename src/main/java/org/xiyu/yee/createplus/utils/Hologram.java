package org.xiyu.yee.createplus.utils;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.xiyu.yee.createplus.features.PaintPalette;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 一团待落地的体素，可以先看后建。
 *
 * 渲染刻意贴近投影（Litematica）的叠加层：半透明外表面 + 每面描边 +
 * 一层能穿墙看到的轮廓。生成器给出的是相对锚点的偏移，锚点跟着准星变，
 * 渲染时才加上 —— 否则挪一下鼠标就得重算几万个体素。
 */
public final class Hologram {

    /** 超过这个面数就只画包围盒。再多的半透明面既看不清也拖帧。 */
    private static final int FACE_LIMIT = 40000;
    /** 描边比填面更吃顶点，单独收紧一档。 */
    private static final int OUTLINE_LIMIT = 12000;
    private static final float GROW = 0.002F;

    /** 一个待绘制的外表面。 */
    private record Face(int x, int y, int z, Direction side) {
    }

    private List<BlockPos> offsets = List.of();
    private List<Face> faces = List.of();
    private AABB bounds;

    public boolean isEmpty() {
        return offsets.isEmpty();
    }

    public int size() {
        return offsets.size();
    }

    public List<BlockPos> offsets() {
        return offsets;
    }

    public void clear() {
        offsets = List.of();
        faces = List.of();
        bounds = null;
    }

    /** 换一批体素，同时重算外表面与包围盒。只在参数变化时调用。 */
    public void set(List<BlockPos> newOffsets) {
        if (newOffsets.isEmpty()) {
            clear();
            return;
        }

        offsets = newOffsets;
        bounds = computeBounds(newOffsets);

        List<Face> computed = computeFaces(newOffsets);
        // 画一半的壳比不画更难看，超限就整个退回只画包围盒
        faces = computed == null ? List.of() : computed;
    }

    private static AABB computeBounds(List<BlockPos> positions) {
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;
        for (BlockPos pos : positions) {
            minX = Math.min(minX, pos.getX());
            minY = Math.min(minY, pos.getY());
            minZ = Math.min(minZ, pos.getZ());
            maxX = Math.max(maxX, pos.getX());
            maxY = Math.max(maxY, pos.getY());
            maxZ = Math.max(maxZ, pos.getZ());
        }
        return new AABB(minX, minY, minZ, maxX + 1.0D, maxY + 1.0D, maxZ + 1.0D);
    }

    /** @return 外表面列表，超过 {@link #FACE_LIMIT} 时返回 null */
    private static List<Face> computeFaces(List<BlockPos> positions) {
        Set<BlockPos> filled = new HashSet<>(positions.size() * 2);
        filled.addAll(positions);

        List<Face> result = new ArrayList<>();
        BlockPos.MutableBlockPos probe = new BlockPos.MutableBlockPos();
        for (BlockPos pos : positions) {
            for (Direction side : Direction.values()) {
                probe.set(pos.getX() + side.getStepX(),
                    pos.getY() + side.getStepY(),
                    pos.getZ() + side.getStepZ());
                if (filled.contains(probe)) continue;
                result.add(new Face(pos.getX(), pos.getY(), pos.getZ(), side));
                if (result.size() >= FACE_LIMIT) return null;
            }
        }
        return result;
    }

    /**
     * 画出预览 —— 投影风格：填色面 + 描边 + 穿墙线框。
     *
     * @param anchor 相对坐标的基准点
     */
    public void render(PoseStack poseStack, Camera camera, BlockPos anchor,
                       float red, float green, float blue, float alpha) {
        if (bounds == null) return;

        AABB world = bounds.move(anchor.getX(), anchor.getY(), anchor.getZ()).inflate(GROW);
        float fillAlpha = Math.min(0.45F, Math.max(0.12F, alpha));
        float edgeAlpha = Math.min(0.95F, fillAlpha + 0.45F);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);

        // 1) 受深度测试的半透明面 —— 被挡住的部分自然变暗，和投影 ghost 一样
        if (!faces.isEmpty()) {
            try (RenderUtils.QuadBatch batch = RenderUtils.beginWorldQuads(poseStack, camera)) {
                for (Face face : faces) {
                    batch.blockFace(
                        anchor.getX() + face.x(),
                        anchor.getY() + face.y(),
                        anchor.getZ() + face.z(),
                        face.side(), GROW, red, green, blue, fillAlpha);
                }
            }
        }

        // 2) 每面描边（投影叠加层的招牌轮廓）
        boolean outlineFaces = !faces.isEmpty() && faces.size() <= OUTLINE_LIMIT;
        try (RenderUtils.LineBatch batch = RenderUtils.beginWorldLines(poseStack, camera)) {
            if (outlineFaces) {
                for (Face face : faces) {
                    batch.blockFaceEdges(
                        anchor.getX() + face.x(),
                        anchor.getY() + face.y(),
                        anchor.getZ() + face.z(),
                        face.side(), GROW, red, green, blue, edgeAlpha);
                }
            }
            batch.box(world, red, green, blue, 0.7F);
        }

        // 3) 穿墙线框：关掉深度测试再描一遍更淡的包围盒，隔着山也能看见落点
        RenderSystem.disableDepthTest();
        try (RenderUtils.LineBatch batch = RenderUtils.beginWorldLines(poseStack, camera)) {
            batch.box(world, red, green, blue, 0.28F);
        }
        RenderSystem.enableDepthTest();

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
    }

    /**
     * 把预览落成真方块。
     *
     * @param fallback 调色板为空时使用的方块
     * @return 排入队列的方块数，超出上限时返回 -1
     */
    public int commit(BlockPos anchor, String label, BlockState fallback) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || offsets.isEmpty()) return 0;
        if (offsets.size() > BlockEdits.MAX_VOLUME) return -1;

        PaintPalette palette = PaintPalette.active();
        EditHistory.Step step = EditHistory.begin(label);
        EditQueue.start(label);

        for (BlockPos offset : offsets) {
            BlockPos target = anchor.offset(offset);
            step.record(target, mc.level.getBlockState(target));
            EditQueue.enqueue(target, palette == null ? fallback : palette.pick(target, fallback));
        }

        EditHistory.commit(step);
        return offsets.size();
    }
}
