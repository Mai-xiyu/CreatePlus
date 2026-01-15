package org.xiyu.yee.createplus.utils;

import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.network.chat.Component;

public class GammaOption {
    private static final double DEFAULT_GAMMA = 1.0D;
    private static final double MAX_GAMMA = 12.0D;
    
    public static OptionInstance<Double> createGammaOption(Options options) {
        return new OptionInstance<>(
            "options.gamma",
            OptionInstance.noTooltip(),
            (component, value) -> {
                if (value == 0.0D) {
                    return Component.translatable("options.gamma.min");
                } else if (value == 1.0D) {
                    return Component.translatable("options.gamma.default");
                } else if (value == MAX_GAMMA) {
                    return Component.translatable("options.gamma.max");
                } else {
                    return Component.literal((int)(value * 100.0D) + "%");
                }
            },
            OptionInstance.UnitDouble.INSTANCE
                .xmap(
                    d -> d * MAX_GAMMA,
                    d -> d / MAX_GAMMA
                ),
            DEFAULT_GAMMA,
            value -> {}
        );
    }
} 