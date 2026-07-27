package org.xiyu.yee.createplus;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;
import org.xiyu.yee.createplus.features.FeatureManager;
import org.xiyu.yee.createplus.features.Performance;
import org.xiyu.yee.createplus.features.SpeedBuild;
import org.xiyu.yee.createplus.tabs.PlayerInventoryTabs;
import org.xiyu.yee.createplus.tabs.SpawnEggsTab;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Mod(Createplus.MODID)
public class Createplus {

    public static final String MODID = "createplus";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    /** 收集所有没有出现在任何原版标签页里的物品（屏障、命令方块之类）。 */
    public static final RegistryObject<CreativeModeTab> CREATEPLUS_TAB =
        CREATIVE_MODE_TABS.register("hidden_items", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.createplus.hidden_items"))
            .icon(() -> new ItemStack(Items.BARRIER))
            .displayItems((parameters, output) -> {
                Set<Item> shownItems = new HashSet<>();
                CreativeModeTabs.tabs().forEach(tab -> {
                    if (tab.getType() != CreativeModeTab.Type.CATEGORY) return;
                    try {
                        tab.getDisplayItems().forEach(stack -> shownItems.add(stack.getItem()));
                    } catch (Exception ignored) {
                        // 某些模组标签页在这个阶段还没填充，跳过即可
                    }
                });

                for (Item item : BuiltInRegistries.ITEM) {
                    if (shownItems.contains(item)) continue;
                    try {
                        ItemStack stack = new ItemStack(item);
                        if (!stack.isEmpty()) output.accept(stack);
                    } catch (Exception ignored) {
                        // 无法实例化的物品直接忽略
                    }
                }
            })
            .build());

    public static final RegistryObject<CreativeModeTab> SPAWN_EGGS_TAB =
        CREATIVE_MODE_TABS.register("spawn_eggs_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.createplus.spawn_eggs"))
            .icon(() -> new ItemStack(Items.SPAWNER))
            .build());

    /**
     * 预留的在线玩家背包页。数量注册期固定，内容与标题在打开创造栏时按在线列表刷新。
     */
    public static final List<RegistryObject<CreativeModeTab>> PLAYER_INV_TABS = new ArrayList<>();

    static {
        for (int i = 0; i < PlayerInventoryTabs.MAX_TABS; i++) {
            final int index = i;
            PLAYER_INV_TABS.add(CREATIVE_MODE_TABS.register("player_inv_" + i, () -> CreativeModeTab.builder()
                .title(PlayerInventoryTabs.title(index))
                .icon(() -> PlayerInventoryTabs.icon(index))
                .build()));
        }
    }

    public static FeatureManager FEATURE_MANAGER;

    /** 高频 mixin 用的缓存引用，避免每帧去列表里查找。 */
    private static Performance performance;
    private static SpeedBuild speedBuild;

    public Createplus() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        CREATIVE_MODE_TABS.register(modEventBus);
        modEventBus.addListener(this::addCreative);

        if (FMLEnvironment.dist == Dist.CLIENT) {
            FEATURE_MANAGER = new FeatureManager();
            performance = FEATURE_MANAGER.get(Performance.class);
            speedBuild = FEATURE_MANAGER.get(SpeedBuild.class);
            org.xiyu.yee.createplus.client.CreatePlusClient.init();
            PlayerInventoryTabs.init();
        } else {
            LOGGER.info("CreatePlus is a client-side mod; nothing to do on a dedicated server.");
        }
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(SPAWN_EGGS_TAB.getKey())) {
            SpawnEggsTab.buildContents(event);
            return;
        }
        for (int i = 0; i < PLAYER_INV_TABS.size(); i++) {
            if (event.getTabKey().equals(PLAYER_INV_TABS.get(i).getKey())) {
                PlayerInventoryTabs.buildContents(event, i);
                return;
            }
        }
    }

    public static Performance performance() {
        return performance;
    }

    public static SpeedBuild speedBuild() {
        return speedBuild;
    }
}
