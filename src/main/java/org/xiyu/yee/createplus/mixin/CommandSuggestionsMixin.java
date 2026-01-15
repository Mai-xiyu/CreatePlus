package org.xiyu.yee.createplus.mixin;

import net.minecraft.client.gui.components.CommandSuggestions;
import net.minecraft.client.gui.components.EditBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CommandSuggestions.class)
public abstract class CommandSuggestionsMixin {
    @Shadow private EditBox input;
    @Shadow public abstract void setAllowSuggestions(boolean allowSuggestions);

    @Inject(method = "keyPressed", at = @At("HEAD"))
    private void onKeyPressed(int p_93889_, int p_93890_, int p_93891_, CallbackInfoReturnable<Boolean> cir) {
        // 允许点号命令的补全
        if (input.getValue().startsWith(".")) {
            setAllowSuggestions(true);
        }
    }
}