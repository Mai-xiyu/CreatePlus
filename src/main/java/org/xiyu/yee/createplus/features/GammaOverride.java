package org.xiyu.yee.createplus.features;

import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import org.xiyu.yee.createplus.utils.GammaOption;

public class GammaOverride extends CreativePlusFeature {
    private double originalGamma;
    private OptionInstance<Double> gammaOption;
    private static final double NIGHT_VISION_GAMMA = 240.0D;
    private boolean isFirstTick = true;
    private MobEffectInstance nightVisionEffect;
    private boolean wasEnabled = false;
    private double nightVisionGamma = 240.0D;

    public GammaOverride() {
        super("gamma_override", "提供更好的夜视效果");
        initGammaOption();
        // 创建一个持续时间很长且无粒子效果的夜视效果
        nightVisionEffect = new MobEffectInstance(
            MobEffects.NIGHT_VISION,
            Integer.MAX_VALUE,
            0,
            false,  // 是否显示粒子
            false,  // 是否显示图标
            false    // 是否显示在背景
        );
    }

    private void initGammaOption() {
        if (Minecraft.getInstance().options != null) {
            gammaOption = GammaOption.createGammaOption(Minecraft.getInstance().options);
        }
    }

    @Override
    public void onEnable() {
        if (gammaOption == null) {
            initGammaOption();
        }
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            // 保存原始亮度值
            originalGamma = mc.options.gamma().get();
            // 设置最大亮度
            setGamma(NIGHT_VISION_GAMMA);
            // 添加夜视效果
            mc.player.addEffect(new MobEffectInstance(nightVisionEffect));
            isFirstTick = true;
            wasEnabled = true;
        }
    }

    @Override
    public void onDisable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options != null) {
            // 恢复原始亮度值
            setGamma(originalGamma);
        }
        if (mc.player != null && wasEnabled) {
            // 移除夜视效果
            mc.player.removeEffect(MobEffects.NIGHT_VISION);
            wasEnabled = false;
        }
    }

    private void setGamma(double value) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options != null) {
            // 直接修改亮度值
            mc.options.gamma().set(value);
            // 保存设置
            mc.options.save();
        }
    }

    @Override
    public String getDescription() {
        StringBuilder desc = new StringBuilder(super.getDescription());
        if (isEnabled()) {
            desc.append("\n§7当前亮度: §e").append(String.format("%.1f", NIGHT_VISION_GAMMA));
            desc.append("\n§7原始亮度: §7").append(String.format("%.1f", originalGamma));
            if (Minecraft.getInstance().player != null && 
                Minecraft.getInstance().player.hasEffect(MobEffects.NIGHT_VISION)) {
                desc.append("\n§a夜视效果: §a已激活");
            }
        }
        return desc.toString();
    }

    @Override
    public void onTick() {
        if (!isEnabled()) return;
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            // 确保亮度保持在最大值
            double currentGamma = mc.options.gamma().get();
            if (currentGamma != NIGHT_VISION_GAMMA || isFirstTick) {
                setGamma(NIGHT_VISION_GAMMA);
                isFirstTick = false;
            }
            
            // 确保夜视效果持续存在
            if (!mc.player.hasEffect(MobEffects.NIGHT_VISION)) {
                mc.player.addEffect(new MobEffectInstance(nightVisionEffect));
            }
        }
    }

    // Getter 和 Setter 方法
    public double getOriginalGamma() {
        return originalGamma;
    }

    public void setOriginalGamma(double originalGamma) {
        this.originalGamma = originalGamma;
    }

    public double getNightVisionGamma() {
        return nightVisionGamma;
    }

    public void setNightVisionGamma(double nightVisionGamma) {
        this.nightVisionGamma = nightVisionGamma;
    }
} 