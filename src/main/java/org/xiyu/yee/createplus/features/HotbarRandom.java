package org.xiyu.yee.createplus.features;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.xiyu.yee.createplus.settings.BooleanSetting;
import org.xiyu.yee.createplus.utils.CreativeGate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 快捷栏随机：按住右键连放时，每次从快捷栏里有方块的格子随机抽一个来放。
 * 配材质调色板之前的轻量版，适合混泥土/混石砖。
 */
public class HotbarRandom extends CreativePlusFeature {

    private final BooleanSetting onlyWhileUsing = boolSetting("only_while_using", true);

    private boolean wasUsing;

    public HotbarRandom() {
        super("hotbar_random", "连放时从快捷栏随机抽方块（需创造）", Category.BUILDING);
    }

    @Override
    public void onEnable() {
        MinecraftForge.EVENT_BUS.register(this);
        if (!CreativeGate.allow()) {
            notifyPlayer("§c[CreatePlus] §7快捷栏随机只在创造模式下生效");
        }
    }

    @Override
    public void onDisable() {
        MinecraftForge.EVENT_BUS.unregister(this);
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase != TickEvent.Phase.START) return;
        if (!isEnabled() || !CreativeGate.allow()) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.screen != null || mc.options == null) return;

        boolean using = mc.options.keyUse.isDown();
        if (onlyWhileUsing.get() && !using) {
            wasUsing = false;
            return;
        }
        // 每次按下或按住期间持续换，让连放每一格都可能不同
        if (using || !onlyWhileUsing.get()) {
            pick(player);
        }
        wasUsing = using;
    }

    private void pick(LocalPlayer player) {
        List<Integer> slots = new ArrayList<>(9);
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.getItem() instanceof BlockItem && !stack.isEmpty()) {
                slots.add(i);
            }
        }
        if (slots.size() < 2) return;
        int next = slots.get(ThreadLocalRandom.current().nextInt(slots.size()));
        player.getInventory().selected = next;
    }

    @Override
    public String getDetailedDescription() {
        return getTranslatedDescription()
            + "\n§7把要混的方块摆进快捷栏，按住右键连放即可"
            + (CreativeGate.allow() ? "\n§a创造 · 已激活" : "\n§c非创造 · 已暂停");
    }
}
