package org.xiyu.yee.createplus.mixin;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.BossHealthOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.xiyu.yee.createplus.utils.HudLayout;

@Mixin(BossHealthOverlay.class)
public class BossBarOverlayMixin {
    @Unique private boolean createplus$pushed;

    @Inject(method = "render", at = @At("HEAD"))
    private void createplus$bossPush(GuiGraphics g, CallbackInfo ci) {
        createplus$pushed = false;
        int dx = HudLayout.dx(HudLayout.BOSS_BAR);
        int dy = HudLayout.dy(HudLayout.BOSS_BAR);
        if (dx != 0 || dy != 0) {
            g.pose().pushPose();
            g.pose().translate(dx, dy, 0);
            createplus$pushed = true;
        }
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void createplus$bossPop(GuiGraphics g, CallbackInfo ci) {
        if (createplus$pushed) {
            g.pose().popPose();
            createplus$pushed = false;
        }
    }
}
