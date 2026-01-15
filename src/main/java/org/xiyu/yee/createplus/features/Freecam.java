package org.xiyu.yee.createplus.features;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import org.xiyu.yee.createplus.utils.KeyBindings;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.level.GameType;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraft.world.phys.AABB;
import java.awt.Color;

import net.minecraft.resources.ResourceLocation;
import org.xiyu.yee.createplus.mixin.CameraMixin;
import org.xiyu.yee.createplus.mixin.CameraAccessor;
import org.xiyu.yee.createplus.api.FreecamAPI;
import net.minecraft.client.Camera;

public class Freecam extends CreativePlusFeature {
    private Vec3 originalPosition;
    private float originalYRot;
    private float originalXRot;
    private GameType originalGameMode; // 记录原始游戏模式
    private float flySpeed = 1.0f;
    private static final float MIN_SPEED = 0.1f;
    private static final float MAX_SPEED = 5.0f;
    private static final float SPEED_STEP = 0.1f;

    public Freecam() {
        super("freecam", "freecam");
        MinecraftForge.EVENT_BUS.register(this);
    }

    @Override
    public void onTick() {
        if (!isEnabled()) return;
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // 检查按键状态
        if (KeyBindings.TOGGLE_FREECAM.consumeClick()) {
            toggleFreecam();
        }

        // 如果处于灵魂出窍状态，更新相机位置
        if (FreecamAPI.isFreecam()) {
            updateCamera();
        }

        // 处理鼠标滚轮调节速度
        while (mc.mouseHandler.getXVelocity() != 0) {
            float delta = (float) (Math.signum(mc.mouseHandler.getXVelocity()) * SPEED_STEP);
            flySpeed = Math.max(MIN_SPEED, Math.min(MAX_SPEED, flySpeed + delta));
        }
    }

    private void toggleFreecam() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.gameMode == null) return;

        if (!FreecamAPI.isFreecam()) {
            // 保存原始状态
            originalPosition = mc.player.position();
            originalYRot = mc.player.getYRot();
            originalXRot = mc.player.getXRot();
            originalGameMode = mc.gameMode.getPlayerMode(); // 保存原始游戏模式

            // 启用灵魂出窍
            FreecamAPI.setFreecamEnabled(true);
            FreecamAPI.setFreecamPosition(originalPosition);
            FreecamAPI.setFreecamRotation(originalYRot, originalXRot);
            
            // 切换到旁观者模式
            mc.gameMode.setLocalMode(GameType.SPECTATOR);
            
        } else {
            // 恢复原始状态
            mc.player.setPos(originalPosition.x, originalPosition.y, originalPosition.z);
            mc.player.setYRot(originalYRot);
            mc.player.setXRot(originalXRot);
            
            // 禁用灵魂出窍
            FreecamAPI.setFreecamEnabled(false);
            
            // 恢复原始游戏模式
            mc.gameMode.setLocalMode(originalGameMode);
        }
    }

    private void updateCamera() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        
        // 计算移动方向
        float yaw = mc.player.getYRot();
        float pitch = mc.player.getXRot();
        float forward = 0;
        float up = 0;
        float strafe = 0;

        if (mc.options.keyUp.isDown()) forward += 1;
        if (mc.options.keyDown.isDown()) forward -= 1;
        if (mc.options.keyLeft.isDown()) strafe -= 1;
        if (mc.options.keyRight.isDown()) strafe += 1;
        if (mc.options.keyJump.isDown()) up += 1;
        if (mc.options.keyShift.isDown()) up -= 1;

        // 计算移动向量
        double rad = Math.toRadians(yaw);
        double dx = (strafe * Math.cos(rad) - forward * Math.sin(rad)) * flySpeed;
        double dz = (forward * Math.cos(rad) + strafe * Math.sin(rad)) * flySpeed;
        double dy = up * flySpeed;

        // 更新相机位置
        Vec3 pos = FreecamAPI.getFreecamPos();
        Vec3 newPos = new Vec3(pos.x + dx, pos.y + dy, pos.z + dz);
        FreecamAPI.setFreecamPosition(newPos);
        FreecamAPI.setFreecamRotation(yaw, pitch);
    }

    @SubscribeEvent
    public void onRenderPlayer(RenderPlayerEvent.Pre event) {
        if (!FreecamAPI.isFreecam() || !isEnabled()) return;
        
        // 如果渲染的是本地玩家，且处于灵魂出窍状态
        if (event.getEntity() == Minecraft.getInstance().player) {
            // 渲染玩家轮廓
            renderPlayerOutline(event.getPoseStack(), event.getEntity());
        }
    }

    private void renderPlayerOutline(PoseStack poseStack, Entity entity) {
        // 保存当前渲染状态
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.disableDepthTest();
        
        // 获取玩家边界框
        AABB box = entity.getBoundingBox();
        float r = 0.0f;
        float g = 1.0f;
        float b = 1.0f;
        float alpha = 0.4f;
        
        // 渲染半透明轮廓
        renderBox(poseStack, box, r, g, b, alpha);
        
        // 恢复渲染状态
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    private void renderBox(PoseStack poseStack, AABB box, float r, float g, float b, float alpha) {
        // 获取Tesselator和BufferBuilder
        Tesselator tesselator = Tesselator.getInstance();
        BufferBuilder bufferBuilder = tesselator.getBuilder();
        
        // 开始绘制
        bufferBuilder.begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
        
        // 绘制边界框的线条
        // 底部矩形
        vertex(bufferBuilder, poseStack, box.minX, box.minY, box.minZ, r, g, b, alpha);
        vertex(bufferBuilder, poseStack, box.maxX, box.minY, box.minZ, r, g, b, alpha);
        vertex(bufferBuilder, poseStack, box.maxX, box.minY, box.minZ, r, g, b, alpha);
        vertex(bufferBuilder, poseStack, box.maxX, box.minY, box.maxZ, r, g, b, alpha);
        vertex(bufferBuilder, poseStack, box.maxX, box.minY, box.maxZ, r, g, b, alpha);
        vertex(bufferBuilder, poseStack, box.minX, box.minY, box.maxZ, r, g, b, alpha);
        vertex(bufferBuilder, poseStack, box.minX, box.minY, box.maxZ, r, g, b, alpha);
        vertex(bufferBuilder, poseStack, box.minX, box.minY, box.minZ, r, g, b, alpha);
        
        // 顶部矩形
        vertex(bufferBuilder, poseStack, box.minX, box.maxY, box.minZ, r, g, b, alpha);
        vertex(bufferBuilder, poseStack, box.maxX, box.maxY, box.minZ, r, g, b, alpha);
        vertex(bufferBuilder, poseStack, box.maxX, box.maxY, box.minZ, r, g, b, alpha);
        vertex(bufferBuilder, poseStack, box.maxX, box.maxY, box.maxZ, r, g, b, alpha);
        vertex(bufferBuilder, poseStack, box.maxX, box.maxY, box.maxZ, r, g, b, alpha);
        vertex(bufferBuilder, poseStack, box.minX, box.maxY, box.maxZ, r, g, b, alpha);
        vertex(bufferBuilder, poseStack, box.minX, box.maxY, box.maxZ, r, g, b, alpha);
        vertex(bufferBuilder, poseStack, box.minX, box.maxY, box.minZ, r, g, b, alpha);
        
        // 连接线
        vertex(bufferBuilder, poseStack, box.minX, box.minY, box.minZ, r, g, b, alpha);
        vertex(bufferBuilder, poseStack, box.minX, box.maxY, box.minZ, r, g, b, alpha);
        vertex(bufferBuilder, poseStack, box.maxX, box.minY, box.minZ, r, g, b, alpha);
        vertex(bufferBuilder, poseStack, box.maxX, box.maxY, box.minZ, r, g, b, alpha);
        vertex(bufferBuilder, poseStack, box.maxX, box.minY, box.maxZ, r, g, b, alpha);
        vertex(bufferBuilder, poseStack, box.maxX, box.maxY, box.maxZ, r, g, b, alpha);
        vertex(bufferBuilder, poseStack, box.minX, box.minY, box.maxZ, r, g, b, alpha);
        vertex(bufferBuilder, poseStack, box.minX, box.maxY, box.maxZ, r, g, b, alpha);
        
        // 结束绘制
        tesselator.end();
    }

    private void vertex(BufferBuilder bufferBuilder, PoseStack poseStack, double x, double y, double z, float r, float g, float b, float alpha) {
        bufferBuilder.vertex(poseStack.last().pose(), (float)x, (float)y, (float)z)
            .color(r, g, b, alpha)
            .endVertex();
    }

    @Override
    public void onDisable() {
        if (FreecamAPI.isFreecam()) {
            toggleFreecam();
        }
    }

    @Override
    public void onEnable() {
        // 不需要特殊初始化
    }

    @Override
    public String getDescription() {
        StringBuilder desc = new StringBuilder(super.getDescription());
        if (isEnabled()) {
            desc.append("\n§7按 F6 开关灵魂出窍");
            if (FreecamAPI.isFreecam()) {
                desc.append("\n§7当前飞行速度: §e").append(String.format("%.1f", flySpeed));
                desc.append("\n§7使用鼠标滚轮调节速度");
                desc.append("\n§7原始模式: §e").append(originalGameMode.getName());
            }
        }
        return desc.toString();
    }
} 