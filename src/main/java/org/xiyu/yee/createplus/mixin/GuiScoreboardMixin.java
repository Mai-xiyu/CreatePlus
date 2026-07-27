package org.xiyu.yee.createplus.mixin;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.scores.Objective;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.xiyu.yee.createplus.utils.HudLayout;

@Mixin(Gui.class)
public class GuiScoreboardMixin {
    @Unique private boolean createplus$scoreboardPushed;

    @Inject(method = "displayScoreboardSidebar", at = @At("HEAD"))
    private void createplus$scoreboardPush(GuiGraphics graphics, Objective objective, CallbackInfo ci) {
        createplus$scoreboardPushed = false;
        int dx = HudLayout.scoreboardDx();
        int dy = HudLayout.scoreboardDy();
        if (dx != 0 || dy != 0) {
            graphics.pose().pushPose();
            graphics.pose().translate(dx, dy, 0);
            createplus$scoreboardPushed = true;
        }
    }

    @Inject(method = "displayScoreboardSidebar", at = @At("RETURN"))
    private void createplus$scoreboardPop(GuiGraphics graphics, Objective objective, CallbackInfo ci) {
        if (createplus$scoreboardPushed) {
            graphics.pose().popPose();
            createplus$scoreboardPushed = false;
        }
    }
}
