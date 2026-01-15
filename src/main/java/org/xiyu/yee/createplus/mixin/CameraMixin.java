package org.xiyu.yee.createplus.mixin;

import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.xiyu.yee.createplus.api.FreecamAPI;

@Mixin(Camera.class)
public class CameraMixin {
    @Shadow private Vec3 position;
    @Shadow private float xRot;
    @Shadow private float yRot;

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        if (FreecamAPI.isFreecam() && FreecamAPI.getFreecamPos() != null) {
            position = FreecamAPI.getFreecamPos();
            yRot = FreecamAPI.getFreecamYRot();
            xRot = FreecamAPI.getFreecamXRot();
        }
    }
} 