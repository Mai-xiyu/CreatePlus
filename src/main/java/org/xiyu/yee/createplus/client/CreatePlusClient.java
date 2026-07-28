package org.xiyu.yee.createplus.client;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.lwjgl.glfw.GLFW;
import org.xiyu.yee.createplus.Createplus;
import org.xiyu.yee.createplus.api.FreecamAPI;
import org.xiyu.yee.createplus.commands.CreatePlusCommand;
import org.xiyu.yee.createplus.features.AreaFill;
import org.xiyu.yee.createplus.features.CurveBuilder;
import org.xiyu.yee.createplus.features.ShapeStudio;
import org.xiyu.yee.createplus.ui.FeatureScreen;
import org.xiyu.yee.createplus.ui.clickgui.ClickGuiScreen;
import org.xiyu.yee.createplus.mapart.MapArtStore;
import org.xiyu.yee.createplus.utils.BuildingImport;
import org.xiyu.yee.createplus.utils.EditQueue;
import org.xiyu.yee.createplus.utils.KeyBindings;

/**
 * 客户端总入口。
 *
 * 所有事件监听都集中在这里注册，方便看清"什么事件驱动了什么"。
 * 以前这些逻辑散落在主类、FeatureScreen 和几个 EventBusSubscriber 里，
 * 导致 F9 被两个地方同时处理、HUD 每帧被画好几次。
 */
public final class CreatePlusClient {
    private static ClickGuiScreen clickGui;
    private static int welcomeCountdown = -1;
    private static int mapArtReinjectCountdown = -1;

    private CreatePlusClient() {
    }

    public static void init() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener(CreatePlusClient::onClientSetup);
        modBus.addListener(CreatePlusClient::onRegisterKeyMappings);
        modBus.addListener(CreatePlusClient::onRegisterOverlays);

        IEventBus forgeBus = MinecraftForge.EVENT_BUS;
        forgeBus.addListener(CreatePlusClient::onClientTick);
        forgeBus.addListener(CreatePlusClient::onKeyInput);
        forgeBus.addListener(CreatePlusClient::onMouseButton);
        forgeBus.addListener(CreatePlusClient::onMouseScroll);
        forgeBus.addListener(CreatePlusClient::onRenderLevel);
        forgeBus.addListener(CreatePlusClient::onLoggingIn);
        forgeBus.addListener(CreatePlusClient::onLoggingOut);
        forgeBus.addListener(CreatePlusClient::onRegisterClientCommands);
    }

    // === 生命周期 ===

    private static void onClientSetup(FMLClientSetupEvent event) {
        // 必须等所有功能注册完再读配置，否则后注册的功能读不到保存的状态
        event.enqueueWork(() -> Createplus.FEATURE_MANAGER.loadConfig());
    }

    private static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        for (KeyMapping mapping : KeyBindings.all()) {
            event.register(mapping);
        }
    }

    private static void onRegisterOverlays(RegisterGuiOverlaysEvent event) {
        // 注册成一个独立图层：每帧只画一次。
        // 旧写法监听 RenderGuiOverlayEvent.Post，那个事件每个原版图层都会触发一次，
        // HUD 因此被重复绘制十几遍。
        event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "createplus_hud", CreatePlusOverlay::render);
    }

    private static void onLoggingIn(ClientPlayerNetworkEvent.LoggingIn event) {
        Createplus.FEATURE_MANAGER.onWorldJoin();
        welcomeCountdown = 40;
        // 等 ClientLevel 就绪后再灌回地图画像素
        mapArtReinjectCountdown = 5;
    }

    private static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        FreecamAPI.reset();
        welcomeCountdown = -1;
        mapArtReinjectCountdown = -1;
        org.xiyu.yee.createplus.mapart.MapArtStore.clearApplied();
    }

    // === 每 tick ===

    private static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Createplus.FEATURE_MANAGER.onTick();
        FeatureScreen.get().tick();
        EditQueue.tick();
        BuildingImport.tick();
        org.xiyu.yee.createplus.mapart.MapArtPlacer.tick();
        org.xiyu.yee.createplus.mapart.BlockPixelPlacer.tick();

        if (welcomeCountdown > 0 && --welcomeCountdown == 0) {
            WelcomeMessage.send();
        }
        if (mapArtReinjectCountdown > 0 && --mapArtReinjectCountdown == 0) {
            MapArtStore.reinjectAll();
        }

        // 这两个按键在非菜单状态下也要响应
        while (KeyBindings.FILL_SELECTION.consumeClick()) {
            buildOrFill();
        }
        while (KeyBindings.UNDO.consumeClick()) {
            AreaFill fill = Createplus.FEATURE_MANAGER.get(AreaFill.class);
            if (fill != null) fill.undo();
        }
    }

    /**
     * 一个键管三件事：有曲线控制点就修曲线，有几何体预览就落地几何体，
     * 都没有才回落到填充选区。
     *
     * 这三者在流程上是同一步"把刚才规划的东西变成方块"，占三个键位没道理。
     */
    private static void buildOrFill() {
        CurveBuilder curve = Createplus.FEATURE_MANAGER.get(CurveBuilder.class);
        if (curve != null && curve.isEnabled() && curve.pointCount() >= 2) {
            curve.build();
            return;
        }

        ShapeStudio studio = Createplus.FEATURE_MANAGER.get(ShapeStudio.class);
        if (studio != null && studio.isEnabled() && studio.build()) {
            return;
        }

        AreaFill fill = Createplus.FEATURE_MANAGER.get(AreaFill.class);
        if (fill != null) fill.fillFromKeybind();
    }

    // === 输入 ===

    private static void onKeyInput(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null) return;

        if (KeyBindings.TOGGLE_HUD.consumeClick()) {
            FeatureScreen.get().toggleVisibility();
            return;
        }

        if (KeyBindings.TOGGLE_MODULES.consumeClick()) {
            if (clickGui == null) clickGui = new ClickGuiScreen();
            mc.setScreen(clickGui);
            return;
        }

        if (KeyBindings.COMMAND_STUDIO.consumeClick()) {
            mc.setScreen(new org.xiyu.yee.createplus.ui.commandstudio.CommandStudioScreen());
            return;
        }

        if (event.getAction() == GLFW.GLFW_PRESS || event.getAction() == GLFW.GLFW_REPEAT) {
            FeatureScreen.get().handleKeyPress(event.getKey());
        }
    }

    private static void onMouseButton(InputEvent.MouseButton.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null || mc.player == null) return;
        if (event.getAction() != GLFW.GLFW_PRESS) return;

        Createplus.FEATURE_MANAGER.onClick(event.getButton() == GLFW.GLFW_MOUSE_BUTTON_RIGHT);
    }

    private static void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen != null || mc.player == null) return;

        double delta = event.getScrollDelta();
        if (delta == 0.0D) return;

        // 菜单打开时滚轮用来上下选择，不传给功能
        if (FeatureScreen.get().isVisible() && FeatureScreen.get().handleScroll(delta)) {
            event.setCanceled(true);
            return;
        }

        if (Createplus.FEATURE_MANAGER.onScroll(delta)) {
            event.setCanceled(true);
        }
    }

    // === 渲染 ===

    private static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;
        Createplus.FEATURE_MANAGER.renderWorld(
            event.getPoseStack(), event.getCamera(), event.getPartialTick());
        BuildingImport.render(event.getPoseStack(), event.getCamera());
    }

    // === 命令 ===

    private static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
        CreatePlusCommand.register(event.getDispatcher());
    }

    public static ClickGuiScreen clickGui() {
        if (clickGui == null) clickGui = new ClickGuiScreen();
        return clickGui;
    }
}
