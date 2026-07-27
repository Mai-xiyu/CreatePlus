package org.xiyu.yee.createplus.mixin;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.xiyu.yee.createplus.Createplus;
import org.xiyu.yee.createplus.features.Performance;

import java.util.concurrent.ThreadLocalRandom;

@Mixin(ParticleEngine.class)
public class ParticleLodMixin {
    @Inject(method = "tickParticle", at = @At("HEAD"))
    private void createplus$particleLod(Particle particle, CallbackInfo ci) {
        Performance performance = Createplus.performance();
        if (performance == null || !performance.isParticleLod()) return;
        if (!createplus$isLowPriority(particle)) return;

        Camera camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        double distSq = particle.getPos().distanceToSqr(camera.getPosition());
        double threshold = performance.getParticleLodDistance();
        if (distSq <= threshold * threshold) {
            ((ParticleAccessor) particle).createplus$setAlpha(1.0F);
            return;
        }
        double keep = performance.getParticleLodKeepChance();
        ((ParticleAccessor) particle).createplus$setAlpha(
            ThreadLocalRandom.current().nextDouble() <= keep ? 1.0F : 0.0F);
    }

    @Unique
    private static boolean createplus$isLowPriority(Particle particle) {
        String name = particle.getClass().getSimpleName().toLowerCase();
        return name.contains("rain") || name.contains("snow") || name.contains("cloud")
            || name.contains("ash") || name.contains("drip") || name.contains("spore")
            || name.contains("water") || name.contains("bubble");
    }
}
