package org.xiyu.yee.createplus.mixin;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.xiyu.yee.createplus.utils.HudLayout;

@Mixin(Gui.class)
public class GuiHudOffsetMixin {
    @Unique private boolean createplus$pushed;

    @Unique
    private void createplus$push(GuiGraphics g, String id) {
        createplus$pushed = false;
        int dx = HudLayout.dx(id);
        int dy = HudLayout.dy(id);
        if (dx != 0 || dy != 0) {
            g.pose().pushPose();
            g.pose().translate(dx, dy, 0);
            createplus$pushed = true;
        }
    }

    @Unique
    private void createplus$pop(GuiGraphics g) {
        if (createplus$pushed) {
            g.pose().popPose();
            createplus$pushed = false;
        }
    }

    @Inject(method = "renderPlayerHealth", at = @At("HEAD"))
    private void createplus$healthPush(GuiGraphics g, CallbackInfo ci) {
        createplus$push(g, HudLayout.HEALTH);
    }

    @Inject(method = "renderPlayerHealth", at = @At("RETURN"))
    private void createplus$healthPop(GuiGraphics g, CallbackInfo ci) {
        createplus$pop(g);
    }

    @Inject(method = "renderHotbar", at = @At("HEAD"))
    private void createplus$hotbarPush(float pt, GuiGraphics g, CallbackInfo ci) {
        createplus$push(g, HudLayout.HOTBAR);
    }

    @Inject(method = "renderHotbar", at = @At("RETURN"))
    private void createplus$hotbarPop(float pt, GuiGraphics g, CallbackInfo ci) {
        createplus$pop(g);
    }

    @Inject(method = "renderExperienceBar", at = @At("HEAD"))
    private void createplus$xpPush(GuiGraphics g, int x, CallbackInfo ci) {
        createplus$push(g, HudLayout.EXPERIENCE);
    }

    @Inject(method = "renderExperienceBar", at = @At("RETURN"))
    private void createplus$xpPop(GuiGraphics g, int x, CallbackInfo ci) {
        createplus$pop(g);
    }

    @Inject(method = "renderEffects", at = @At("HEAD"))
    private void createplus$fxPush(GuiGraphics g, CallbackInfo ci) {
        createplus$push(g, HudLayout.EFFECTS);
    }

    @Inject(method = "renderEffects", at = @At("RETURN"))
    private void createplus$fxPop(GuiGraphics g, CallbackInfo ci) {
        createplus$pop(g);
    }
}
