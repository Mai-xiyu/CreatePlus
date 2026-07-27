package org.xiyu.yee.createplus.tabs;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;

import java.util.Set;

/**
 * 全实体刷怪蛋标签页。
 *
 * 原版只给一部分生物做了刷怪蛋，剩下的（凋灵、末影龙、铁傀儡、各种载具……）
 * 只能靠命令生成。这里给每个可召唤的实体都摆一颗蛋：有原版蛋的用原版蛋，
 * 没有的用一颗写了 {@code EntityTag} 的蛋顶上。
 *
 * 这个 NBT 是原版 {@link SpawnEggItem} 自己认的，所以拿到的蛋在纯净服务端
 * 上一样能用，不需要对方装模组。
 */
public final class SpawnEggsTab {

    /** 生成出来立刻消失或者完全看不见的实体，给蛋没有意义。 */
    private static final Set<EntityType<?>> SKIPPED = Set.of(
        EntityType.ITEM,
        EntityType.EXPERIENCE_ORB,
        EntityType.FALLING_BLOCK,
        EntityType.MARKER
    );

    private SpawnEggsTab() {
    }

    public static void buildContents(BuildCreativeModeTabContentsEvent event) {
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            // canSummon 已经排掉了玩家和鱼钩这类不允许召唤的实体
            if (!type.canSummon() || SKIPPED.contains(type)) continue;

            SpawnEggItem vanilla = SpawnEggItem.byId(type);
            event.accept(vanilla != null ? new ItemStack(vanilla) : customEgg(type));
        }
    }

    /** 借一颗原版蛋当壳子，用 EntityTag 指定真正要生成的实体。 */
    private static ItemStack customEgg(EntityType<?> type) {
        ItemStack stack = new ItemStack(Items.SKELETON_HORSE_SPAWN_EGG);

        CompoundTag entityTag = new CompoundTag();
        entityTag.putString("id", BuiltInRegistries.ENTITY_TYPE.getKey(type).toString());
        stack.getOrCreateTag().put("EntityTag", entityTag);

        // 不关斜体的话，改过名字的物品在物品栏里全是斜体，一眼看过去很乱
        stack.setHoverName(Component.translatable("createplus.spawn_egg", type.getDescription())
            .withStyle(style -> style.withItalic(false).withColor(ChatFormatting.YELLOW)));
        return stack;
    }
}
