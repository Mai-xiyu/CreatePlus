package org.xiyu.yee.createplus.features;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.xiyu.yee.createplus.settings.ColorSetting;
import org.xiyu.yee.createplus.settings.EnumSetting;
import org.xiyu.yee.createplus.settings.IntSetting;
import org.xiyu.yee.createplus.utils.CreativeGate;
import org.xiyu.yee.createplus.utils.KeyBindings;
import org.xiyu.yee.createplus.utils.RenderUtils;

/**
 * 层切面工坊：高亮当前工作层，并把放置锁在这一层（Litematica 层模式的轻量版）。
 */
public class LayerSlice extends CreativePlusFeature implements WorldRenderFeature {

    public enum Mode { ALL_BELOW, ALL_ABOVE, ONLY_LAYER }

    private final EnumSetting<Mode> mode = enumSetting("mode", Mode.ONLY_LAYER);
    private final IntSetting layer = intSetting("layer", 64, -64, 320);
    private final IntSetting thickness = intSetting("thickness", 1, 1, 16);
    private final IntSetting planeSize = intSetting("plane_size", 24, 8, 64);
    private final ColorSetting planeColor = colorSetting("plane_color", 0x4400A2FF);

    public LayerSlice() {
        super("layer_slice", "按 Y 层锁定建造，带层平面预览（需创造）", Category.BUILDING);
    }

    @Override
    public void onEnable() {
        MinecraftForge.EVENT_BUS.register(this);
        if (player() != null) layer.set(player().blockPosition().getY());
        if (!CreativeGate.allow()) {
            notifyPlayer("§c[CreatePlus] §7层切面只在创造模式下生效");
        } else {
            notifyPlayer("§b[CreatePlus] §7层切面：Ctrl+滚轮改层");
        }
    }

    @Override
    public void onDisable() {
        MinecraftForge.EVENT_BUS.unregister(this);
    }

    @Override
    public boolean handleScroll(double delta) {
        if (!isEnabled() || !CreativeGate.allow() || !KeyBindings.modifierDown()) return false;
        layer.cycle(delta > 0 ? 1 : -1);
        notifyActionBar("§b工作层: §fY=" + layer.value() + " §7· " + mode.getDisplayValue());
        return true;
    }

    @SubscribeEvent
    public void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        if (!isEnabled() || !event.isUseItem() || !CreativeGate.allow()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.hitResult == null || mc.hitResult.getType() != HitResult.Type.BLOCK) return;
        if (!(mc.player.getItemInHand(event.getHand()).getItem() instanceof BlockItem)) return;

        BlockHitResult hit = (BlockHitResult) mc.hitResult;
        BlockPos target = new BlockPlaceContext(mc.player, event.getHand(),
            mc.player.getItemInHand(event.getHand()), hit).getClickedPos();
        if (allowsY(target.getY())) return;

        event.setSwingHand(false);
        event.setCanceled(true);
        notifyActionBar("§c超出工作层 Y=" + layer.value());
    }

    public boolean allowsY(int y) {
        if (!isEnabled() || !CreativeGate.allow()) return true;
        int L = layer.value();
        int t = thickness.value();
        return switch (mode.get()) {
            case ONLY_LAYER -> y >= L && y < L + t;
            case ALL_BELOW -> y <= L;
            case ALL_ABOVE -> y >= L;
        };
    }

    @Override
    public void renderWorld(PoseStack poseStack, Camera camera, float partialTick) {
        if (!CreativeGate.allow() || player() == null) return;

        int L = layer.value();
        int size = planeSize.value();
        BlockPos origin = player().blockPosition();
        double x1 = origin.getX() - size;
        double z1 = origin.getZ() - size;
        double x2 = origin.getX() + size + 1;
        double z2 = origin.getZ() + size + 1;
        double y = L;

        int c = planeColor.argb();
        float r = ((c >> 16) & 0xFF) / 255.0F;
        float g = ((c >> 8) & 0xFF) / 255.0F;
        float b = (c & 0xFF) / 255.0F;
        float a = ((c >>> 24) & 0xFF) / 255.0F;

        AABB plane = new AABB(x1, y, z1, x2, y + 0.02D, z2);
        RenderUtils.drawWorldFilledBox(poseStack, camera, plane, r, g, b, a);
        try (RenderUtils.LineBatch batch = RenderUtils.beginWorldLines(poseStack, camera)) {
            batch.box(plane.inflate(0.0D, 0.01D, 0.0D), r, g, b, Math.min(1.0F, a + 0.4F));
        }
    }

    @Override
    public String getDetailedDescription() {
        return getTranslatedDescription()
            + "\n§7模式: §f" + mode.getDisplayValue()
            + " §7层: §f" + layer.value()
            + " §7厚度: §f" + thickness.value()
            + "\n§7Ctrl + 滚轮调层 · 越层放置会被拦下"
            + (CreativeGate.allow() ? "\n§a创造 · 已激活" : "\n§c非创造 · 已暂停");
    }
}
