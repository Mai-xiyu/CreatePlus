package org.xiyu.yee.createplus.utils;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 区域操作的撤销历史。
 *
 * 每次批量编辑之前先把原始方块状态抄下来，撤销时按原状态写回去。
 * 写回依赖 setblock 命令，所以撤销只对有权限的玩家有效 —— 没有命令权限时
 * 客户端根本无法凭空恢复任意方块状态，与其假装成功不如明确告诉玩家。
 */
public final class EditHistory {
    private static final int MAX_STEPS = 8;
    private static final Deque<Step> STEPS = new ArrayDeque<>();

    private EditHistory() {
    }

    public static final class Step {
        private final String label;
        private final Map<BlockPos, BlockState> before = new LinkedHashMap<>();

        private Step(String label) {
            this.label = label;
        }

        public void record(BlockPos pos, BlockState state) {
            before.putIfAbsent(pos.immutable(), state);
        }

        public int size() {
            return before.size();
        }

        public String getLabel() {
            return label;
        }

        public Map<BlockPos, BlockState> getBefore() {
            return before;
        }
    }

    public static Step begin(String label) {
        return new Step(label);
    }

    public static void commit(Step step) {
        if (step == null || step.size() == 0) return;
        STEPS.push(step);
        while (STEPS.size() > MAX_STEPS) {
            STEPS.removeLast();
        }
    }

    public static boolean isEmpty() {
        return STEPS.isEmpty();
    }

    public static int depth() {
        return STEPS.size();
    }

    /** 弹出最近一步，交给调用方决定怎么写回。 */
    public static Step pop() {
        return STEPS.poll();
    }

    public static void clear() {
        STEPS.clear();
    }
}
