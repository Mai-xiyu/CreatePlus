package org.xiyu.yee.createplus.mixin;

import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.arguments.CompoundTagArgument;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.xiyu.yee.createplus.nbt.NbtSuggestionEngine;

import java.util.concurrent.CompletableFuture;

/**
 * 为 CompoundTag 参数补充键名建议（自研实现，不复制 LGPL 源码）。
 */
@Mixin(CompoundTagArgument.class)
public abstract class CompoundTagArgumentMixin implements ArgumentType<CompoundTag> {
    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        if (!NbtSuggestionEngine.enabled()) {
            return Suggestions.empty();
        }
        String input = builder.getInput().toLowerCase();
        String category = "generic";
        if (input.contains("summon") || input.contains("data merge entity")) {
            category = "entity";
        } else if (input.contains("give") || input.contains("item replace")) {
            category = "item";
        } else if (input.contains("setblock") || input.contains("fill") || input.contains("data merge block")) {
            category = "block";
        }
        return NbtSuggestionEngine.suggest(builder, category);
    }
}
