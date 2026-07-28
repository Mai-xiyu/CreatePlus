package org.xiyu.yee.createplus.utils;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.function.BiConsumer;

/**
 * 方块编辑的统一入口。
 *
 * 有权限时走 setblock/fill 命令（准确、能保留方块状态），
 * 没权限时退回到模拟"用手里的方块右键"的数据包。
 */
public final class BlockEdits {

    /** 原版 {@code /fill} 单条命令硬上限。 */
    public static final int FILL_COMMAND_MAX = 32768;

    /** 可调上限的默认值（一亿）。 */
    public static final int DEFAULT_MAX_VOLUME = 100_000_000;

    /** 可调上限的最大值（一亿）。 */
    public static final int ABSOLUTE_MAX_VOLUME = 100_000_000;

    /** 可写入撤销历史的最大格数，过大易爆内存。 */
    public static final int HISTORY_VOLUME = 32768;

    private static int maxVolume = DEFAULT_MAX_VOLUME;

    private BlockEdits() {
    }

    /**
     * 客户端单次操作软上限。超过 {@link #FILL_COMMAND_MAX} 时用多批 fill / EditQueue 拆开。
     * 可在「区域填充」设置里改，或 {@code /createplus maxvolume}。
     */
    public static int maxVolume() {
        return maxVolume;
    }

    public static void setMaxVolume(int value) {
        if (value < FILL_COMMAND_MAX) value = FILL_COMMAND_MAX;
        if (value > ABSOLUTE_MAX_VOLUME) value = ABSOLUTE_MAX_VOLUME;
        maxVolume = value;
    }

    /** 玩家是否有权限使用 setblock / fill 这类命令。 */
    public static boolean canUseCommands() {
        LocalPlayer player = Minecraft.getInstance().player;
        return player != null && player.hasPermissions(2);
    }

    /** 把方块状态序列化成命令里能用的形式，例如 minecraft:oak_stairs[facing=north,half=top]。 */
    public static String toCommandString(BlockState state) {
        String id = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        if (state.getProperties().isEmpty()) return id;

        StringBuilder builder = new StringBuilder(id);
        builder.append('[');
        boolean first = true;
        for (Property<?> property : state.getProperties()) {
            if (!first) builder.append(',');
            first = false;
            builder.append(property.getName()).append('=').append(valueOf(state, property));
        }
        builder.append(']');
        return builder.toString();
    }

    private static <T extends Comparable<T>> String valueOf(BlockState state, Property<T> property) {
        return property.getName(state.getValue(property));
    }

    public static void sendCommand(String command) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && player.connection != null) {
            player.connection.sendCommand(command);
        }
    }

    /** 用命令设置一个方块。调用方需要先确认 {@link #canUseCommands()}。 */
    public static void setBlockViaCommand(BlockPos pos, BlockState state) {
        sendCommand(String.format("setblock %d %d %d %s",
            pos.getX(), pos.getY(), pos.getZ(), toCommandString(state)));
    }

    /** 用 fill 命令填充一个长方体。单区体积须 ≤ {@link #FILL_COMMAND_MAX}。 */
    public static void fillViaCommand(BlockPos from, BlockPos to, BlockState state, String mode) {
        sendCommand(String.format("fill %d %d %d %d %d %d %s%s",
            from.getX(), from.getY(), from.getZ(),
            to.getX(), to.getY(), to.getZ(),
            toCommandString(state),
            mode == null || mode.isEmpty() ? "" : " " + mode));
    }

    /**
     * 把大长方体切成体积 ≤ {@code limit} 的子块（优先沿最长轴切开），
     * 以便多条 {@code /fill} 突破原版单次 32768 限制。
     */
    public static void forEachFillChunk(BlockPos min, BlockPos max, int limit,
                                        BiConsumer<BlockPos, BlockPos> consumer) {
        if (limit < 1) limit = FILL_COMMAND_MAX;
        BlockPos a = min(min, max);
        BlockPos b = max(min, max);
        long volume = volumeOf(a, b);
        if (volume <= limit) {
            consumer.accept(a, b);
            return;
        }

        int dx = b.getX() - a.getX() + 1;
        int dy = b.getY() - a.getY() + 1;
        int dz = b.getZ() - a.getZ() + 1;

        // 选切片方向：尽量让「另外两维乘积」小，从而每片能切得更厚
        int axis; // 0=X 1=Y 2=Z
        long sliceArea;
        if (dx >= dy && dx >= dz) {
            axis = 0;
            sliceArea = (long) dy * dz;
        } else if (dz >= dy) {
            axis = 2;
            sliceArea = (long) dx * dy;
        } else {
            axis = 1;
            sliceArea = (long) dx * dz;
        }

        int step = (int) Math.max(1L, Math.min(Integer.MAX_VALUE, limit / Math.max(1L, sliceArea)));
        if (sliceArea > limit) {
            // 单层仍超限：递归切另外的轴
            if (axis == 0) {
                for (int x = a.getX(); x <= b.getX(); x++) {
                    forEachFillChunk(new BlockPos(x, a.getY(), a.getZ()),
                        new BlockPos(x, b.getY(), b.getZ()), limit, consumer);
                }
            } else if (axis == 1) {
                for (int y = a.getY(); y <= b.getY(); y++) {
                    forEachFillChunk(new BlockPos(a.getX(), y, a.getZ()),
                        new BlockPos(b.getX(), y, b.getZ()), limit, consumer);
                }
            } else {
                for (int z = a.getZ(); z <= b.getZ(); z++) {
                    forEachFillChunk(new BlockPos(a.getX(), a.getY(), z),
                        new BlockPos(b.getX(), b.getY(), z), limit, consumer);
                }
            }
            return;
        }

        if (axis == 0) {
            for (int x = a.getX(); x <= b.getX(); x += step) {
                int x1 = Math.min(x + step - 1, b.getX());
                consumer.accept(new BlockPos(x, a.getY(), a.getZ()),
                    new BlockPos(x1, b.getY(), b.getZ()));
            }
        } else if (axis == 1) {
            for (int y = a.getY(); y <= b.getY(); y += step) {
                int y1 = Math.min(y + step - 1, b.getY());
                consumer.accept(new BlockPos(a.getX(), y, a.getZ()),
                    new BlockPos(b.getX(), y1, b.getZ()));
            }
        } else {
            for (int z = a.getZ(); z <= b.getZ(); z += step) {
                int z1 = Math.min(z + step - 1, b.getZ());
                consumer.accept(new BlockPos(a.getX(), a.getY(), z),
                    new BlockPos(b.getX(), b.getY(), z1));
            }
        }
    }

    /** 估算需要多少条 fill 才能铺满该区。 */
    public static int countFillChunks(BlockPos min, BlockPos max, int limit) {
        int[] count = {0};
        forEachFillChunk(min, max, limit, (a, b) -> count[0]++);
        return count[0];
    }

    /**
     * 通过数据包在指定位置放置手上的方块。
     *
     * 会挑一个相邻的实心方块作为支撑面，这样服务端才会接受这次放置；
     * 找不到支撑面时返回 false。
     */
    public static boolean placeWithHeldItem(BlockPos target, InteractionHand hand) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null || player.connection == null) return false;

        for (Direction direction : Direction.values()) {
            BlockPos support = target.relative(direction);
            if (mc.level.getBlockState(support).isAir()) continue;

            Direction face = direction.getOpposite();
            Vec3 hit = Vec3.atCenterOf(support).add(
                face.getStepX() * 0.5D, face.getStepY() * 0.5D, face.getStepZ() * 0.5D);

            player.connection.send(new ServerboundUseItemOnPacket(
                hand, new BlockHitResult(hit, face, support, false), 0));
            return true;
        }
        return false;
    }

    /** 创造模式瞬间破坏一个方块。 */
    public static void breakBlock(BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.gameMode == null || mc.level == null) return;
        if (mc.level.getBlockState(pos).isAir()) return;
        mc.gameMode.destroyBlock(pos);
    }

    /** 直接发送破坏数据包，用于 gameMode 不可用的场合。 */
    public static void sendBreakPacket(BlockPos pos) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || player.connection == null) return;
        player.connection.send(new ServerboundPlayerActionPacket(
            ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, pos, Direction.UP));
        player.connection.send(new ServerboundPlayerActionPacket(
            ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, pos, Direction.UP));
    }

    /** 长方体包含的方块总数。 */
    public static long volumeOf(BlockPos a, BlockPos b) {
        long dx = Math.abs(a.getX() - b.getX()) + 1L;
        long dy = Math.abs(a.getY() - b.getY()) + 1L;
        long dz = Math.abs(a.getZ() - b.getZ()) + 1L;
        return dx * dy * dz;
    }

    public static BlockPos min(BlockPos a, BlockPos b) {
        return new BlockPos(
            Math.min(a.getX(), b.getX()),
            Math.min(a.getY(), b.getY()),
            Math.min(a.getZ(), b.getZ()));
    }

    public static BlockPos max(BlockPos a, BlockPos b) {
        return new BlockPos(
            Math.max(a.getX(), b.getX()),
            Math.max(a.getY(), b.getY()),
            Math.max(a.getZ(), b.getZ()));
    }
}
