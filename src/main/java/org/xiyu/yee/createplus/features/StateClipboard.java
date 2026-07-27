package org.xiyu.yee.createplus.features;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.xiyu.yee.createplus.settings.BooleanSetting;
import org.xiyu.yee.createplus.utils.CreativeGate;
import org.xiyu.yee.createplus.utils.EditHistory;
import org.xiyu.yee.createplus.utils.EditQueue;
import org.xiyu.yee.createplus.utils.KeyBindings;

/**
 * 方块状态剪贴板：Ctrl+中键复制准星方块的完整 BlockState，放置时自动套上。
 */
public class StateClipboard extends CreativePlusFeature {

    private final BooleanSetting applyOnPlace = boolSetting("apply_on_place", true);

    private BlockState clipboard;

    public StateClipboard() {
        super("state_clipboard", "复制/粘贴方块状态（朝向、含水等，需创造）", Category.BUILDING);
    }

    @Override
    public void onEnable() {
        MinecraftForge.EVENT_BUS.register(this);
        if (!CreativeGate.allow()) {
            notifyPlayer("§c[CreatePlus] §7状态剪贴板只在创造模式下生效");
        } else {
            notifyPlayer("§b[CreatePlus] §7Ctrl+中键复制方块状态，放置时自动套用");
        }
    }

    @Override
    public void onDisable() {
        MinecraftForge.EVENT_BUS.unregister(this);
    }

    @SubscribeEvent
    public void onMouse(InputEvent.MouseButton.Pre event) {
        if (!isEnabled() || !CreativeGate.allow()) return;
        if (event.getButton() != org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_MIDDLE) return;
        if (event.getAction() != org.lwjgl.glfw.GLFW.GLFW_PRESS) return;
        if (!KeyBindings.modifierDown()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null || mc.level == null) return;
        if (mc.hitResult == null || mc.hitResult.getType() != HitResult.Type.BLOCK) return;

        BlockPos pos = ((BlockHitResult) mc.hitResult).getBlockPos();
        clipboard = mc.level.getBlockState(pos);
        notifyActionBar("§a已复制状态 §f" + describe(clipboard));
        event.setCanceled(true);
    }

    @SubscribeEvent
    public void onPlace(PlayerInteractEvent.RightClickBlock event) {
        if (!isEnabled() || !applyOnPlace.get() || !CreativeGate.allow()) return;
        if (!event.getLevel().isClientSide() || clipboard == null) return;
        if (!(event.getItemStack().getItem() instanceof BlockItem item)) return;
        if (item.getBlock() != clipboard.getBlock()) return;
        if (EditQueue.isBusy()) return;

        // 等原版放完再把状态纠正过来 —— 只改同种方块，避免乱套
        BlockPos placed = event.getPos().relative(event.getFace());
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        EditHistory.Step step = EditHistory.begin("粘贴方块状态");
        step.record(placed, mc.level.getBlockState(placed));
        EditHistory.commit(step);
        EditQueue.start("粘贴方块状态");
        EditQueue.enqueue(placed, clipboard);
    }

    public BlockState getClipboard() {
        return clipboard;
    }

    private static String describe(BlockState state) {
        CompoundTag tag = NbtUtils.writeBlockState(state);
        String id = tag.getString("Name");
        return id.isEmpty() ? state.getBlock().getName().getString() : id;
    }

    @Override
    public String getDetailedDescription() {
        return getTranslatedDescription()
            + "\n§7剪贴板: §f" + (clipboard == null ? "空" : describe(clipboard))
            + "\n§7Ctrl + 中键复制 · 手持同种方块放置时自动粘贴状态"
            + (CreativeGate.allow() ? "\n§a创造 · 已激活" : "\n§c非创造 · 已暂停");
    }
}
