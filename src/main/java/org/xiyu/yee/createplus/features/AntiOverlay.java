package org.xiyu.yee.createplus.features;

import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import org.xiyu.yee.createplus.settings.BooleanSetting;
import org.xiyu.yee.createplus.utils.CreativeGate;

/**
 * 清障：去掉黑暗/失明/反胃/凋零等干扰建造的状态，并可选给夜视。
 */
public class AntiOverlay extends CreativePlusFeature {

    private final BooleanSetting clearBlindness = boolSetting("clear_blindness", true);
    private final BooleanSetting clearDarkness = boolSetting("clear_darkness", true);
    private final BooleanSetting clearNausea = boolSetting("clear_nausea", true);
    private final BooleanSetting clearWither = boolSetting("clear_wither", false);
    private final BooleanSetting nightVision = boolSetting("night_vision", false);

    public AntiOverlay() {
        super("anti_overlay", "清除失明/黑暗等遮罩，可选夜视（需创造）", Category.RENDER);
    }

    @Override
    public void onEnable() {
        if (!CreativeGate.allow()) {
            notifyPlayer("§c[CreatePlus] §7清障只在创造模式下生效");
        }
    }

    @Override
    public void onTick() {
        if (!CreativeGate.allow()) return;
        LocalPlayer player = player();
        if (player == null) return;

        if (clearBlindness.get()) player.removeEffect(MobEffects.BLINDNESS);
        if (clearDarkness.get()) player.removeEffect(MobEffects.DARKNESS);
        if (clearNausea.get()) player.removeEffect(MobEffects.CONFUSION);
        if (clearWither.get()) player.removeEffect(MobEffects.WITHER);

        if (nightVision.get()) {
            MobEffectInstance current = player.getEffect(MobEffects.NIGHT_VISION);
            if (current == null || current.getDuration() < 220) {
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 400, 0, false, false));
            }
        }
    }

    @Override
    public String getDetailedDescription() {
        return getTranslatedDescription()
            + (CreativeGate.allow() ? "\n§a创造 · 已激活" : "\n§c非创造 · 已暂停");
    }
}
