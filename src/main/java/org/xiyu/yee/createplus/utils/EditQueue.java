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
 * 支持逐格 setblock，也支持分批 {@code /fill}（突破原版单次 32768）。
 */
public final class EditQueue {

    private record Task(BlockPos from, BlockPos to, BlockState state, String fillMode) {
        static Task block(BlockPos pos, BlockState state) {
            return new Task(pos, null, state, null);
        }

        static Task fill(BlockPos from, BlockPos to, BlockState state, String fillMode) {
            return new Task(from, to, state, fillMode);
        }

        boolean isRegionFill() {
            return to != null;
        }

        boolean isBreak() {
            return state == null || state.isAir();
        }
    }

    private static final Deque<Task> TASKS = new ArrayDeque<>();
    private static int perTick = 64;
    /** 每 tick 最多下发几条 fill（fill 服务端很重）。 */
    private static final int FILLS_PER_TICK = 2;
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
        TASKS.add(Task.block(pos.immutable(), state));
        total++;
    }

    public static void enqueueBreak(BlockPos pos) {
        enqueue(pos, Blocks.AIR.defaultBlockState());
    }

    /** 排队一条区域 fill（体积须已按 {@link BlockEdits#FILL_COMMAND_MAX} 切好）。 */
    public static void enqueueFill(BlockPos from, BlockPos to, BlockState state, String fillMode) {
        TASKS.add(Task.fill(from.immutable(), to.immutable(), state, fillMode));
        total++;
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
        int blockBudget = perTick;
        int fillBudget = FILLS_PER_TICK;

        while (!TASKS.isEmpty()) {
            Task peek = TASKS.peek();
            if (peek == null) break;

            if (peek.isRegionFill()) {
                if (fillBudget-- <= 0) break;
                Task task = TASKS.poll();
                if (task == null) break;
                if (commands) {
                    BlockEdits.fillViaCommand(task.from(), task.to(), task.state(), task.fillMode());
                    done++;
                } else {
                    skipped++;
                }
                continue;
            }

            if (blockBudget-- <= 0) break;
            Task task = TASKS.poll();
            if (task == null) break;

            if (commands) {
                if (task.isBreak()) {
                    BlockEdits.setBlockViaCommand(task.from(), Blocks.AIR.defaultBlockState());
                } else {
                    BlockEdits.setBlockViaCommand(task.from(), task.state());
                }
                done++;
            } else if (task.isBreak()) {
                BlockEdits.sendBreakPacket(task.from());
                done++;
            } else if (matchesHeldItem(player, task.state())) {
                if (BlockEdits.placeWithHeldItem(task.from(), InteractionHand.MAIN_HAND)) {
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
            message(String.format("§7%s §f%d§7/§f%d", label, done + skipped, total));
        }
    }

    private static boolean matchesHeldItem(LocalPlayer player, BlockState state) {
        ItemStack held = player.getMainHandItem();
        return held.getItem() instanceof BlockItem item && item.getBlock() == state.getBlock();
    }

    private static void finish() {
        StringBuilder text = new StringBuilder();
        text.append("§a").append(label).append(" 完成 §f").append(done).append(" §7步");
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
