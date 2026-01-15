package org.xiyu.yee.createplus.mixin;

import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Camera.class)
public interface CameraAccessor {
    @Accessor("entity")
    Entity getEntity();
    
    @Accessor("entity")
    void setEntity(Entity entity);
    
    @Invoker("setPosition")
    void invokeSetPosition(Vec3 pos);

    @Invoker("setRotation")
    void invokeSetRotation(float yRot, float xRot);
} 