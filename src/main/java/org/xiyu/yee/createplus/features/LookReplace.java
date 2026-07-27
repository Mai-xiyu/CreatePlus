package org.xiyu.yee.createplus.features;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.xiyu.yee.createplus.settings.BooleanSetting;
import org.xiyu.yee.createplus.utils.CreativeGate;
import org.xiyu.yee.createplus.utils.EditHistory;
import org.xiyu.yee.createplus.utils.EditQueue;
import org.xiyu.yee.createplus.utils.KeyBindings;

/**
 * 准星替换：Ctrl+左键把手上的方块盖到准星所指位置，不破坏原操作手感。
 */
public class LookReplace extends CreativePlusFeature {

    private final BooleanSetting requireModifier = boolSetting("require_modifier", true);

    public LookReplace() {
        super("look_replace", "Ctrl+左键用手上的方块替换准星方块（需创造）", Category.BUILDING);
    }

    @Override
    public void onEnable() {
        MinecraftForge.EVENT_BUS.register(this);
        if (!CreativeGate.allow()) {
            notifyPlayer("§c[CreatePlus] §7准星替换只在创造模式下生效");
        } else {
            notifyPlayer("§b[CreatePlus] §7准星替换：Ctrl+左键盖掉准星方块");
        }
    }

    @Override
    public void onDisable() {
        MinecraftForge.EVENT_BUS.unregister(this);
    }

    @SubscribeEvent
    public void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        if (!isEnabled() || !event.isAttack() || !CreativeGate.allow()) return;
        if (requireModifier.get() && !KeyBindings.modifierDown()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (mc.hitResult == null || mc.hitResult.getType() != HitResult.Type.BLOCK) return;
        if (EditQueue.isBusy()) {
            notifyActionBar("§c上一次操作还没做完");
            return;
        }

        ItemStack held = mc.player.getMainHandItem();
        if (!(held.getItem() instanceof BlockItem item)) {
            notifyActionBar("§c主手需要手持方块");
            return;
        }

        BlockPos pos = ((BlockHitResult) mc.hitResult).getBlockPos();
        BlockState target = item.getBlock().defaultBlockState();
        // 若状态剪贴板有同种方块，优先用剪贴板
        StateClipboard clip = org.xiyu.yee.createplus.Createplus.FEATURE_MANAGER.get(StateClipboard.class);
        if (clip != null && clip.isEnabled() && clip.getClipboard() != null
            && clip.getClipboard().getBlock() == item.getBlock()) {
            target = clip.getClipboard();
        }

        if (PaintPalette.active() != null) {
            target = PaintPalette.active().pick(pos, target);
        }

        EditHistory.Step step = EditHistory.begin("准星替换");
        step.record(pos, mc.level.getBlockState(pos));
        EditHistory.commit(step);
        EditQueue.start("准星替换");
        EditQueue.enqueue(pos, target);

        event.setSwingHand(false);
        event.setCanceled(true);
        notifyActionBar("§a已替换 §f" + pos.toShortString());
    }

    @Override
    public String getDetailedDescription() {
        return getTranslatedDescription()
            + "\n§7" + (requireModifier.get() ? "Ctrl + 左键" : "左键") + " 替换准星方块"
            + (CreativeGate.allow() ? "\n§a创造 · 已激活" : "\n§c非创造 · 已暂停");
    }
}
