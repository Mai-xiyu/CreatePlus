package org.xiyu.yee.createplus.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.xiyu.yee.createplus.Createplus;
import org.xiyu.yee.createplus.features.Performance;

/**
 * 站在传送门里时，原版每刻都会把当前界面强制关掉。
 * 建筑时经常一边调设置一边站在门里，界面被反复关掉很难用。
 *
 * 这里只拦下"关闭界面"这一步：容器界面照旧关闭（否则会和服务端的
 * 容器状态脱节），其他界面保持打开。
 */
@Mixin(LocalPlayer.class)
public class PortalScreenMixin {

    @Redirect(
        method = "handleNetherPortalClient",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;setScreen(Lnet/minecraft/client/gui/screens/Screen;)V"),
        require = 0)
    private void createplus$keepScreenOpenInPortal(Minecraft minecraft, Screen screen) {
        Performance performance = Createplus.performance();
        boolean keepOpen = performance != null
            && performance.isEnabled()
            && performance.isDisablePortalGuiClosing()
            && !(minecraft.screen instanceof AbstractContainerScreen);

        if (!keepOpen) {
            minecraft.setScreen(screen);
        }
    }
}
