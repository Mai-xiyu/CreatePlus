package org.xiyu.yee.createplus.utils;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * 导入 .mcfunction 建筑：以执行 import 时的玩家坐标为固定原点预览，
 * 确认后把相对坐标写成绝对坐标再分批下发，避免玩家走动导致偏移。
 */
public final class BuildingImport {
    private static final int COMMANDS_PER_TICK = 8;

    private static final List<String> pending = new ArrayList<>();
    private static final Deque<String> running = new ArrayDeque<>();

    private static BlockPos origin;
    private static AABB previewBox;
    private static String name = "";
    private static int total;
    private static int sent;
    /** 文件用的是 ~ 相对坐标还是绝对坐标，决定预览框要不要加上玩家位置。 */
    private static boolean relative = true;

    private BuildingImport() {
    }

    public static boolean hasPreview() {
        return previewBox != null && running.isEmpty() && !pending.isEmpty();
    }

    public static boolean isRunning() {
        return !running.isEmpty();
    }

    public static String getName() {
        return name;
    }

    public static BlockPos getOrigin() {
        return origin;
    }

    public static int size() {
        return pending.size();
    }

    /**
     * 载入文件并生成预览。原点锁定为当前玩家脚下坐标。
     *
     * @return 读到的方块数量，失败时返回 -1
     */
    public static int preview(Path file, String displayName) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return -1;

        List<String> lines;
        try {
            lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return -1;
        }

        cancel();
        name = displayName;
        origin = player.blockPosition();

        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;

        for (String raw : lines) {
            String line = raw.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            if (line.startsWith("/")) line = line.substring(1);
            if (!line.startsWith("setblock ")) continue;

            String[] parts = line.split(" ");
            if (parts.length < 5) continue;

            try {
                int x = parseCoord(parts[1]);
                int y = parseCoord(parts[2]);
                int z = parseCoord(parts[3]);
                if (pending.isEmpty()) {
                    relative = parts[1].startsWith("~")
                        || parts[2].startsWith("~")
                        || parts[3].startsWith("~");
                }
                minX = Math.min(minX, x);
                minY = Math.min(minY, y);
                minZ = Math.min(minZ, z);
                maxX = Math.max(maxX, x);
                maxY = Math.max(maxY, y);
                maxZ = Math.max(maxZ, z);
            } catch (NumberFormatException e) {
                continue;
            }
            pending.add(line);
        }

        if (pending.isEmpty()) {
            previewBox = null;
            return 0;
        }

        // 绝对坐标的文件不能再叠加玩家位置，否则预览框会飘到几百格外
        int baseX = relative ? origin.getX() : 0;
        int baseY = relative ? origin.getY() : 0;
        int baseZ = relative ? origin.getZ() : 0;
        previewBox = new AABB(
            baseX + minX, baseY + minY, baseZ + minZ,
            baseX + maxX + 1.0D, baseY + maxY + 1.0D, baseZ + maxZ + 1.0D);
        return pending.size();
    }

    /** 相对坐标写作 ~12，绝对坐标就是普通数字。 */
    private static int parseCoord(String token) {
        String value = token.startsWith("~") ? token.substring(1) : token;
        return value.isEmpty() ? 0 : Integer.parseInt(value);
    }

    /**
     * 确认建造：把相对 setblock 钉死到预览原点上的绝对坐标，
     * 之后玩家怎么走都不影响落点。
     */
    public static void confirm() {
        if (pending.isEmpty() || origin == null) return;

        running.clear();
        if (relative) {
            for (String line : pending) {
                running.add(toAbsolute(line, origin));
            }
        } else {
            running.addAll(pending);
        }
        total = running.size();
        sent = 0;
        pending.clear();
    }

    /** setblock ~x ~y ~z block… → setblock absX absY absZ block… */
    private static String toAbsolute(String line, BlockPos base) {
        String[] parts = line.split(" ", 5);
        if (parts.length < 5) return line;
        int x = base.getX() + parseCoord(parts[1]);
        int y = base.getY() + parseCoord(parts[2]);
        int z = base.getZ() + parseCoord(parts[3]);
        return "setblock " + x + " " + y + " " + z + " " + parts[4];
    }

    public static void cancel() {
        pending.clear();
        running.clear();
        previewBox = null;
        origin = null;
        total = 0;
        sent = 0;
        relative = true;
    }

    public static void tick() {
        if (running.isEmpty()) return;

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || player.connection == null) {
            cancel();
            return;
        }

        for (int i = 0; i < COMMANDS_PER_TICK && !running.isEmpty(); i++) {
            String command = running.poll();
            if (command == null) break;
            player.connection.sendCommand(command);
            sent++;
        }

        if (running.isEmpty()) {
            player.displayClientMessage(Component.literal(
                "§a建筑 §f" + name + " §a导入完成（" + sent + " 个方块）"), true);
            previewBox = null;
            origin = null;
        } else {
            player.displayClientMessage(Component.literal(
                String.format("§7导入中 §f%d§7/§f%d", sent, total)), true);
        }
    }

    public static void render(PoseStack poseStack, Camera camera) {
        if (previewBox == null) return;

        RenderUtils.drawWorldFilledBox(poseStack, camera, previewBox, 0.2F, 1.0F, 0.4F, 0.12F);
        RenderUtils.drawWorldBox(poseStack, camera, previewBox, 0.2F, 1.0F, 0.4F, 0.9F);

        double sizeX = previewBox.maxX - previewBox.minX;
        double sizeY = previewBox.maxY - previewBox.minY;
        double sizeZ = previewBox.maxZ - previewBox.minZ;
        String originText = origin == null ? ""
            : String.format("  原点 %d %d %d", origin.getX(), origin.getY(), origin.getZ());
        RenderUtils.drawWorldLabel(poseStack, camera,
            String.format("%s  %.0fx%.0fx%.0f%s", name, sizeX, sizeY, sizeZ, originText),
            previewBox.getCenter().x, previewBox.maxY + 0.5D, previewBox.getCenter().z,
            0xFFFFFFFF);
    }
}
