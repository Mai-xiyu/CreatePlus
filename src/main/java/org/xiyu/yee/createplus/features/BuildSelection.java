package org.xiyu.yee.createplus.features;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.xiyu.yee.createplus.settings.BooleanSetting;
import org.xiyu.yee.createplus.settings.ColorSetting;
import org.xiyu.yee.createplus.settings.EnumSetting;
import org.xiyu.yee.createplus.utils.RenderUtils;
import org.xiyu.yee.createplus.utils.Selection;

/**
 * WorldEdit 风格的选区工具。
 *
 * 手持选区棒时：左键设起点，右键设终点，两者都不会真的破坏/放置方块。
 * 选好的区域会以线框高亮显示，并且被区域复制、建筑导出、区域填充共用。
 */
public class BuildSelection extends CreativePlusFeature implements WorldRenderFeature {

    public enum Wand {
        WOODEN_SHOVEL(Items.WOODEN_SHOVEL),
        WOODEN_AXE(Items.WOODEN_AXE),
        GOLDEN_SHOVEL(Items.GOLDEN_SHOVEL),
        STICK(Items.STICK),
        BLAZE_ROD(Items.BLAZE_ROD);

        private final Item item;

        Wand(Item item) {
            this.item = item;
        }

        public Item getItem() {
            return item;
        }
    }

    private final EnumSetting<Wand> wand = enumSetting("wand", Wand.WOODEN_SHOVEL);
    private final BooleanSetting showBox = boolSetting("show_box", true);
    private final BooleanSetting showFill = boolSetting("show_fill", true);
    private final BooleanSetting showCorners = boolSetting("show_corners", true);
    private final BooleanSetting showInfo = boolSetting("show_info", true);
    private final ColorSetting boxColor = colorSetting("box_color", 0xFF00A2FF);

    public BuildSelection() {
        super("build_selection", "选区工具：手持选区棒左键设起点、右键设终点", Category.BUILDING);
    }

    public static Selection selection() {
        return Selection.shared();
    }

    @Override
    public void onEnable() {
        MinecraftForge.EVENT_BUS.register(this);
        notifyPlayer("§b[CreatePlus] §7选区工具已启用，手持 §f"
            + wand.get().getItem().getDescription().getString()
            + " §7左键设起点、右键设终点");
    }

    @Override
    public void onDisable() {
        MinecraftForge.EVENT_BUS.unregister(this);
        selection().clear();
    }

    private boolean holdingWand() {
        Minecraft mc = Minecraft.getInstance();
        return mc.player != null && mc.player.getMainHandItem().is(wand.get().getItem());
    }

    /**
     * 拦截选区棒的左右键，避免在设置选区的同时破坏或放置方块。
     */
    @SubscribeEvent
    public void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        if (!isEnabled() || !holdingWand()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.hitResult == null || mc.hitResult.getType() != HitResult.Type.BLOCK) return;

        if (!event.isAttack() && !event.isUseItem()) return;

        BlockHitResult hit = (BlockHitResult) mc.hitResult;
        Selection selection = selection();

        if (event.isAttack()) {
            selection.setFirst(hit.getBlockPos());
            notifyActionBar("§a起点: §f" + hit.getBlockPos().toShortString()
                + (selection.isComplete() ? "  §7" + selection.describe() : ""));
        } else {
            selection.setSecond(hit.getBlockPos());
            notifyActionBar("§a终点: §f" + hit.getBlockPos().toShortString()
                + (selection.isComplete() ? "  §7" + selection.describe() : ""));
        }

        event.setSwingHand(false);
        event.setCanceled(true);
    }

    /** 潜行 + 手持选区棒时滚轮清空选区。 */
    @Override
    public boolean handleScroll(double delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !mc.player.isShiftKeyDown() || !holdingWand()) return false;
        if (selection().isEmpty()) return false;
        selection().clear();
        notifyActionBar("§c已清空选区");
        return true;
    }

    @Override
    public void renderWorld(PoseStack poseStack, Camera camera, float partialTick) {
        Selection selection = selection();
        AABB box = selection.getBox();
        if (box == null) return;

        int color = boxColor.argb();
        float r = ((color >> 16) & 0xFF) / 255.0F;
        float g = ((color >> 8) & 0xFF) / 255.0F;
        float b = (color & 0xFF) / 255.0F;

        // 只选了一个点时，用更小的高亮提示还差一个点
        AABB drawBox = selection.isComplete() ? box : box.inflate(0.005D);

        if (showFill.get()) {
            RenderUtils.drawWorldFilledBox(poseStack, camera, drawBox, r, g, b, 0.15F);
        }
        if (showBox.get()) {
            RenderUtils.drawWorldBox(poseStack, camera, drawBox, r, g, b, 0.9F);
        }
        if (showCorners.get() && selection.isComplete()) {
            RenderUtils.drawWorldCornerMarkers(poseStack, camera, drawBox, 0.35F, 1.0F, 0.85F, 0.2F, 1.0F);
        }
        if (showInfo.get() && selection.isComplete()) {
            RenderUtils.drawWorldLabel(poseStack, camera, selection.describe(),
                drawBox.getCenter().x, drawBox.maxY + 0.4D, drawBox.getCenter().z, 0xFFFFFFFF);
        }
    }

    @Override
    public String getDetailedDescription() {
        StringBuilder text = new StringBuilder(getTranslatedDescription());
        text.append("\n§7选区棒: §f").append(wand.get().getItem().getDescription().getString());
        Selection selection = selection();
        if (selection.isComplete()) {
            text.append("\n§7当前选区: §a").append(selection.describe());
        } else if (selection.getFirst() != null) {
            text.append("\n§7已设起点，还差终点");
        } else {
            text.append("\n§7尚未选择区域");
        }
        return text.toString();
    }
}
