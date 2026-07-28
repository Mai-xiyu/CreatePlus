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
 * 本机生成的 filled_map 若缓存里有像素，缺失时补回（仅自己客户端显示用地图）。
 */
@Mixin(MapItem.class)
public class MapItemMixin {

    @Inject(
        method = "getSavedData(Ljava/lang/Integer;Lnet/minecraft/world/level/Level;)Lnet/minecraft/world/level/saveddata/maps/MapItemSavedData;",
        at = @At("RETURN"),
        cancellable = true)
    private static void createplus$fromLocalCache(
        Integer mapId, Level level, CallbackInfoReturnable<MapItemSavedData> cir) {
        if (cir.getReturnValue() != null || mapId == null || !(level instanceof ClientLevel client)) return;
        if (!MapArtStore.isOurId(mapId)) return;
        byte[] colors = MapArtStore.getColors(mapId);
        if (colors == null) return;
        MapArtStore.injectIntoClient(mapId, colors, client.dimension());
        cir.setReturnValue(client.getMapData(MapItem.makeKey(mapId)));
    }
}
