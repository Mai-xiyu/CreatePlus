package org.xiyu.yee.createplus.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 批量方块编辑的执行队列。
 *
 * 大批量修改必须分摊到多个 tick 上：一次性发几千个数据包会被服务器踢掉，
 * 而在客户端主线程里同步跑完则会直接卡死游戏。
 *
 * 旧版本的做法是开一个 ScheduledExecutorService 从别的线程发命令，
 * 那是典型的"能跑但随时会炸"——网络线程和世界访问都不是线程安全的。
 */
public final class EditQueue {

    private record Task(BlockPos pos, BlockState state) {
        boolean isBreak() {
            return state == null || state.isAir();
        }
    }

    private static final Deque<Task> TASKS = new ArrayDeque<>();
    private static int perTick = 64;
    private static int total = 0;
    private static int done = 0;
    private static int skipped = 0;
    private static String label = "";

    private EditQueue() {
    }

    public static void setPerTick(int value) {
        perTick = Math.max(1, Math.min(1024, value));
    }

    public static boolean isBusy() {
        return !TASKS.isEmpty();
    }

    public static int remaining() {
        return TASKS.size();
    }

    public static void start(String taskLabel) {
        TASKS.clear();
        label = taskLabel;
        total = 0;
        done = 0;
        skipped = 0;
    }

    public static void enqueue(BlockPos pos, BlockState state) {
        TASKS.add(new Task(pos.immutable(), state));
        total++;
    }

    public static void enqueueBreak(BlockPos pos) {
        enqueue(pos, Blocks.AIR.defaultBlockState());
    }

    public static void cancel() {
        if (TASKS.isEmpty()) return;
        TASKS.clear();
        message("§c已取消剩余操作");
    }

    /** 每个客户端 tick 调用一次。 */
    public static void tick() {
        if (TASKS.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            TASKS.clear();
            return;
        }

        boolean commands = BlockEdits.canUseCommands();
        int budget = perTick;

        while (budget-- > 0 && !TASKS.isEmpty()) {
            Task task = TASKS.poll();
            if (task == null) break;

            if (commands) {
                if (task.isBreak()) {
                    BlockEdits.setBlockViaCommand(task.pos(), Blocks.AIR.defaultBlockState());
                } else {
                    BlockEdits.setBlockViaCommand(task.pos(), task.state());
                }
                done++;
            } else if (task.isBreak()) {
                BlockEdits.sendBreakPacket(task.pos());
                done++;
            } else if (matchesHeldItem(player, task.state())) {
                if (BlockEdits.placeWithHeldItem(task.pos(), InteractionHand.MAIN_HAND)) {
                    done++;
                } else {
                    skipped++;
                }
            } else {
                skipped++;
            }
        }

        if (TASKS.isEmpty()) {
            finish();
        } else {
            // 物品栏上方的提示会互相覆盖，每 tick 刷一次就是一条进度条
            message(String.format("§7%s §f%d§7/§f%d", label, done + skipped, total));
        }
    }

    private static boolean matchesHeldItem(LocalPlayer player, BlockState state) {
        ItemStack held = player.getMainHandItem();
        return held.getItem() instanceof BlockItem item && item.getBlock() == state.getBlock();
    }

    private static void finish() {
        StringBuilder text = new StringBuilder();
        text.append("§a").append(label).append(" 完成 §f").append(done).append(" §7个方块");
        if (skipped > 0) {
            text.append(" §c(跳过 ").append(skipped).append(")");
        }
        message(text.toString());

        if (skipped > 0 && !BlockEdits.canUseCommands()) {
            message("§7无命令权限时只能放置手中的方块，其余位置已跳过");
        }
    }

    private static void message(String text) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            player.displayClientMessage(Component.literal(text), true);
        }
    }
}
