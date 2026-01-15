package org.xiyu.yee.createplus.features;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraftforge.client.event.RenderNameTagEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.common.MinecraftForge;
import org.joml.Matrix4f;

public class PingDisplay extends CreativePlusFeature {
    private int backgroundColor = 0x80000000; // 默认半透明黑色背景
    private float backgroundOpacity = 0.5f;   // 默认背景透明度
    
    public PingDisplay() {
        super("ping_display", "在玩家名字旁显示延迟");
    }

    @Override
    public void onEnable() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    public void onDisable() {
        MinecraftForge.EVENT_BUS.unregister(this);
    }

    @SubscribeEvent
    public void onRenderNameTag(RenderNameTagEvent event) {
        if (!isEnabled() || !(event.getEntity() instanceof Player player)) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() == null) return;

        // 获取玩家延迟
        int ping = mc.getConnection().getPlayerInfo(player.getUUID()) != null ? 
                  mc.getConnection().getPlayerInfo(player.getUUID()).getLatency() : 0;

        // 根据延迟值选择颜色
        String pingColor = getPingColor(ping);
        
        // 获取原始玩家名
        String originalName = event.getContent().getString();
        
        // 构建新的显示文本
        Component newContent = Component.literal(originalName)
            .append(Component.literal(" §8[§9Ping: " + pingColor + ping + "§8]"));

        // 设置新的显示内容
        event.setContent(newContent);

        // 不再需要手动渲染，让Minecraft处理渲染
        // 移除了自定义渲染代码，因为它导致了重复渲染
    }

    private String getPingColor(int ping) {
        if (ping < 50) return "§a"; // 绿色
        if (ping < 100) return "§e"; // 黄色
        if (ping < 200) return "§6"; // 金色
        return "§c"; // 红色
    }

    @Override
    public String getDescription() {
        StringBuilder desc = new StringBuilder(super.getDescription());
        if (isEnabled()) {
            desc.append("\n§7在玩家名字旁显示网络延迟");
            desc.append("\n§7延迟颜色说明:");
            desc.append("\n§a绿色 §7- 极佳 (<50ms)");
            desc.append("\n§e黄色 §7- 良好 (<100ms)");
            desc.append("\n§6金色 §7- 一般 (<200ms)");
            desc.append("\n§c红色 §7- 较差 (>200ms)");
        }
        return desc.toString();
    }

    @Override
    public void onTick() {
        // 不需要tick更新
    }

    // Getter 和 Setter 方法
    public int getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundColor(int backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public float getBackgroundOpacity() {
        return backgroundOpacity;
    }

    public void setBackgroundOpacity(float backgroundOpacity) {
        this.backgroundOpacity = backgroundOpacity;
    }
} 