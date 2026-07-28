package org.xiyu.yee.createplus.mapart;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.xiyu.yee.createplus.utils.BlockEdits;
import org.xiyu.yee.createplus.utils.CreativeGate;
import org.xiyu.yee.createplus.utils.EditHistory;
import org.xiyu.yee.createplus.utils.EditQueue;

/**
 * 方块像素画放置：右键锚定，按网格放置实心方块（原版全局可见）。
 * <ul>
 *   <li>点侧面：竖着铺（右 / 上）</li>
 *   <li>点顶面/底面：平铺在地板或天花板（右 / 前，跟玩家朝向）</li>
 * </ul>
 */
public final class BlockPixelPlacer {
    private static boolean listening;
    private static Pending pending;
    private static Job job;

    private BlockPixelPlacer() {
    }

    public record Pending(BlockPixelArt.Grid grid) {
    }

    private static final class Job {
        final BlockPos[] positions;
        final BlockState[] states;
        int index;
        int waitTicks;
        final boolean commands;

        Job(BlockPos[] positions, BlockState[] states, boolean commands) {
            this.positions = positions;
            this.states = states;
            this.commands = commands;
        }
    }

    public static boolean isArmed() {
        return pending != null || job != null;
    }

    public static void arm(BlockPixelArt.Grid grid) {
        if (grid == null || grid.width() < 1 || grid.height() < 1) return;
        pending = new Pending(grid);
        job = null;
        ensureListening();
        tip("§a[像素画] §f右键放置 " + grid.width() + "×" + grid.height()
            + " · 点侧面竖着 / 点地板或天花板平铺");
    }

    public static void cancel() {
        boolean was = pending != null || job != null;
        pending = null;
        job = null;
        if (was) tip("§7[像素画] 已取消");
    }

    public static void tick() {
        if (job == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            job = null;
            return;
        }
        if (job.waitTicks > 0) {
            job.waitTicks--;
            return;
        }

        if (job.commands) {
            if (!EditQueue.isBusy()) {
                finish(job.positions.length);
            }
            return;
        }

        int budget = 4;
        while (budget-- > 0 && job != null && job.index < job.positions.length) {
            BlockState state = job.states[job.index];
            BlockPos pos = job.positions[job.index];
            job.index++;
            if (state == null || state.isAir()) continue;
            giveCreative(mc, new ItemStack(state.getBlock().asItem()));
            BlockEdits.placeWithHeldItem(pos, InteractionHand.MAIN_HAND);
        }
        if (job != null && job.index >= job.positions.length) {
            finish(job.positions.length);
        }
    }

    private static void startFromHit(BlockHitResult hit) {
        if (pending == null) return;
        if (!CreativeGate.allow() && !BlockEdits.canUseCommands()) {
            tip("§c[像素画] 需要创造模式（或 OP）");
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        BlockPixelArt.Grid grid = pending.grid();
        Direction face = hit.getDirection();
        BlockPos origin = hit.getBlockPos();
        int w = grid.width();
        int h = grid.height();
        BlockPos[] positions = new BlockPos[w * h];
        BlockState[] states = new BlockState[w * h];

        if (face.getAxis() == Direction.Axis.Y) {
            Direction forward = mc.player.getDirection();
            Direction right = forward.getClockWise();
            int i = 0;
            for (int row = 0; row < h; row++) {
                for (int col = 0; col < w; col++) {
                    int depth = h - 1 - row;
                    BlockPos pos = origin.relative(right, col).relative(forward, depth);
                    BlockState state = grid.get(col, row);
                    positions[i] = pos;
                    states[i] = state == null ? Blocks.AIR.defaultBlockState() : state;
                    i++;
                }
            }
        } else {
            Direction right = rightOf(face);
            int i = 0;
            for (int row = 0; row < h; row++) {
                for (int col = 0; col < w; col++) {
                    int up = h - 1 - row;
                    BlockPos pos = origin.relative(right, col).relative(Direction.UP, up);
                    BlockState state = grid.get(col, row);
                    positions[i] = pos;
                    states[i] = state == null ? Blocks.AIR.defaultBlockState() : state;
                    i++;
                }
            }
        }

        boolean commands = BlockEdits.canUseCommands();
        pending = null;

        if (commands) {
            EditHistory.Step step = EditHistory.begin("方块像素画");
            EditQueue.start("方块像素画");
            int queued = 0;
            for (int n = 0; n < positions.length; n++) {
                if (states[n].isAir()) continue;
                step.record(positions[n], mc.level.getBlockState(positions[n]));
                EditQueue.enqueue(positions[n], states[n]);
                queued++;
            }
            EditHistory.commit(step);
            job = new Job(positions, states, true);
            tip("§a[像素画] §f已排队 " + queued + " 格（"
                + (face.getAxis() == Direction.Axis.Y ? "平铺" : "竖放")
                + "·命令·全局可见）…");
        } else {
            job = new Job(positions, states, false);
            tip("§a[像素画] §f开始放置 " + positions.length + " 格（"
                + (face.getAxis() == Direction.Axis.Y ? "平铺" : "竖放")
                + "·创造·全局可见）…");
        }
    }

    static Direction rightOf(Direction face) {
        return switch (face) {
            case NORTH -> Direction.WEST;
            case SOUTH -> Direction.EAST;
            case WEST -> Direction.SOUTH;
            case EAST -> Direction.NORTH;
            default -> Direction.EAST;
        };
    }

    private static void giveCreative(Minecraft mc, ItemStack stack) {
        if (mc.player == null || mc.gameMode == null) return;
        int slot = mc.player.getInventory().selected;
        mc.gameMode.handleCreativeModeItemAdd(stack.copy(), 36 + slot);
        mc.player.getInventory().setItem(slot, stack.copy());
    }

    private static void finish(int n) {
        job = null;
        tip("§a[像素画] §f放置完成（约 " + n + " 格）· 所有玩家可见");
    }

    private static void ensureListening() {
        if (listening) return;
        listening = true;
        MinecraftForge.EVENT_BUS.register(BlockPixelPlacer.class);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        if (pending == null) return;
        if (!event.isUseItem()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null || mc.player == null) return;
        if (mc.hitResult instanceof BlockHitResult blockHit
            && blockHit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
            event.setCanceled(true);
            event.setSwingHand(false);
            startFromHit(blockHit);
        }
    }

    private static void tip(String msg) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            player.displayClientMessage(Component.literal(msg), false);
        }
    }
}
