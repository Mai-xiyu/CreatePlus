package org.xiyu.yee.createplus.features;

import net.minecraft.client.player.LocalPlayer;
import org.xiyu.yee.createplus.settings.DoubleSetting;
import org.xiyu.yee.createplus.utils.CreativeGate;

/**
 * 自动上台阶：一步跨上整格方块，修路/跑图不用跳。
 */
public class StepAssist extends CreativePlusFeature {

    private final DoubleSetting height = doubleSetting("height", 1.0D, 0.6D, 2.5D, 0.1D);
    private float savedStep = 0.6F;

    public StepAssist() {
        super("step_assist", "自动跨上整格台阶（需创造）", Category.MOVEMENT);
    }

    @Override
    public void onEnable() {
        LocalPlayer player = player();
        if (player != null) savedStep = player.maxUpStep();
        if (!CreativeGate.allow()) {
            notifyPlayer("§c[CreatePlus] §7自动上台阶只在创造模式下生效");
        }
    }

    @Override
    public void onDisable() {
        LocalPlayer player = player();
        if (player != null) player.setMaxUpStep(savedStep);
    }

    @Override
    public void onTick() {
        LocalPlayer player = player();
        if (player == null) return;
        if (!CreativeGate.allow()) {
            player.setMaxUpStep(savedStep);
            return;
        }
        player.setMaxUpStep(height.floatValue());
    }

    @Override
    public String getDetailedDescription() {
        return getTranslatedDescription()
            + "\n§7高度: §f" + height.getDisplayValue()
            + (CreativeGate.allow() ? "\n§a创造 · 已激活" : "\n§c非创造 · 已暂停");
    }
}
