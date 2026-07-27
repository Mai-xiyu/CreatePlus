package org.xiyu.yee.createplus.mixin;

import net.minecraft.client.OptionInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 直接写入 OptionInstance 的内部值，绕过取值范围校验。
 *
 * 夜视功能需要把亮度推到远高于 1.0 的地方。走 set() 会被
 * validateValue 拒绝并在日志里刷 "Illegal option value"，
 * 而替换整个 OptionInstance 又会让原版的亮度滑条和存档失效。
 */
@Mixin(OptionInstance.class)
public interface OptionInstanceAccessor {
    @Accessor("value")
    void createplus$setRawValue(Object value);
}
