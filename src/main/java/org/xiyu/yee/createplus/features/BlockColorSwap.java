package org.xiyu.yee.createplus.features;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.lwjgl.glfw.GLFW;

import java.util.*;

public class BlockColorSwap extends CreativePlusFeature {
    private static final int CHANGE_COOLDOWN = 2; // 添加冷却时间避免过快变化
    private long lastChangeTime = 0;

    // 定义可切换颜色的方块组
    private static final Map<Item, List<Item>> COLOR_GROUPS = new HashMap<>();
    
    static {
        // 羊毛组
        List<Item> wools = Arrays.asList(
            Items.WHITE_WOOL, Items.ORANGE_WOOL, Items.MAGENTA_WOOL,
            Items.LIGHT_BLUE_WOOL, Items.YELLOW_WOOL, Items.LIME_WOOL,
            Items.PINK_WOOL, Items.GRAY_WOOL, Items.LIGHT_GRAY_WOOL,
            Items.CYAN_WOOL, Items.PURPLE_WOOL, Items.BLUE_WOOL,
            Items.BROWN_WOOL, Items.GREEN_WOOL, Items.RED_WOOL,
            Items.BLACK_WOOL
        );
        
        // 玻璃组
        List<Item> glass = Arrays.asList(
            Items.GLASS, Items.WHITE_STAINED_GLASS, Items.ORANGE_STAINED_GLASS,
            Items.MAGENTA_STAINED_GLASS, Items.LIGHT_BLUE_STAINED_GLASS,
            Items.YELLOW_STAINED_GLASS, Items.LIME_STAINED_GLASS,
            Items.PINK_STAINED_GLASS, Items.GRAY_STAINED_GLASS,
            Items.LIGHT_GRAY_STAINED_GLASS, Items.CYAN_STAINED_GLASS,
            Items.PURPLE_STAINED_GLASS, Items.BLUE_STAINED_GLASS,
            Items.BROWN_STAINED_GLASS, Items.GREEN_STAINED_GLASS,
            Items.RED_STAINED_GLASS, Items.BLACK_STAINED_GLASS
        );

        // 地毯组
        List<Item> carpets = Arrays.asList(
            Items.WHITE_CARPET, Items.ORANGE_CARPET, Items.MAGENTA_CARPET,
            Items.LIGHT_BLUE_CARPET, Items.YELLOW_CARPET, Items.LIME_CARPET,
            Items.PINK_CARPET, Items.GRAY_CARPET, Items.LIGHT_GRAY_CARPET,
            Items.CYAN_CARPET, Items.PURPLE_CARPET, Items.BLUE_CARPET,
            Items.BROWN_CARPET, Items.GREEN_CARPET, Items.RED_CARPET,
            Items.BLACK_CARPET
        );

        // 陶瓦组
        List<Item> terracottas = Arrays.asList(
            Items.TERRACOTTA, Items.WHITE_TERRACOTTA, Items.ORANGE_TERRACOTTA,
            Items.MAGENTA_TERRACOTTA, Items.LIGHT_BLUE_TERRACOTTA,
            Items.YELLOW_TERRACOTTA, Items.LIME_TERRACOTTA,
            Items.PINK_TERRACOTTA, Items.GRAY_TERRACOTTA,
            Items.LIGHT_GRAY_TERRACOTTA, Items.CYAN_TERRACOTTA,
            Items.PURPLE_TERRACOTTA, Items.BLUE_TERRACOTTA,
            Items.BROWN_TERRACOTTA, Items.GREEN_TERRACOTTA,
            Items.RED_TERRACOTTA, Items.BLACK_TERRACOTTA
        );

        // 混凝土组
        List<Item> concretes = Arrays.asList(
            Items.WHITE_CONCRETE, Items.ORANGE_CONCRETE, Items.MAGENTA_CONCRETE,
            Items.LIGHT_BLUE_CONCRETE, Items.YELLOW_CONCRETE, Items.LIME_CONCRETE,
            Items.PINK_CONCRETE, Items.GRAY_CONCRETE, Items.LIGHT_GRAY_CONCRETE,
            Items.CYAN_CONCRETE, Items.PURPLE_CONCRETE, Items.BLUE_CONCRETE,
            Items.BROWN_CONCRETE, Items.GREEN_CONCRETE, Items.RED_CONCRETE,
            Items.BLACK_CONCRETE
        );

        // 混凝土粉末组
        List<Item> concretePowders = Arrays.asList(
            Items.WHITE_CONCRETE_POWDER, Items.ORANGE_CONCRETE_POWDER,
            Items.MAGENTA_CONCRETE_POWDER, Items.LIGHT_BLUE_CONCRETE_POWDER,
            Items.YELLOW_CONCRETE_POWDER, Items.LIME_CONCRETE_POWDER,
            Items.PINK_CONCRETE_POWDER, Items.GRAY_CONCRETE_POWDER,
            Items.LIGHT_GRAY_CONCRETE_POWDER, Items.CYAN_CONCRETE_POWDER,
            Items.PURPLE_CONCRETE_POWDER, Items.BLUE_CONCRETE_POWDER,
            Items.BROWN_CONCRETE_POWDER, Items.GREEN_CONCRETE_POWDER,
            Items.RED_CONCRETE_POWDER, Items.BLACK_CONCRETE_POWDER
        );

        // 带釉陶瓦组
        List<Item> glazedTerracottas = Arrays.asList(
            Items.WHITE_GLAZED_TERRACOTTA, Items.ORANGE_GLAZED_TERRACOTTA,
            Items.MAGENTA_GLAZED_TERRACOTTA, Items.LIGHT_BLUE_GLAZED_TERRACOTTA,
            Items.YELLOW_GLAZED_TERRACOTTA, Items.LIME_GLAZED_TERRACOTTA,
            Items.PINK_GLAZED_TERRACOTTA, Items.GRAY_GLAZED_TERRACOTTA,
            Items.LIGHT_GRAY_GLAZED_TERRACOTTA, Items.CYAN_GLAZED_TERRACOTTA,
            Items.PURPLE_GLAZED_TERRACOTTA, Items.BLUE_GLAZED_TERRACOTTA,
            Items.BROWN_GLAZED_TERRACOTTA, Items.GREEN_GLAZED_TERRACOTTA,
            Items.RED_GLAZED_TERRACOTTA, Items.BLACK_GLAZED_TERRACOTTA
        );

        // 玻璃板组
        List<Item> glassPane = Arrays.asList(
            Items.GLASS_PANE, Items.WHITE_STAINED_GLASS_PANE,
            Items.ORANGE_STAINED_GLASS_PANE, Items.MAGENTA_STAINED_GLASS_PANE,
            Items.LIGHT_BLUE_STAINED_GLASS_PANE, Items.YELLOW_STAINED_GLASS_PANE,
            Items.LIME_STAINED_GLASS_PANE, Items.PINK_STAINED_GLASS_PANE,
            Items.GRAY_STAINED_GLASS_PANE, Items.LIGHT_GRAY_STAINED_GLASS_PANE,
            Items.CYAN_STAINED_GLASS_PANE, Items.PURPLE_STAINED_GLASS_PANE,
            Items.BLUE_STAINED_GLASS_PANE, Items.BROWN_STAINED_GLASS_PANE,
            Items.GREEN_STAINED_GLASS_PANE, Items.RED_STAINED_GLASS_PANE,
            Items.BLACK_STAINED_GLASS_PANE
        );

        // 潜影盒组
        List<Item> shulkerBoxes = Arrays.asList(
            Items.SHULKER_BOX, Items.WHITE_SHULKER_BOX, Items.ORANGE_SHULKER_BOX,
            Items.MAGENTA_SHULKER_BOX, Items.LIGHT_BLUE_SHULKER_BOX,
            Items.YELLOW_SHULKER_BOX, Items.LIME_SHULKER_BOX,
            Items.PINK_SHULKER_BOX, Items.GRAY_SHULKER_BOX,
            Items.LIGHT_GRAY_SHULKER_BOX, Items.CYAN_SHULKER_BOX,
            Items.PURPLE_SHULKER_BOX, Items.BLUE_SHULKER_BOX,
            Items.BROWN_SHULKER_BOX, Items.GREEN_SHULKER_BOX,
            Items.RED_SHULKER_BOX, Items.BLACK_SHULKER_BOX
        );

        // 蜡烛组
        List<Item> candles = Arrays.asList(
            Items.CANDLE, Items.WHITE_CANDLE, Items.ORANGE_CANDLE,
            Items.MAGENTA_CANDLE, Items.LIGHT_BLUE_CANDLE,
            Items.YELLOW_CANDLE, Items.LIME_CANDLE,
            Items.PINK_CANDLE, Items.GRAY_CANDLE,
            Items.LIGHT_GRAY_CANDLE, Items.CYAN_CANDLE,
            Items.PURPLE_CANDLE, Items.BLUE_CANDLE,
            Items.BROWN_CANDLE, Items.GREEN_CANDLE,
            Items.RED_CANDLE, Items.BLACK_CANDLE
        );

        // 旗帜组
        List<Item> banners = Arrays.asList(
            Items.WHITE_BANNER, Items.ORANGE_BANNER, Items.MAGENTA_BANNER,
            Items.LIGHT_BLUE_BANNER, Items.YELLOW_BANNER, Items.LIME_BANNER,
            Items.PINK_BANNER, Items.GRAY_BANNER, Items.LIGHT_GRAY_BANNER,
            Items.CYAN_BANNER, Items.PURPLE_BANNER, Items.BLUE_BANNER,
            Items.BROWN_BANNER, Items.GREEN_BANNER, Items.RED_BANNER,
            Items.BLACK_BANNER
        );

        // 床组
        List<Item> beds = Arrays.asList(
            Items.WHITE_BED, Items.ORANGE_BED, Items.MAGENTA_BED,
            Items.LIGHT_BLUE_BED, Items.YELLOW_BED, Items.LIME_BED,
            Items.PINK_BED, Items.GRAY_BED, Items.LIGHT_GRAY_BED,
            Items.CYAN_BED, Items.PURPLE_BED, Items.BLUE_BED,
            Items.BROWN_BED, Items.GREEN_BED, Items.RED_BED,
            Items.BLACK_BED
        );

        // 木头组
        List<Item> logs = Arrays.asList(
            Items.OAK_LOG, Items.SPRUCE_LOG, Items.BIRCH_LOG,
            Items.JUNGLE_LOG, Items.ACACIA_LOG, Items.DARK_OAK_LOG,
            Items.MANGROVE_LOG, Items.CHERRY_LOG, Items.BAMBOO_BLOCK,
            Items.STRIPPED_OAK_LOG, Items.STRIPPED_SPRUCE_LOG,
            Items.STRIPPED_BIRCH_LOG, Items.STRIPPED_JUNGLE_LOG,
            Items.STRIPPED_ACACIA_LOG, Items.STRIPPED_DARK_OAK_LOG,
            Items.STRIPPED_MANGROVE_LOG, Items.STRIPPED_CHERRY_LOG,
            Items.STRIPPED_BAMBOO_BLOCK
        );

        // 木板组
        List<Item> planks = Arrays.asList(
            Items.OAK_PLANKS, Items.SPRUCE_PLANKS, Items.BIRCH_PLANKS,
            Items.JUNGLE_PLANKS, Items.ACACIA_PLANKS, Items.DARK_OAK_PLANKS,
            Items.MANGROVE_PLANKS, Items.CHERRY_PLANKS, Items.BAMBOO_PLANKS
        );

        // 栅栏组
        List<Item> fences = Arrays.asList(
            Items.OAK_FENCE, Items.SPRUCE_FENCE, Items.BIRCH_FENCE,
            Items.JUNGLE_FENCE, Items.ACACIA_FENCE, Items.DARK_OAK_FENCE,
            Items.MANGROVE_FENCE, Items.CHERRY_FENCE, Items.BAMBOO_FENCE
        );

        // 将所有组添加到映射中
        for (Item item : wools) COLOR_GROUPS.put(item, wools);
        for (Item item : glass) COLOR_GROUPS.put(item, glass);
        for (Item item : carpets) COLOR_GROUPS.put(item, carpets);
        for (Item item : terracottas) COLOR_GROUPS.put(item, terracottas);
        for (Item item : concretes) COLOR_GROUPS.put(item, concretes);
        for (Item item : concretePowders) COLOR_GROUPS.put(item, concretePowders);
        for (Item item : glazedTerracottas) COLOR_GROUPS.put(item, glazedTerracottas);
        for (Item item : glassPane) COLOR_GROUPS.put(item, glassPane);
        for (Item item : shulkerBoxes) COLOR_GROUPS.put(item, shulkerBoxes);
        for (Item item : candles) COLOR_GROUPS.put(item, candles);
        for (Item item : banners) COLOR_GROUPS.put(item, banners);
        for (Item item : beds) COLOR_GROUPS.put(item, beds);
        for (Item item : logs) COLOR_GROUPS.put(item, logs);
        for (Item item : planks) COLOR_GROUPS.put(item, planks);
        for (Item item : fences) COLOR_GROUPS.put(item, fences);
    }

    public BlockColorSwap() {
        super("block_color_swap", "按住左CTRL并滚动鼠标滚轮切换同类方块颜色");
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onMouseScroll(InputEvent.MouseScrollingEvent event) {
        if (!isEnabled()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !mc.player.isCreative()) return;

        // 检查是否按住左CTRL
        if (!InputConstants.isKeyDown(mc.getWindow().getWindow(), GLFW.GLFW_KEY_LEFT_CONTROL)) return;

        // 检查冷却时间
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastChangeTime < CHANGE_COOLDOWN) {
            event.setCanceled(true);
            return;
        }

        // 获取玩家手中的物品
        ItemStack mainHand = mc.player.getMainHandItem();
        if (mainHand.isEmpty()) return;

        // 检查是否是可变色物品
        List<Item> group = COLOR_GROUPS.get(mainHand.getItem());
        if (group != null) {
            int currentIndex = group.indexOf(mainHand.getItem());
            if (currentIndex != -1) {
                // 计算新的索引
                int newIndex;
                if (event.getScrollDelta() > 0) {
                    newIndex = (currentIndex + 1) % group.size();
                } else {
                    newIndex = (currentIndex - 1 + group.size()) % group.size();
                }

                // 创建新的物品堆并保持数量
                ItemStack newStack = new ItemStack(group.get(newIndex), mainHand.getCount());
                
                // 如果原物品有NBT，复制NBT
                if (mainHand.hasTag()) {
                    newStack.setTag(mainHand.getTag().copy());
                }

                // 更新客户端物品栏
                int slot = mc.player.getInventory().selected;
                mc.player.getInventory().items.set(slot, newStack);

                // 同步到服务端
                mc.gameMode.handleCreativeModeItemAdd(newStack.copy(), 36 + slot); // 36是快捷栏的起始索引

                // 播放音效和更新冷却时间
                mc.player.playSound(net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), 0.2F, 1.0F);
                lastChangeTime = currentTime;
            }
        }

        // 取消滚动事件
        event.setCanceled(true);
    }

    private net.minecraft.world.level.block.state.BlockState copyBlockProperties(
        net.minecraft.world.level.block.state.BlockState from, 
        net.minecraft.world.level.block.state.BlockState to) {
        // 复制方块的朝向等属性
        for (net.minecraft.world.level.block.state.properties.Property<?> prop : from.getProperties()) {
            if (to.hasProperty(prop)) {
                to = copyProperty(from, to, prop);
            }
        }
        return to;
    }

    @SuppressWarnings("unchecked")
    private <T extends Comparable<T>> net.minecraft.world.level.block.state.BlockState copyProperty(
        net.minecraft.world.level.block.state.BlockState from,
        net.minecraft.world.level.block.state.BlockState to,
        net.minecraft.world.level.block.state.properties.Property<T> prop) {
        return to.setValue(prop, from.getValue(prop));
    }

    @Override
    public void onEnable() {}

    @Override
    public void onDisable() {}

} 