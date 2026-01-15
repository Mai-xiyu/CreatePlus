package org.xiyu.yee.createplus;

import com.mojang.logging.LogUtils;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;
import net.minecraft.network.chat.Component;
import org.xiyu.yee.createplus.features.*;

import org.xiyu.yee.createplus.ui.FeatureScreen;

import org.xiyu.yee.createplus.commands.FeaturesCommand;

import org.xiyu.yee.createplus.utils.KeyBindings;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import org.xiyu.yee.createplus.tabs.SpawnEggsTab;
import net.minecraft.core.registries.BuiltInRegistries;
import java.util.HashSet;
import java.util.Set;

import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import org.xiyu.yee.createplus.events.ChatSuggestionHandler;


// The value here should match an entry in the META-INF/mods.toml file
@Mod(Createplus.MODID)
public class Createplus {

    // Define mod id in a common place for everything to reference
    public static final String MODID = "createplus";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();
    // Create a Deferred Register to hold Blocks which will all be registered under the "createplus" namespace

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    // 创建创造模式物品栏
    public static final RegistryObject<CreativeModeTab> CREATEPLUS_TAB = CREATIVE_MODE_TABS.register("hidden_items", () ->
        CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.createplus.hidden_items"))
            .icon(() -> new ItemStack(Items.BARRIER))
            .displayItems((parameters, output) -> {
                // 收集所有已显示的物品
                Set<Item> shownItems = new HashSet<>();
                
                // 获取所有标准标签页
                CreativeModeTabs.tabs().forEach(tab -> {
                    String tabId = tab.getDisplayName().getString();
                    if (!tabId.contains("hidden_items")) {
                        tab.getDisplayItems().forEach(stack -> shownItems.add(stack.getItem()));
                    }
                });

                // 添加未显示的物品
                for (Item item : BuiltInRegistries.ITEM) {
                    if (!shownItems.contains(item)) {
                        try {
                            ItemStack stack = new ItemStack(item);
                            if (!stack.isEmpty()) {
                                stack.setCount(1); // 确保堆叠数量为1
                                output.accept(stack);
                            }
                        } catch (Exception ignored) {
                            // 忽略无法创建的物品
                        }
                    }
                }
            })
            .build()
    );

    // 创建实体刷怪蛋标签页
    public static final RegistryObject<CreativeModeTab> SPAWN_EGGS_TAB = CREATIVE_MODE_TABS.register("spawn_eggs_tab",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + MODID + ".spawn_eggs"))
                    .icon(() -> new ItemStack(Items.SPAWNER))
                    .build());

    public static FeatureManager FEATURE_MANAGER;

    public static FeatureScreen featureScreen;

    public Createplus() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        IEventBus forgeBus = MinecraftForge.EVENT_BUS;

        // 只注册创造模式标签页
        CREATIVE_MODE_TABS.register(modEventBus);

        // 注册客户端事件处理器
        modEventBus.addListener(this::addCreative);

        // 注册到Forge事件总线
        forgeBus.register(this);
        
        // 注册客户端tick事件
        forgeBus.addListener(this::onClientTick);

        // 初始化UI
        featureScreen = new FeatureScreen();
        
        // 注册按键事件
        forgeBus.register(featureScreen);

        // 注册命令
        forgeBus.register(FeaturesCommand.class);

        // 初始化FEATURE_MANAGER
        FEATURE_MANAGER = new FeatureManager();

        // 在FEATURE_MANAGER初始化时添加新功能
        FEATURE_MANAGER.registerFeature(new Gibbon());
        FEATURE_MANAGER.registerFeature(new SpeedAdjust());
        FEATURE_MANAGER.registerFeature(new Freecam());
        FEATURE_MANAGER.registerFeature(new Zoom());
        FEATURE_MANAGER.registerFeature(new Performance());
        FEATURE_MANAGER.registerFeature(new TimeWeatherControl());
        FEATURE_MANAGER.registerFeature(new MiniHUD());

        // 注册聊天建议处理器
        MinecraftForge.EVENT_BUS.register(ChatSuggestionHandler.class);
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == SPAWN_EGGS_TAB.getKey()) {
            SpawnEggsTab.buildContents(event);
        }
    }

    private void onClientTick(net.minecraftforge.event.TickEvent.ClientTickEvent event) {
        if (event.phase == net.minecraftforge.event.TickEvent.Phase.END) {
            FEATURE_MANAGER.onTick();
        }
    }

    @SubscribeEvent
    public void onMouseInput(InputEvent.MouseButton.Post event) {
        // 只处理鼠标按下事件 (0是释放，1是按下)
        if (event.getAction() != 1) return;
        
        // 遍历所有启用的功能
        for (CreativePlusFeature feature : FEATURE_MANAGER.getFeatures()) {
            if (feature.isEnabled()) {
                feature.handleClick(event.getButton() == 1); // 1是右键，0是左键
            }
        }
    }

    // 客户端专用的设置
    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            LOGGER.info("Create Plus client initialization...");
        }

        @SubscribeEvent
        public static void registerBindings(RegisterKeyMappingsEvent event) {
            event.register(KeyBindings.TOGGLE_HUD);
            event.register(KeyBindings.TOGGLE_FREECAM);
            event.register(KeyBindings.TOGGLE_ZOOM);
            event.register(KeyBindings.TOGGLE_MODULES);
        }
    }

    // 将渲染事件移到单独的类中
    @Mod.EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
    public static class RenderEvents {
        @SubscribeEvent
        public static void onRenderGui(RenderGuiOverlayEvent.Post event) {
            if (featureScreen != null) {
                featureScreen.render(event.getGuiGraphics(), 0, 0, event.getPartialTick());
            }
            
            // 渲染MiniHUD
            FEATURE_MANAGER.getFeatures().stream()
                .filter(feature -> feature instanceof MiniHUD)
                .map(feature -> (MiniHUD) feature)
                .findFirst()
                .ifPresent(miniHUD -> miniHUD.render(event.getGuiGraphics(), event.getPartialTick()));
        }
    }
}
