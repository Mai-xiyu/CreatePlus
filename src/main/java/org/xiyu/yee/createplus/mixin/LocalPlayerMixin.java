package org.xiyu.yee.createplus.mixin;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.xiyu.yee.createplus.api.FreecamAPI;

@Mixin(AbstractClientPlayer.class)
public abstract class LocalPlayerMixin extends Player {

    public LocalPlayerMixin(Level level, BlockPos pos, float rot, GameProfile profile) {
        super(level, pos, rot, profile);
    }

    @Inject(method = "isSpectator", at = @At("RETURN"), cancellable = true)
    public void spectator(CallbackInfoReturnable<Boolean> cir) {
        try {
            if (Minecraft.getInstance().player != null && 
                FreecamAPI.isFreecam() && 
                (this.getId() == Minecraft.getInstance().player.getId())) {
                cir.setReturnValue(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}