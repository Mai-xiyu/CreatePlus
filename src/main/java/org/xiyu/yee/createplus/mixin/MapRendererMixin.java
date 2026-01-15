package org.xiyu.yee.createplus.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.MapRenderer;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MapRenderer.class)
public class MapRendererMixin {
    private static final ResourceLocation[] LOADING_FRAMES = new ResourceLocation[6];
    private static final int FRAME_TIME = 150; // 调整帧率
    private long startTime = -1;
    private int currentFrame = 0;
    private boolean isLoading = true;
    
    static {
        // 初始化所有帧的资源位置
        for (int i = 0; i < LOADING_FRAMES.length; i++) {
            LOADING_FRAMES[i] = new ResourceLocation("createplus", 
                String.format("textures/gui/loading/frame_%d.png", i));
        }
    }

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void onRender(PoseStack poseStack, MultiBufferSource buffer, int packedLight, 
                         MapItemSavedData mapData, boolean isSecret, int decorations, CallbackInfo ci) {
        if (!isLoading) {
            return;
        }

        if (startTime == -1) {
            startTime = System.currentTimeMillis();
        }

        long currentTime = System.currentTimeMillis();
        long elapsedTime = currentTime - startTime;
        
        // 计算当前帧
        currentFrame = (int)((elapsedTime / FRAME_TIME) % LOADING_FRAMES.length);
        
        // 渲染加载动画
        renderLoadingFrame(poseStack);
        
        // 检查是否应该结束动画
        if (elapsedTime > FRAME_TIME * LOADING_FRAMES.length * 2) { // 播放两次循环
            isLoading = false;
            startTime = -1;
            return;
        }
        
        // 取消原始渲染
        ci.cancel();
    }

    private void renderLoadingFrame(PoseStack poseStack) {
        // 保存当前矩阵状态
        poseStack.pushPose();
        
        // 设置渲染状态
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, LOADING_FRAMES[currentFrame]);

        // 获取Minecraft实例和窗口尺寸
        Minecraft mc = Minecraft.getInstance();
        float scale = (float)mc.getWindow().getGuiScale();
        
        // 计算渲染位置(居中)
        float mapSize = 128.0F;
        float x = (mc.getWindow().getGuiScaledWidth() - mapSize) / 2;
        float y = (mc.getWindow().getGuiScaledHeight() - mapSize) / 2;
        
        // 渲染当前帧
        BufferBuilder bufferbuilder = Tesselator.getInstance().getBuilder();
        bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        
        // 绘制纹理四边形
        bufferbuilder.vertex(poseStack.last().pose(), x, y + mapSize, 0).uv(0, 1).endVertex();
        bufferbuilder.vertex(poseStack.last().pose(), x + mapSize, y + mapSize, 0).uv(1, 1).endVertex();
        bufferbuilder.vertex(poseStack.last().pose(), x + mapSize, y, 0).uv(1, 0).endVertex();
        bufferbuilder.vertex(poseStack.last().pose(), x, y, 0).uv(0, 0).endVertex();
        
        // 完成渲染
        BufferUploader.drawWithShader(bufferbuilder.end());
        
        // 恢复渲染状态
        RenderSystem.disableBlend();
        poseStack.popPose();
    }
} 