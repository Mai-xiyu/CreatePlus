package org.xiyu.yee.createplus.features;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.xiyu.yee.createplus.settings.BooleanSetting;
import org.xiyu.yee.createplus.utils.KeyBindings;

import java.util.*;

/**
 * 按住修饰键滚轮，在同类方块之间循环切换颜色/材质。
 *
 * 建造时最费时间的操作之一就是翻创造物品栏找另一种颜色的同款方块。
 */
public class BlockColorSwap extends CreativePlusFeature {

    /** 可互换的方块组，key 是组里的每个物品，value 是整个组。 */
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

    private final BooleanSetting playSound = boolSetting("play_sound", true);
    private final BooleanSetting showName = boolSetting("show_name", true);

    public BlockColorSwap() {
        super("block_color_swap", "按住修饰键滚轮，在同类方块间切换颜色", Category.BUILDING);
    }

    @Override
    public boolean handleScroll(double delta) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || !player.isCreative() || mc.gameMode == null) return false;
        if (!KeyBindings.modifierDown()) return false;

        ItemStack held = player.getMainHandItem();
        List<Item> group = COLOR_GROUPS.get(held.getItem());
        if (group == null) return false;

        int index = group.indexOf(held.getItem());
        if (index < 0) return false;

        int next = (index + (delta > 0 ? 1 : -1) + group.size()) % group.size();
        ItemStack replacement = new ItemStack(group.get(next), held.getCount());
        if (held.hasTag()) {
            replacement.setTag(held.getTag().copy());
        }

        int slot = player.getInventory().selected;
        player.getInventory().items.set(slot, replacement);
        // 创造物品栏槽位编号：快捷栏从 36 开始
        mc.gameMode.handleCreativeModeItemAdd(replacement.copy(), 36 + slot);

        if (playSound.get()) {
            player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.2F, 1.6F);
        }
        if (showName.get()) {
            notifyActionBar("§b" + replacement.getHoverName().getString()
                + " §7(" + (next + 1) + "/" + group.size() + ")");
        }
        return true;
    }

    @Override
    public String getDetailedDescription() {
        Item held = Minecraft.getInstance().player != null
            ? Minecraft.getInstance().player.getMainHandItem().getItem() : null;
        List<Item> group = held == null ? null : COLOR_GROUPS.get(held);
        return getTranslatedDescription()
            + "\n§7按住 §f" + KeyBindings.MODIFIER.getTranslatedKeyMessage().getString()
            + " §7再滚动滚轮"
            + "\n§7当前手持: §f" + (group == null ? "不支持切换" : group.size() + " 种可选");
    }
} 