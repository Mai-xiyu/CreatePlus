package org.xiyu.yee.createplus.features;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.common.ForgeMod;
import org.xiyu.yee.createplus.api.ReachAPI;
import org.xiyu.yee.createplus.settings.DoubleSetting;
import org.xiyu.yee.createplus.utils.CreativeGate;

import java.util.UUID;

/**
 * 加长交互距离（需创造模式）。
 *
 * 客户端走 Forge reach 属性；本机主机同时写到 ServerPlayer，否则逻辑服仍按原版 5 格拒包。
 * 远端多人服依赖服务端是否认可超距包 —— 创造服通常放宽，严格反作弊仍可能拒绝。
 */
public class ExtendedReach extends CreativePlusFeature {
    private static final UUID BLOCK_REACH_ID = UUID.fromString("5d4a1f6b-8f2c-4f0e-9c11-1a0f2b7c3d41");
    private static final UUID ENTITY_REACH_ID = UUID.fromString("5d4a1f6b-8f2c-4f0e-9c11-1a0f2b7c3d42");
    private static final double VANILLA_BLOCK = 4.5D;
    private static final double VANILLA_ENTITY = 3.0D;

    private final DoubleSetting blockBonus = doubleSetting("block_bonus", 8.0D, 0.0D, 64.0D, 0.5D);
    private final DoubleSetting entityBonus = doubleSetting("entity_bonus", 5.0D, 0.0D, 64.0D, 0.5D);

    private boolean warnedCreative;

    public ExtendedReach() {
        super("extended_reach", "加长方块与实体的交互距离（需创造模式）", Category.MOVEMENT);
        blockBonus.onChange(value -> reapply());
        entityBonus.onChange(value -> reapply());
    }

    @Override
    public void onEnable() {
        warnedCreative = false;
        if (!CreativeGate.allow()) {
            notifyPlayer("§c[CreatePlus] §7超距交互只在创造模式下生效");
            return;
        }
        apply();
        notifyPlayer("§b[CreatePlus] §7超距交互已开启（创造）");
    }

    @Override
    public void onWorldJoin() {
        warnedCreative = false;
        if (isEnabled() && CreativeGate.allow()) apply();
    }

    @Override
    public void onDisable() {
        remove();
        ReachAPI.setActive(false);
    }

    @Override
    public void onTick() {
        if (!CreativeGate.allow()) {
            if (ReachAPI.isActive()) {
                remove();
                ReachAPI.setActive(false);
            }
            if (!warnedCreative) {
                warnedCreative = true;
                notifyActionBar("§c超距交互需要创造模式");
            }
            return;
        }
        warnedCreative = false;
        if (!ReachAPI.isActive()) apply();
        // 本机主机每 tick 保底同步一次，防止属性被其它逻辑清掉
        applyIntegrated();
        publishApi();
    }

    private void reapply() {
        if (!isEnabled()) return;
        remove();
        if (CreativeGate.allow()) apply();
    }

    private void apply() {
        LocalPlayer player = player();
        if (player == null) return;

        addModifier(player.getAttribute(ForgeMod.BLOCK_REACH.get()),
            BLOCK_REACH_ID, "createplus_block_reach", blockBonus.value());
        addModifier(player.getAttribute(ForgeMod.ENTITY_REACH.get()),
            ENTITY_REACH_ID, "createplus_entity_reach", entityBonus.value());
        applyIntegrated();
        publishApi();
        ReachAPI.setActive(true);
    }

    private void applyIntegrated() {
        ServerPlayer server = CreativeGate.integratedServerPlayer();
        if (server == null) return;
        addModifier(server.getAttribute(ForgeMod.BLOCK_REACH.get()),
            BLOCK_REACH_ID, "createplus_block_reach", blockBonus.value());
        addModifier(server.getAttribute(ForgeMod.ENTITY_REACH.get()),
            ENTITY_REACH_ID, "createplus_entity_reach", entityBonus.value());
    }

    private void remove() {
        LocalPlayer player = player();
        if (player != null) {
            removeModifier(player.getAttribute(ForgeMod.BLOCK_REACH.get()), BLOCK_REACH_ID);
            removeModifier(player.getAttribute(ForgeMod.ENTITY_REACH.get()), ENTITY_REACH_ID);
        }
        ServerPlayer server = CreativeGate.integratedServerPlayer();
        if (server != null) {
            removeModifier(server.getAttribute(ForgeMod.BLOCK_REACH.get()), BLOCK_REACH_ID);
            removeModifier(server.getAttribute(ForgeMod.ENTITY_REACH.get()), ENTITY_REACH_ID);
        }
    }

    private void publishApi() {
        ReachAPI.setReach(VANILLA_BLOCK + blockBonus.value(), VANILLA_ENTITY + entityBonus.value());
    }

    private static void addModifier(AttributeInstance attribute, UUID id, String name, double amount) {
        if (attribute == null || amount <= 0.0D) return;
        attribute.removeModifier(id);
        attribute.addTransientModifier(
            new AttributeModifier(id, name, amount, AttributeModifier.Operation.ADDITION));
    }

    private static void removeModifier(AttributeInstance attribute, UUID id) {
        if (attribute != null) attribute.removeModifier(id);
    }

    @Override
    public String getDetailedDescription() {
        LocalPlayer player = player();
        String current = player == null ? "-" : String.format("%.1f", player.getBlockReach());
        return getTranslatedDescription()
            + String.format("\n§7方块 §f+%.1f §7实体 §f+%.1f", blockBonus.value(), entityBonus.value())
            + "\n§7当前方块交互距离: §f" + current
            + (CreativeGate.allow() ? "\n§a创造模式 · 已激活" : "\n§c非创造 · 已暂停");
    }
}
