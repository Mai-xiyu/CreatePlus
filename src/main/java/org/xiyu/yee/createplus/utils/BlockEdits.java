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

/**
 * 方块编辑的统一入口。
 *
 * 有权限时走 setblock/fill 命令（准确、能保留方块状态），
 * 没权限时退回到模拟"用手里的方块右键"的数据包。
 */
public final class BlockEdits {

    /** 单次操作允许影响的最大方块数，防止手滑把服务器刷崩。 */
    public static final int MAX_VOLUME = 32768;

    private BlockEdits() {
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

    /** 用 fill 命令填充一个长方体。 */
    public static void fillViaCommand(BlockPos from, BlockPos to, BlockState state, String mode) {
        sendCommand(String.format("fill %d %d %d %d %d %d %s%s",
            from.getX(), from.getY(), from.getZ(),
            to.getX(), to.getY(), to.getZ(),
            toCommandString(state),
            mode == null || mode.isEmpty() ? "" : " " + mode));
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
