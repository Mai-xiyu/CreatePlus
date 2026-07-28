package org.xiyu.yee.createplus.features;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.xiyu.yee.createplus.settings.IntSetting;
import org.xiyu.yee.createplus.utils.BlockEdits;
import org.xiyu.yee.createplus.utils.CreativeGate;
import org.xiyu.yee.createplus.utils.EditHistory;
import org.xiyu.yee.createplus.utils.EditQueue;
import org.xiyu.yee.createplus.utils.KeyBindings;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Queue;
import java.util.Set;

/**
 * 洪水填充：潜行+Ctrl+右键，把手上的方块灌进与准星相连的同种方块区域。
 */
public class FloodFill extends CreativePlusFeature {

    private final IntSetting maxBlocks = intSetting("max_blocks", 4096, 64, 32768);

    public FloodFill() {
        super("flood_fill", "洪水填充相连的同种方块（需创造）", Category.BUILDING);
    }

    @Override
    public void onEnable() {
        MinecraftForge.EVENT_BUS.register(this);
        if (!CreativeGate.allow()) {
            notifyPlayer("§c[CreatePlus] §7洪水填充只在创造模式下生效");
        } else {
            notifyPlayer("§b[CreatePlus] §7洪水填充：潜行 + Ctrl + 右键");
        }
    }

    @Override
    public void onDisable() {
        MinecraftForge.EVENT_BUS.unregister(this);
    }

    @SubscribeEvent
    public void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        if (!isEnabled() || !event.isUseItem() || !CreativeGate.allow()) return;
        if (!KeyBindings.modifierDown()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || !mc.player.isShiftKeyDown()) return;
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

        BlockPos start = ((BlockHitResult) mc.hitResult).getBlockPos();
        BlockState match = mc.level.getBlockState(start);
        BlockState fill = item.getBlock().defaultBlockState();
        if (match.isAir() || match.getBlock() == fill.getBlock()) {
            notifyActionBar("§7无需填充");
            return;
        }

        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();
        queue.add(start);
        visited.add(start);

        EditHistory.Step step = EditHistory.begin("洪水填充");
        EditQueue.start("洪水填充");
        int count = 0;
        int limit = Math.min(maxBlocks.value(), BlockEdits.maxVolume());

        while (!queue.isEmpty() && count < limit) {
            BlockPos pos = queue.poll();
            BlockState current = mc.level.getBlockState(pos);
            if (current.getBlock() != match.getBlock()) continue;

            BlockState placed = PaintPalette.active() == null
                ? fill
                : PaintPalette.active().pick(pos, fill);
            step.record(pos, current);
            EditQueue.enqueue(pos, placed);
            count++;

            for (Direction dir : Direction.values()) {
                BlockPos next = pos.relative(dir);
                if (visited.add(next)) queue.add(next);
            }
        }

        EditHistory.commit(step);
        event.setSwingHand(false);
        event.setCanceled(true);
        notifyActionBar(count >= limit
            ? "§e已达上限 §f" + count + " §e个方块"
            : "§a洪水填充 §f" + count + " §a个方块");
    }

    @Override
    public String getDetailedDescription() {
        return getTranslatedDescription()
            + "\n§7上限: §f" + maxBlocks.value()
            + "\n§7潜行 + Ctrl + 右键 · 替换与准星相连的同种方块"
            + (CreativeGate.allow() ? "\n§a创造 · 已激活" : "\n§c非创造 · 已暂停");
    }
}
