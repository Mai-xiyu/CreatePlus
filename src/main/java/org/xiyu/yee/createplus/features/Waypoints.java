package org.xiyu.yee.createplus.features;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.xiyu.yee.createplus.settings.ColorSetting;
import org.xiyu.yee.createplus.settings.IntSetting;
import org.xiyu.yee.createplus.utils.CreativeGate;
import org.xiyu.yee.createplus.utils.KeyBindings;
import org.xiyu.yee.createplus.utils.RenderUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 路径面包屑：一边飞一边撒点，回头按轨迹找路；也可手动钉航点光柱。
 */
public class Waypoints extends CreativePlusFeature implements WorldRenderFeature {

    private record Mark(BlockPos pos, String label) {
    }

    private final IntSetting maxPoints = intSetting("max_points", 64, 8, 256);
    private final IntSetting autoInterval = intSetting("auto_interval", 0, 0, 40);
    private final ColorSetting color = colorSetting("color", 0xE055FFAA);
    private final ColorSetting beamColor = colorSetting("beam_color", 0x8800FFAA);

    private final List<Mark> marks = new ArrayList<>();
    private int ticker;

    public Waypoints() {
        super("waypoints", "航点光柱与自动面包屑轨迹（需创造）", Category.RENDER);
    }

    @Override
    public void onEnable() {
        MinecraftForge.EVENT_BUS.register(this);
        ticker = 0;
        if (!CreativeGate.allow()) {
            notifyPlayer("§c[CreatePlus] §7航点只在创造模式下生效");
        } else {
            notifyPlayer("§b[CreatePlus] §7航点：Ctrl+右键钉点，潜行+滚轮清空");
        }
    }

    @Override
    public void onDisable() {
        MinecraftForge.EVENT_BUS.unregister(this);
        marks.clear();
    }

    @Override
    public void onTick() {
        if (!CreativeGate.allow() || autoInterval.value() <= 0 || player() == null) return;
        if (++ticker < autoInterval.value()) return;
        ticker = 0;
        add(player().blockPosition(), "轨迹");
    }

    @SubscribeEvent
    public void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        if (!isEnabled() || !event.isUseItem() || !CreativeGate.allow()) return;
        if (!KeyBindings.modifierDown()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.hitResult == null || mc.hitResult.getType() != HitResult.Type.BLOCK) return;

        BlockPos pos = ((BlockHitResult) mc.hitResult).getBlockPos().above();
        add(pos, "航点");
        notifyActionBar("§a航点 #" + marks.size() + " §f" + pos.toShortString());
        event.setSwingHand(false);
        event.setCanceled(true);
    }

    @Override
    public boolean handleScroll(double delta) {
        if (!isEnabled() || !CreativeGate.allow()) return false;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !mc.player.isShiftKeyDown()) return false;
        marks.clear();
        notifyActionBar("§c已清空航点");
        return true;
    }

    private void add(BlockPos pos, String label) {
        marks.add(new Mark(pos.immutable(), label));
        while (marks.size() > maxPoints.value()) marks.remove(0);
    }

    @Override
    public void renderWorld(PoseStack poseStack, Camera camera, float partialTick) {
        if (!CreativeGate.allow() || marks.isEmpty()) return;
        int c = color.argb();
        int beam = beamColor.argb();
        float r = ((c >> 16) & 0xFF) / 255.0F;
        float g = ((c >> 8) & 0xFF) / 255.0F;
        float b = (c & 0xFF) / 255.0F;
        float a = ((c >>> 24) & 0xFF) / 255.0F;
        float br = ((beam >> 16) & 0xFF) / 255.0F;
        float bg = ((beam >> 8) & 0xFF) / 255.0F;
        float bb = (beam & 0xFF) / 255.0F;
        float ba = ((beam >>> 24) & 0xFF) / 255.0F;

        try (RenderUtils.LineBatch batch = RenderUtils.beginWorldLines(poseStack, camera)) {
            Mark prev = null;
            for (Mark mark : marks) {
                BlockPos p = mark.pos();
                batch.box(new AABB(p).inflate(0.05D), r, g, b, a);
                batch.line(p.getX() + 0.5, p.getY(), p.getZ() + 0.5,
                    p.getX() + 0.5, p.getY() + 32.0, p.getZ() + 0.5, br, bg, bb, ba);
                if (prev != null) {
                    batch.line(
                        prev.pos().getX() + 0.5, prev.pos().getY() + 0.5, prev.pos().getZ() + 0.5,
                        p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, r, g, b, a * 0.7F);
                }
                prev = mark;
            }
        }
    }

    @Override
    public String getDetailedDescription() {
        return getTranslatedDescription()
            + "\n§7航点: §f" + marks.size()
            + " §7自动间隔: §f" + (autoInterval.value() == 0 ? "关" : autoInterval.value() + "t")
            + "\n§7Ctrl+右键钉点 · 潜行+滚轮清空"
            + (CreativeGate.allow() ? "\n§a创造 · 已激活" : "\n§c非创造 · 已暂停");
    }
}
