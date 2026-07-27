package org.xiyu.yee.createplus.features;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.xiyu.yee.createplus.api.NoClipAPI;
import org.xiyu.yee.createplus.settings.BooleanSetting;
import org.xiyu.yee.createplus.settings.DoubleSetting;
import org.xiyu.yee.createplus.utils.CreativeGate;

/**
 * 穿墙：跳过本地玩家及本机主机 ServerPlayer 的方块碰撞。
 *
 * 远端多人服没有服务端 mixin，靠"忽略拉回包 + 持续上报坐标"维持客户端位置；
 * 对弱校验/创造服通常有效，严格反作弊仍可能踢人。
 */
public class NoClip extends CreativePlusFeature {
    private static final double BASE_SPEED = 0.6D;

    private final DoubleSetting speed = doubleSetting("speed", 1.0D, 0.2D, 5.0D, 0.1D);
    private final BooleanSetting keepFlying = boolSetting("keep_flying", true);
    private final BooleanSetting ignoreRubberband = boolSetting("ignore_rubberband", true);

    private boolean savedFlying;
    private boolean warnedCreative;

    public NoClip() {
        super("noclip", "穿墙移动，忽略方块碰撞（需创造模式）", Category.MOVEMENT);
        ignoreRubberband.onChange(v -> NoClipAPI.setIgnoreRubberband(isEnabled() && v && CreativeGate.allow()));
    }

    @Override
    public void onEnable() {
        warnedCreative = false;
        if (!CreativeGate.allow()) {
            notifyPlayer("§c[CreatePlus] §7穿墙只在创造模式下生效");
            return;
        }
        activate();
        notifyPlayer("§b[CreatePlus] §7穿墙已开启（创造）· 本机主机与多人服均会尽量生效");
    }

    @Override
    public void onWorldJoin() {
        warnedCreative = false;
        if (isEnabled() && CreativeGate.allow()) activate();
    }

    @Override
    public void onDisable() {
        deactivate();
    }

    private void activate() {
        NoClipAPI.setActive(true);
        NoClipAPI.setIgnoreRubberband(ignoreRubberband.get());
        LocalPlayer player = player();
        if (player != null) {
            savedFlying = player.getAbilities().flying;
            applyClient(player);
        }
        applyIntegrated();
    }

    private void deactivate() {
        NoClipAPI.setActive(false);
        NoClipAPI.setIgnoreRubberband(false);
        LocalPlayer player = player();
        if (player != null) {
            player.noPhysics = false;
            player.getAbilities().flying = savedFlying;
            player.onUpdateAbilities();
        }
        ServerPlayer server = CreativeGate.integratedServerPlayer();
        if (server != null) {
            server.noPhysics = false;
        }
    }

    private void applyClient(LocalPlayer player) {
        player.noPhysics = true;
        if (keepFlying.get()) {
            player.getAbilities().mayfly = true;
            player.getAbilities().flying = true;
        }
    }

    /** 本机主机：同步改 ServerPlayer，否则逻辑服务端仍按碰撞结算。 */
    private void applyIntegrated() {
        ServerPlayer server = CreativeGate.integratedServerPlayer();
        if (server == null) return;
        server.noPhysics = true;
        if (keepFlying.get()) {
            server.getAbilities().mayfly = true;
            server.getAbilities().flying = true;
        }
        server.fallDistance = 0.0F;
    }

    @Override
    public void onTick() {
        if (!CreativeGate.allow()) {
            if (NoClipAPI.isActive()) {
                NoClipAPI.setActive(false);
                NoClipAPI.setIgnoreRubberband(false);
            }
            LocalPlayer player = player();
            if (player != null) player.noPhysics = false;
            if (!warnedCreative) {
                warnedCreative = true;
                notifyActionBar("§c穿墙需要创造模式");
            }
            return;
        }
        warnedCreative = false;

        if (!NoClipAPI.isActive()) activate();

        LocalPlayer player = player();
        if (player == null) return;

        applyClient(player);
        applyIntegrated();
        player.fallDistance = 0.0F;

        if (speed.value() > 1.0D) {
            player.setDeltaMovement(boost(player.getDeltaMovement(), speed.value()));
        }

        // 远端服：主动上报当前位置，配合忽略拉回，避免一钻墙就被拽走
        if (!Minecraft.getInstance().hasSingleplayerServer() && player.connection != null) {
            player.connection.send(new ServerboundMovePlayerPacket.PosRot(
                player.getX(), player.getY(), player.getZ(),
                player.getYRot(), player.getXRot(), false));
        }
    }

    private static Vec3 boost(Vec3 motion, double multiplier) {
        double x = motion.x * multiplier;
        double z = motion.z * multiplier;
        double horizontal = Math.sqrt(x * x + z * z);
        double max = BASE_SPEED * multiplier;
        if (horizontal > max && horizontal > 1.0E-6D) {
            double scale = max / horizontal;
            x *= scale;
            z *= scale;
        }
        return new Vec3(x, motion.y, z);
    }

    @Override
    public String getDetailedDescription() {
        boolean creative = CreativeGate.allow();
        return getTranslatedDescription()
            + String.format("\n§7速度: §f%.1fx", speed.value())
            + "\n§7忽略拉回: §f" + (ignoreRubberband.get() ? "开" : "关")
            + (creative ? "\n§a创造模式 · 已激活" : "\n§c非创造 · 已暂停");
    }
}
