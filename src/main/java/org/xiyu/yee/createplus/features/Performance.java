package org.xiyu.yee.createplus.features;

import org.xiyu.yee.createplus.settings.BooleanSetting;
import org.xiyu.yee.createplus.settings.DoubleSetting;
import org.xiyu.yee.createplus.settings.IntSetting;

/**
 * 一组客户端渲染 / 体验优化开关（参考 Graphene 等开源思路，纯客户端）。
 *
 * 各个开关由对应的 mixin 读取；功能本身必须开启才会生效。
 */
public class Performance extends CreativePlusFeature {
    private final BooleanSetting disableParticles = boolSetting("disable_particles", false);
    private final BooleanSetting disableBlockBreakingParticles =
        boolSetting("disable_block_breaking_particles", true);
    private final BooleanSetting disableDeadMobRendering = boolSetting("disable_dead_mob_rendering", false);
    private final BooleanSetting disableEntityRendering = boolSetting("disable_entity_rendering", false);
    private final BooleanSetting disableFallingBlockRendering =
        boolSetting("disable_falling_block_rendering", false);
    private final BooleanSetting disablePortalGuiClosing = boolSetting("disable_portal_gui_closing", false);

    // —— Graphene 风格优化 ——
    private final BooleanSetting skipUnderwaterSky = boolSetting("skip_underwater_sky", true);
    private final BooleanSetting skipOutlineWhenNoGlow = boolSetting("skip_outline_when_no_glow", true);
    private final BooleanSetting particleLod = boolSetting("particle_lod", true);
    private final DoubleSetting particleLodDistance = doubleSetting("particle_lod_distance", 48.0D, 16.0D, 128.0D, 1.0D);
    private final DoubleSetting particleLodKeepChance = doubleSetting("particle_lod_keep_chance", 0.25D, 0.0D, 1.0D, 0.05D);
    private final BooleanSetting distantChestSkip = boolSetting("distant_chest_skip", true);
    private final IntSetting distantChestChunks = intSetting("distant_chest_chunks", 4, 1, 16);
    private final BooleanSetting inactiveFpsLimit = boolSetting("inactive_fps_limit", true);
    private final IntSetting inactiveFps = intSetting("inactive_fps", 30, 5, 120);
    private final BooleanSetting bambooFullBright = boolSetting("bamboo_full_bright", false);

    public Performance() {
        super("performance", "性能优化：粒子、实体、天空、箱子距离与失焦限帧等", Category.SYSTEM);
    }

    public boolean isDisableParticles() {
        return disableParticles.get();
    }

    public boolean isDisableBlockBreakingParticles() {
        return disableBlockBreakingParticles.get();
    }

    public boolean isDisableDeadMobRendering() {
        return disableDeadMobRendering.get();
    }

    public boolean isDisableEntityRendering() {
        return disableEntityRendering.get();
    }

    public boolean isDisableFallingBlockRendering() {
        return disableFallingBlockRendering.get();
    }

    public boolean isDisablePortalGuiClosing() {
        return disablePortalGuiClosing.get();
    }

    public boolean isSkipUnderwaterSky() {
        return isEnabled() && skipUnderwaterSky.get();
    }

    public boolean isSkipOutlineWhenNoGlow() {
        return isEnabled() && skipOutlineWhenNoGlow.get();
    }

    public boolean isParticleLod() {
        return isEnabled() && particleLod.get();
    }

    public double getParticleLodDistance() {
        return particleLodDistance.value();
    }

    public double getParticleLodKeepChance() {
        return particleLodKeepChance.value();
    }

    public boolean isDistantChestSkip() {
        return isEnabled() && distantChestSkip.get();
    }

    public int getDistantChestChunks() {
        return distantChestChunks.value();
    }

    public boolean isInactiveFpsLimit() {
        return isEnabled() && inactiveFpsLimit.get();
    }

    public int getInactiveFps() {
        return inactiveFps.value();
    }

    public boolean isBambooFullBright() {
        return isEnabled() && bambooFullBright.get();
    }

    @Override
    public String getDetailedDescription() {
        return getTranslatedDescription() + "\n§7可在 选项 → 性能优化 中详细配置";
    }
}
