package org.xiyu.yee.createplus.features;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;

/** 需要在世界中（3D 空间）绘制内容的功能。 */
public interface WorldRenderFeature {
    void renderWorld(PoseStack poseStack, Camera camera, float partialTick);
}
