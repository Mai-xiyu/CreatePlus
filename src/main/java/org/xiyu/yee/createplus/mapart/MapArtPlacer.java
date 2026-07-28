package org.xiyu.yee.createplus.mapart;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.xiyu.yee.createplus.utils.BlockEdits;
import org.xiyu.yee.createplus.utils.CreativeGate;

import java.util.ArrayList;
import java.util.List;

/**
 * 地图画自动放置：墙 → 展示框 → 挂地图。
 * 单人/房主写入世界地图数据后他人可见；非房主联机仅自己客户端能看见画。
 */
public final class MapArtPlacer {
    private static boolean listening;
    private static Job job;
    private static Pending pending;

    private MapArtPlacer() {
    }

    public record Pending(List<ItemStack> maps, int cols, int rows, BlockState wall) {
        public Pending {
            maps = List.copyOf(maps);
            wall = wall == null ? Blocks.QUARTZ_BLOCK.defaultBlockState() : wall;
        }
    }

    private enum Phase { WALL, FRAMES, MAPS, DONE }

    private static final class Cell {
        final BlockPos wallPos;
        final BlockPos framePos;
        final ItemStack map;

        Cell(BlockPos wallPos, BlockPos framePos, ItemStack map) {
            this.wallPos = wallPos;
            this.framePos = framePos;
            this.map = map;
        }
    }

    private static final class Job {
        final List<Cell> cells;
        final Direction face;
        final BlockState wall;
        final boolean commands;
        Phase phase = Phase.WALL;
        int index;
        int waitTicks;

        Job(List<Cell> cells, Direction face, BlockState wall, boolean commands) {
            this.cells = cells;
            this.face = face;
            this.wall = wall;
            this.commands = commands;
        }
    }

    public static boolean isArmed() {
        return pending != null || job != null;
    }

    public static void arm(List<ItemStack> maps, int cols, int rows, BlockState wall) {
        if (maps == null || maps.isEmpty() || cols < 1 || rows < 1) return;
        pending = new Pending(maps, cols, rows, wall);
        job = null;
        ensureListening();
        tip("§a[地图画] §f右键点墙面锚定（" + cols + "×" + rows + "）");
    }

    public static void cancel() {
        boolean was = pending != null || job != null;
        pending = null;
        job = null;
        if (was) tip("§7[地图画] 已取消自动放置");
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

        int budget = job.commands ? 8 : 2;
        while (budget-- > 0 && job != null && job.phase != Phase.DONE) {
            step(mc);
        }
    }

    private static void step(Minecraft mc) {
        Job j = job;
        if (j == null) return;

        if (j.phase == Phase.WALL) {
            if (j.index >= j.cells.size()) {
                j.phase = Phase.FRAMES;
                j.index = 0;
                j.waitTicks = 2;
                return;
            }
            Cell cell = j.cells.get(j.index++);
            if (j.commands) {
                BlockEdits.setBlockViaCommand(cell.wallPos, j.wall);
            } else {
                giveCreative(mc, new ItemStack(j.wall.getBlock().asItem()));
                BlockEdits.placeWithHeldItem(cell.wallPos, InteractionHand.MAIN_HAND);
                j.waitTicks = 1;
            }
            return;
        }

        if (j.phase == Phase.FRAMES) {
            if (j.index >= j.cells.size()) {
                j.phase = Phase.MAPS;
                j.index = 0;
                j.waitTicks = 3;
                return;
            }
            Cell cell = j.cells.get(j.index++);
            if (j.commands) {
                int facing = j.face.get3DDataValue();
                BlockEdits.sendCommand(String.format(
                    "summon item_frame %d %d %d {Facing:%d,Fixed:1b,Invulnerable:1b}",
                    cell.framePos.getX(), cell.framePos.getY(), cell.framePos.getZ(), facing));
            } else {
                giveCreative(mc, new ItemStack(Items.ITEM_FRAME));
                clickFace(mc, cell.wallPos, j.face);
                j.waitTicks = 2;
            }
            return;
        }

        if (j.phase == Phase.MAPS) {
            if (j.index >= j.cells.size()) {
                j.phase = Phase.DONE;
                finish(j.cells.size());
                return;
            }
            Cell cell = j.cells.get(j.index++);
            if (cell.map.isEmpty()) return;
            ItemFrame frame = findFrame(mc, cell.wallPos, cell.framePos, j.face);
            if (frame == null) {
                j.index--;
                j.waitTicks = 5;
                return;
            }
            if (!frame.getItem().isEmpty()) {
                if (mc.gameMode != null && mc.player != null) {
                    mc.gameMode.attack(mc.player, frame);
                }
                j.index--;
                j.waitTicks = 4;
                return;
            }
            giveCreative(mc, cell.map.copy());
            interactEntity(mc, frame);
            j.waitTicks = j.commands ? 1 : 2;
        }
    }

    private static void finish(int n) {
        job = null;
        boolean host = Minecraft.getInstance().getSingleplayerServer() != null;
        tip("§a[地图画] §f自动放置完成（" + n + " 格）"
            + (host ? "· 已写入世界，他人可见" : "· 非房主仅自己可见；全局请用方块像素画"));
    }

    private static void startFromHit(BlockHitResult hit) {
        if (pending == null) return;
        if (!CreativeGate.allow() && !BlockEdits.canUseCommands()) {
            tip("§c[地图画] 需要创造模式（或 OP）");
            return;
        }
        Direction face = hit.getDirection();
        if (face.getAxis() == Direction.Axis.Y) {
            tip("§c[地图画] 请点侧面墙");
            return;
        }

        Direction right = rightOf(face);
        BlockPos origin = hit.getBlockPos();
        List<Cell> cells = new ArrayList<>(pending.cols() * pending.rows());
        int i = 0;
        for (int row = 0; row < pending.rows(); row++) {
            for (int col = 0; col < pending.cols(); col++) {
                int up = pending.rows() - 1 - row;
                BlockPos wallPos = origin.relative(right, col).relative(Direction.UP, up);
                BlockPos framePos = wallPos.relative(face);
                ItemStack map = i < pending.maps().size()
                    ? pending.maps().get(i).copy() : ItemStack.EMPTY;
                cells.add(new Cell(wallPos, framePos, map));
                i++;
            }
        }

        boolean commands = BlockEdits.canUseCommands();
        job = new Job(cells, face, pending.wall(), commands);
        pending = null;
        tip("§a[地图画] §f开始放置 " + cells.size() + " 格…");
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

    private static void clickFace(Minecraft mc, BlockPos wallPos, Direction face) {
        if (mc.player == null || mc.gameMode == null) return;
        Vec3 hit = Vec3.atCenterOf(wallPos).add(
            face.getStepX() * 0.51, face.getStepY() * 0.51, face.getStepZ() * 0.51);
        mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND,
            new BlockHitResult(hit, face, wallPos, false));
    }

    private static void interactEntity(Minecraft mc, Entity entity) {
        if (mc.player == null || mc.gameMode == null) return;
        mc.gameMode.interact(mc.player, entity, InteractionHand.MAIN_HAND);
    }

    private static ItemFrame findFrame(Minecraft mc, BlockPos wallPos, BlockPos framePos, Direction face) {
        if (mc.level == null) return null;
        AABB box = new AABB(wallPos).minmax(new AABB(framePos)).inflate(0.6);
        List<ItemFrame> list = mc.level.getEntitiesOfClass(ItemFrame.class, box, f -> {
            if (f.getDirection() != face) return false;
            BlockPos hang = f.getPos();
            return hang.equals(wallPos) || hang.equals(framePos)
                || f.blockPosition().equals(framePos) || f.blockPosition().equals(wallPos);
        });
        return list.isEmpty() ? null : list.get(0);
    }

    private static void ensureListening() {
        if (listening) return;
        listening = true;
        MinecraftForge.EVENT_BUS.register(MapArtPlacer.class);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        if (pending == null && job == null) return;
        if (!event.isUseItem()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null || mc.player == null) return;
        if (pending != null && mc.hitResult instanceof BlockHitResult blockHit
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
