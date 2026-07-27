package org.xiyu.yee.createplus.mixin;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.xiyu.yee.createplus.mapart.MapArtStore;

/**
 * 多人客户端地图数据只存在内存里；缺失时从本地缓存补回，避免重进世界后地图空白。
 */
@Mixin(MapItem.class)
public class MapItemMixin {

    @Inject(
        method = "getSavedData(Ljava/lang/Integer;Lnet/minecraft/world/level/Level;)Lnet/minecraft/world/level/saveddata/maps/MapItemSavedData;",
        at = @At("RETURN"),
        cancellable = true)
    private static void createplus$reinjectMissing(
        Integer mapId, Level level, CallbackInfoReturnable<MapItemSavedData> cir) {
        if (cir.getReturnValue() != null || mapId == null || !(level instanceof ClientLevel client)) return;
        if (!MapArtStore.isOurId(mapId)) return;
        byte[] colors = MapArtStore.getColors(mapId);
        if (colors == null) return;
        MapArtStore.injectIntoClient(mapId, colors, client.dimension());
        cir.setReturnValue(client.getMapData(MapItem.makeKey(mapId)));
    }
}
