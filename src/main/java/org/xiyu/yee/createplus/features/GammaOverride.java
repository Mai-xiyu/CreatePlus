package org.xiyu.yee.createplus.features;

import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import org.xiyu.yee.createplus.mixin.OptionInstanceAccessor;
import org.xiyu.yee.createplus.settings.BooleanSetting;
import org.xiyu.yee.createplus.settings.DoubleSetting;

/**
 * 夜视 / 全亮。
 *
 * 亮度是可调的（README 里一直这么写，但旧版本其实写死成 240 并且
 * 每 tick 都在往 options.txt 写盘）。
 */
public class GammaOverride extends CreativePlusFeature {
    private static final double VANILLA_MAX_GAMMA = 1.0D;

    private final DoubleSetting brightness = doubleSetting("brightness", 8.0D, 1.0D, 20.0D, 0.5D);
    private final BooleanSetting nightVisionEffect = boolSetting("night_vision_effect", true);

    private double savedGamma = VANILLA_MAX_GAMMA;
    private boolean gammaOverridden = false;

    public GammaOverride() {
        super("gamma_override", "夜视：大幅提高画面亮度，亮度可调", Category.RENDER);
        brightness.onChange(value -> {
            if (isEnabled()) applyGamma(value);
        });
    }

    @Override
    public void onEnable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options == null) return;

        if (!gammaOverridden) {
            savedGamma = mc.options.gamma().get();
            gammaOverridden = true;
        }
        applyGamma(brightness.value());
        applyNightVision();
    }

    @Override
    public void onWorldJoin() {
        applyGamma(brightness.value());
        applyNightVision();
    }

    @Override
    public void onDisable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options != null && gammaOverridden) {
            applyGamma(savedGamma);
            gammaOverridden = false;
        }

        LocalPlayer player = player();
        if (player != null && nightVisionEffect.get()) {
            player.removeEffect(MobEffects.NIGHT_VISION);
        }
    }

    @Override
    public void onTick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options == null) return;

        // 别的界面（比如原版亮度滑条）可能把值改回去，这里只在真的不一致时才写
        if (Math.abs(mc.options.gamma().get() - brightness.value()) > 0.01D) {
            applyGamma(brightness.value());
        }

        applyNightVision();
    }

    private void applyNightVision() {
        if (!nightVisionEffect.get()) return;
        LocalPlayer player = player();
        if (player == null) return;
        if (player.hasEffect(MobEffects.NIGHT_VISION)) return;

        // 隐藏粒子与图标，避免屏幕上一直挂着药水效果
        player.addEffect(new MobEffectInstance(
            MobEffects.NIGHT_VISION, Integer.MAX_VALUE, 0, false, false, false));
    }

    private void applyGamma(double value) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options == null) return;

        // 一律直接写字段：既能超出原版上限，又不会把亮度存进 options.txt，
        // 关掉功能后玩家原来的亮度设置还在
        OptionInstance<Double> gamma = mc.options.gamma();
        ((OptionInstanceAccessor) (Object) gamma).createplus$setRawValue(value);
    }

    @Override
    public String getDetailedDescription() {
        return getTranslatedDescription()
            + String.format("\n§7亮度: §f%.1f §7(原始 %.2f)", brightness.value(), savedGamma)
            + (nightVisionEffect.get() ? "\n§7附带无粒子夜视效果" : "");
    }
}
