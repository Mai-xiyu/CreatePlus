package org.xiyu.yee.createplus.features;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.xiyu.yee.createplus.settings.DoubleSetting;
import org.xiyu.yee.createplus.utils.CreativeGate;

/**
 * 空气放置：准星瞄着空气也能在视线落点放方块（创造建造神器）。
 */
public class AirPlace extends CreativePlusFeature {

    private final DoubleSetting reach = doubleSetting("reach", 5.0D, 2.0D, 32.0D, 0.5D);

    public AirPlace() {
        super("air_place", "对着空气也能放方块（需创造）", Category.BUILDING);
    }

    @Override
    public void onEnable() {
        MinecraftForge.EVENT_BUS.register(this);
        if (!CreativeGate.allow()) {
            notifyPlayer("§c[CreatePlus] §7空气放置只在创造模式下生效");
        }
    }

    @Override
    public void onDisable() {
        MinecraftForge.EVENT_BUS.unregister(this);
    }

    @SubscribeEvent
    public void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        if (!isEnabled() || !event.isUseItem() || !CreativeGate.allow()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gameMode == null || mc.level == null) return;
        if (mc.hitResult != null && mc.hitResult.getType() != HitResult.Type.MISS) return;

        ItemStack held = mc.player.getItemInHand(event.getHand());
        if (!(held.getItem() instanceof BlockItem)) return;

        Vec3 start = mc.player.getEyePosition(1.0F);
        Vec3 look = mc.player.getLookAngle();
        Vec3 end = start.add(look.scale(reach.value()));
        BlockPos pos = BlockPos.containing(end);
        if (!mc.level.getBlockState(pos).canBeReplaced()) return;

        Direction face = Direction.getNearest(-look.x, -look.y, -look.z);
        BlockHitResult fake = new BlockHitResult(end, face, pos, false);
        mc.gameMode.useItemOn(mc.player, event.getHand(), fake);
        mc.player.swing(event.getHand());
        event.setSwingHand(false);
        event.setCanceled(true);
    }

    @Override
    public String getDetailedDescription() {
        return getTranslatedDescription()
            + "\n§7距离: §f" + reach.getDisplayValue()
            + (CreativeGate.allow() ? "\n§a创造 · 已激活" : "\n§c非创造 · 已暂停");
    }
}
