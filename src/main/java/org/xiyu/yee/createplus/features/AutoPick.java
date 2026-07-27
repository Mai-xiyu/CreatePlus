package org.xiyu.yee.createplus.features;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;
import org.xiyu.yee.createplus.settings.BooleanSetting;
import org.xiyu.yee.createplus.utils.CreativeGate;

/**
 * 中键精确拾取：创造模式下把方块状态 / 方块实体 NBT 一起抓进物品栏。
 */
public class AutoPick extends CreativePlusFeature {

    private final BooleanSetting copyNbt = boolSetting("copy_nbt", true);

    public AutoPick() {
        super("auto_pick", "中键拾取完整方块状态与 NBT（需创造）", Category.BUILDING);
    }

    @Override
    public void onEnable() {
        MinecraftForge.EVENT_BUS.register(this);
        if (!CreativeGate.allow()) {
            notifyPlayer("§c[CreatePlus] §7精确拾取只在创造模式下生效");
        }
    }

    @Override
    public void onDisable() {
        MinecraftForge.EVENT_BUS.unregister(this);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onMouse(InputEvent.MouseButton.Pre event) {
        if (!isEnabled() || !CreativeGate.allow()) return;
        if (event.getButton() != GLFW.GLFW_MOUSE_BUTTON_MIDDLE || event.getAction() != GLFW.GLFW_PRESS) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null || mc.player == null || mc.level == null || mc.gameMode == null) return;
        if (mc.hitResult == null || mc.hitResult.getType() != HitResult.Type.BLOCK) return;

        BlockPos pos = ((BlockHitResult) mc.hitResult).getBlockPos();
        BlockState state = mc.level.getBlockState(pos);
        ItemStack stack = state.getBlock().getCloneItemStack(mc.level, pos, state);
        if (stack.isEmpty()) {
            stack = new ItemStack(state.getBlock().asItem());
        }

        if (copyNbt.get()) {
            BlockEntity be = mc.level.getBlockEntity(pos);
            if (be != null) {
                CompoundTag tag = be.saveWithFullMetadata();
                stack.getOrCreateTag().put("BlockEntityTag", tag);
            }
            // 把方块状态也塞进显示名旁注，方便辨认
            stack.setHoverName(net.minecraft.network.chat.Component.literal(
                state.getBlock().getName().getString() + " §8[" +
                    BuiltInRegistries.BLOCK.getKey(state.getBlock()).getPath() + "]"));
        }

        mc.gameMode.handleCreativeModeItemAdd(stack, 36 + mc.player.getInventory().selected);
        mc.player.getInventory().setItem(mc.player.getInventory().selected, stack);
        notifyActionBar("§a已拾取 §f" + stack.getHoverName().getString());
        event.setCanceled(true);
    }

    @Override
    public String getDetailedDescription() {
        return getTranslatedDescription()
            + "\n§7复制 NBT: §f" + (copyNbt.get() ? "开" : "关")
            + (CreativeGate.allow() ? "\n§a创造 · 已激活" : "\n§c非创造 · 已暂停");
    }
}
