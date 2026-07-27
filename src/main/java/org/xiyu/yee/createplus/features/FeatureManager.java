package org.xiyu.yee.createplus.features;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.gui.GuiGraphics;
import org.xiyu.yee.createplus.Createplus;
import org.xiyu.yee.createplus.utils.ConfigManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FeatureManager {
    private final Map<String, CreativePlusFeature> byName = new LinkedHashMap<>();
    private final List<CreativePlusFeature> featureList = new ArrayList<>();
    private boolean configLoaded = false;

    public FeatureManager() {
        registerAll();
    }

    private void registerAll() {
        // 建筑
        register(new BuildSelection());
        register(new SpeedBuild());
        register(new InfinitePlacement());
        register(new AreaFill());
        register(new AreaCopy());
        register(new MirrorBuild());
        register(new PaintPalette());

        // 以下顺序同时决定 Ctrl + 滚轮的归属：第一个接下的独占这次事件，
        // 所以按"生效窗口从窄到宽"排 —— 越是玩家正捏在手里的操作越靠前。
        // 放置限制只在右键按住不放的那几秒内生效，窗口最窄；
        // 曲线要先有控制点，几何体要先有预览；
        // 方块变色只要手上是成组方块就一直算数；
        // 范围放置/破坏则是任何时候都想调半径。
        register(new PlacementRestriction());
        register(new CurveBuilder());
        register(new ShapeStudio());
        register(new BlockColorSwap());
        register(new AreaPlace());
        register(new Nucker());
        register(new BuildingExport());
        register(new Scaffold());

        // 移动
        register(new ImprovedFlight());
        register(new SpeedAdjust());
        register(new ExtendedReach());
        register(new NoClip());
        register(new StepAssist());
        register(new LiquidWalk());
        register(new FastBreak());
        register(new SpinBot());
        register(new SnapAim());

        // 建筑（续）
        register(new AirPlace());
        register(new AutoBridge());
        register(new MeasureRuler());
        register(new LayerSlice());
        register(new AutoPick());

        // 渲染 / 视觉
        register(new Freecam());
        register(new Zoom());
        register(new GammaOverride());
        register(new ChunkBorders());
        register(new LightOverlay());
        register(new TimeWeatherControl());
        register(new StorageEsp());
        register(new Waypoints());
        register(new CameraClip());
        register(new AntiOverlay());
        register(new StateClipboard());
        register(new HotbarRandom());
        register(new LookReplace());
        register(new FloodFill());
        register(new MapArt());

        // HUD
        register(new MiniHUD());
        register(new BlockInfo());
        register(new PingDisplay());
        register(new HudEditor());

        // 系统
        register(new Performance());
        register(new CommandStudio());
    }

    /**
     * 加载配置。必须在所有功能注册完成之后调用，
     * 否则后注册的功能会读不到已保存的状态（这是旧版本的一个 bug）。
     */
    public void loadConfig() {
        ConfigManager.load();
        configLoaded = true;
    }

    /** 功能状态或设置发生变化后写盘。配置加载过程中调用会被忽略，避免自我覆盖。 */
    public void saveConfig() {
        if (configLoaded) {
            ConfigManager.save();
        }
    }

    public void register(CreativePlusFeature feature) {
        if (byName.containsKey(feature.getName())) {
            Createplus.LOGGER.warn("CreatePlus: duplicate feature id '{}' ignored", feature.getName());
            return;
        }
        byName.put(feature.getName(), feature);
        featureList.add(feature);
    }

    public List<CreativePlusFeature> getFeatures() {
        return Collections.unmodifiableList(featureList);
    }

    public CreativePlusFeature getFeature(String name) {
        return byName.get(name);
    }

    /** 按类型查找功能，比按名字查找更不容易写错。 */
    @SuppressWarnings("unchecked")
    public <T extends CreativePlusFeature> T get(Class<T> type) {
        for (CreativePlusFeature feature : featureList) {
            if (type.isInstance(feature)) return (T) feature;
        }
        return null;
    }

    public List<CreativePlusFeature> getByCategory(CreativePlusFeature.Category category) {
        List<CreativePlusFeature> result = new ArrayList<>();
        for (CreativePlusFeature feature : featureList) {
            if (feature.getCategory() == category) result.add(feature);
        }
        return result;
    }

    /** 切换功能并保存配置。所有 UI 与命令都应该走这里。 */
    public void toggle(CreativePlusFeature feature) {
        feature.toggle();
        saveConfig();
    }

    // === 事件分发 ===

    public void onTick() {
        for (CreativePlusFeature feature : featureList) {
            if (!feature.isEnabled()) continue;
            try {
                feature.onTick();
            } catch (Exception e) {
                Createplus.LOGGER.error("CreatePlus feature '{}' crashed during tick, disabling it",
                    feature.getName(), e);
                feature.setEnabled(false);
            }
        }
    }

    public void onWorldJoin() {
        for (CreativePlusFeature feature : featureList) {
            if (!feature.isEnabled()) continue;
            try {
                feature.onWorldJoin();
            } catch (Exception e) {
                Createplus.LOGGER.error("CreatePlus feature '{}' crashed on world join",
                    feature.getName(), e);
            }
        }
    }

    public void onClick(boolean isRightClick) {
        for (CreativePlusFeature feature : featureList) {
            if (!feature.isEnabled()) continue;
            try {
                feature.handleClick(isRightClick);
            } catch (Exception e) {
                Createplus.LOGGER.error("CreatePlus feature '{}' crashed handling a click",
                    feature.getName(), e);
            }
        }
    }

    /**
     * 第一个接下滚轮的功能独占这次事件。
     *
     * 不能让所有功能都处理：Ctrl + 滚轮同时被范围放置、范围破坏、镜像轴、
     * 粘贴旋转认领的话，滚一格会一次性改掉四个设置。
     *
     * @return true 表示某个功能消费了滚轮事件
     */
    public boolean onScroll(double delta) {
        for (CreativePlusFeature feature : featureList) {
            if (!feature.isEnabled()) continue;
            try {
                if (feature.handleScroll(delta)) return true;
            } catch (Exception e) {
                Createplus.LOGGER.error("CreatePlus feature '{}' crashed handling scroll",
                    feature.getName(), e);
            }
        }
        return false;
    }

    public void renderHud(GuiGraphics graphics, int width, int height, float partialTick) {
        for (CreativePlusFeature feature : featureList) {
            if (!feature.isEnabled() || !(feature instanceof HudRenderFeature hud)) continue;
            try {
                hud.renderHud(graphics, width, height, partialTick);
            } catch (Exception e) {
                Createplus.LOGGER.error("CreatePlus feature '{}' crashed rendering HUD",
                    feature.getName(), e);
                feature.setEnabled(false);
            }
        }
    }

    public void renderWorld(PoseStack poseStack, Camera camera, float partialTick) {
        for (CreativePlusFeature feature : featureList) {
            if (!feature.isEnabled() || !(feature instanceof WorldRenderFeature world)) continue;
            try {
                world.renderWorld(poseStack, camera, partialTick);
            } catch (Exception e) {
                Createplus.LOGGER.error("CreatePlus feature '{}' crashed rendering world",
                    feature.getName(), e);
                feature.setEnabled(false);
            }
        }
    }
}
