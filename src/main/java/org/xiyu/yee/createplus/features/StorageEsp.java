package org.xiyu.yee.createplus.features;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;
import net.minecraft.world.phys.AABB;
import org.xiyu.yee.createplus.settings.ColorSetting;
import org.xiyu.yee.createplus.settings.IntSetting;
import org.xiyu.yee.createplus.utils.CreativeGate;
import org.xiyu.yee.createplus.utils.RenderUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 容器透视：箱子/潜影盒/木桶等隔墙描边。
 */
public class StorageEsp extends CreativePlusFeature implements WorldRenderFeature {

    private final IntSetting range = intSetting("range", 48, 16, 96);
    private final IntSetting refreshTicks = intSetting("refresh_ticks", 15, 5, 40);
    private final ColorSetting color = colorSetting("color", 0xD0FFD24B);

    private final List<BlockPos> hits = new ArrayList<>();
    private int ticker;

    public StorageEsp() {
        super("storage_esp", "隔墙显示附近容器（需创造）", Category.RENDER);
    }

    @Override
    public void onEnable() {
        hits.clear();
        if (!CreativeGate.allow()) {
            notifyPlayer("§c[CreatePlus] §7容器透视只在创造模式下生效");
        }
    }

    @Override
    public void onDisable() {
        hits.clear();
    }

    @Override
    public void onTick() {
        if (!CreativeGate.allow()) {
            hits.clear();
            return;
        }
        if (++ticker < refreshTicks.value()) return;
        ticker = 0;
        scan();
    }

    private void scan() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        hits.clear();
        BlockPos origin = mc.player.blockPosition();
        int r = range.value();
        for (int x = -r; x <= r; x++) {
            for (int y = -r / 2; y <= r / 2; y++) {
                for (int z = -r; z <= r; z++) {
                    BlockPos pos = origin.offset(x, y, z);
                    BlockEntity be = mc.level.getBlockEntity(pos);
                    if (be instanceof ChestBlockEntity
                        || be instanceof BarrelBlockEntity
                        || be instanceof ShulkerBoxBlockEntity
                        || be instanceof HopperBlockEntity
                        || be instanceof DispenserBlockEntity) {
                        hits.add(pos.immutable());
                    } else {
                        Block block = mc.level.getBlockState(pos).getBlock();
                        if (block == Blocks.ENDER_CHEST) hits.add(pos.immutable());
                    }
                }
            }
        }
    }

    @Override
    public void renderWorld(PoseStack poseStack, Camera camera, float partialTick) {
        if (!CreativeGate.allow() || hits.isEmpty()) return;
        int c = color.argb();
        float r = ((c >> 16) & 0xFF) / 255.0F;
        float g = ((c >> 8) & 0xFF) / 255.0F;
        float b = (c & 0xFF) / 255.0F;
        float a = ((c >>> 24) & 0xFF) / 255.0F;
        try (RenderUtils.LineBatch batch = RenderUtils.beginWorldLines(poseStack, camera)) {
            for (BlockPos pos : hits) {
                batch.box(new AABB(pos).inflate(0.02D), r, g, b, a);
            }
        }
    }

    @Override
    public String getDetailedDescription() {
        return getTranslatedDescription()
            + "\n§7命中: §f" + hits.size()
            + (CreativeGate.allow() ? "\n§a创造 · 已激活" : "\n§c非创造 · 已暂停");
    }
}
