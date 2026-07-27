package org.xiyu.yee.createplus.tabs;

import com.mojang.authlib.GameProfile;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 动态玩家背包标签页。
 *
 * 创造标签页数量在注册期就定死了，所以预留 {@link #MAX_TABS} 个槽位：
 * 打开创造栏时按当前在线玩家列表填充，每位玩家独占一页（主背包 + 盔甲 + 副手）。
 *
 * 标签标题用玩家名；图标用本地已解析皮肤的玩家头颅（写入完整 GameProfile，含 textures）。
 *
 * 本机主机 / 单人：能读到完整 {@link ServerPlayer} 背包。
 * 远端多人：别人的主背包原版不同步，只能展示盔甲与双手；自己仍是完整背包。
 */
public final class PlayerInventoryTabs {

    public static final int MAX_TABS = 16;

    private static final Component[] TITLES = new Component[MAX_TABS];
    private static final List<OnlineInv> CACHE = new ArrayList<>();

    private PlayerInventoryTabs() {
    }

    public static void init() {
        for (int i = 0; i < MAX_TABS; i++) {
            TITLES[i] = new DynamicTitle(i);
        }
        MinecraftForge.EVENT_BUS.addListener(PlayerInventoryTabs::onScreenOpen);
    }

    public static Component title(int index) {
        return TITLES[index];
    }

    /** 标签页图标：本地解析出的皮肤头颅。 */
    public static ItemStack icon(int index) {
        refreshCache();
        if (index < CACHE.size()) {
            return skullOf(CACHE.get(index).profile());
        }
        return new ItemStack(Items.GRAY_DYE);
    }

    public static void buildContents(BuildCreativeModeTabContentsEvent event, int index) {
        refreshCache();
        if (index >= CACHE.size()) {
            event.accept(hint(Items.BARRIER, "§7当前没有第 " + (index + 1) + " 位在线玩家", ChatFormatting.GRAY));
            return;
        }

        OnlineInv inv = CACHE.get(index);
        event.accept(skullOf(inv.profile()));
        event.accept(hint(Items.NAME_TAG, "§b" + inv.profile().getName()
            + (inv.full() ? " §7· 完整背包" : " §e· 仅装备（远端不同步主背包）"), ChatFormatting.AQUA));

        acceptSection(event, "盔甲", inv.armor());
        acceptSection(event, "副手", inv.offhand());
        acceptSection(event, "快捷栏", inv.hotbar());
        acceptSection(event, "主背包", inv.main());
    }

    private static void acceptSection(BuildCreativeModeTabContentsEvent event, String label, List<ItemStack> stacks) {
        boolean any = false;
        for (ItemStack stack : stacks) {
            if (!stack.isEmpty()) {
                any = true;
                break;
            }
        }
        if (!any) return;

        event.accept(hint(Items.PAPER, "§e— " + label + " —", ChatFormatting.YELLOW));
        for (ItemStack stack : stacks) {
            // 创造标签页强制要求 count == 1，背包里的堆叠直接 accept 会崩
            if (!stack.isEmpty()) event.accept(stack.copyWithCount(1));
        }
    }

    private static void onScreenOpen(ScreenEvent.Opening event) {
        if (!(event.getNewScreen() instanceof net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen)) {
            return;
        }
        refreshCache();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        net.minecraft.world.item.CreativeModeTabs.tryRebuildTabContents(
            mc.player.connection.enabledFeatures(),
            mc.player.canUseGameMasterBlocks(),
            mc.player.level().registryAccess());
    }

    private static synchronized void refreshCache() {
        CACHE.clear();
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        // 以客户端世界里的玩家为准：GameProfile 带本地已解析的皮肤
        for (AbstractClientPlayer client : mc.level.players()) {
            GameProfile profile = resolveProfile(client);
            Inventory inventory = null;
            boolean full = false;

            if (mc.hasSingleplayerServer() && mc.getSingleplayerServer() != null) {
                ServerPlayer server = mc.getSingleplayerServer().getPlayerList().getPlayer(client.getUUID());
                if (server != null) {
                    inventory = server.getInventory();
                    full = true;
                }
            } else if (client instanceof LocalPlayer) {
                inventory = ((LocalPlayer) client).getInventory();
                full = true;
            }

            if (full && inventory != null) {
                CACHE.add(fromInventory(profile, inventory, true));
            } else {
                CACHE.add(fromEquipment(profile, client));
            }
        }
    }

    /**
     * 尽量拿到带 textures 属性的 GameProfile。
     * 优先用玩家实体上的；没有就回落到 tab list / SkinManager 缓存。
     */
    private static GameProfile resolveProfile(AbstractClientPlayer player) {
        GameProfile profile = player.getGameProfile();
        if (hasTextures(profile)) return profile;

        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() != null) {
            PlayerInfo info = mc.getConnection().getPlayerInfo(player.getUUID());
            if (info != null && hasTextures(info.getProfile())) {
                return info.getProfile();
            }
        }

        // 触发一次皮肤查询，并把 insecure 信息里的纹理填进拷贝
        try {
            mc.getSkinManager().registerSkins(profile, (type, location, texture) -> {
            }, true);
            var textures = mc.getSkinManager().getInsecureSkinInformation(profile);
            if (!textures.isEmpty()) {
                GameProfile filled = new GameProfile(profile.getId(), profile.getName());
                filled.getProperties().putAll(profile.getProperties());
                // SkinManager 内部会往原 profile 写 property；再读一次
                if (hasTextures(profile)) return profile;
                return filled;
            }
        } catch (Exception ignored) {
        }
        return profile;
    }

    private static boolean hasTextures(GameProfile profile) {
        return profile != null
            && profile.getProperties() != null
            && profile.getProperties().containsKey("textures");
    }

    private static OnlineInv fromInventory(GameProfile profile, Inventory inventory, boolean full) {
        List<ItemStack> armor = new ArrayList<>(4);
        for (ItemStack stack : inventory.armor) armor.add(stack.copy());

        List<ItemStack> offhand = new ArrayList<>(1);
        for (ItemStack stack : inventory.offhand) offhand.add(stack.copy());

        List<ItemStack> hotbar = new ArrayList<>(9);
        List<ItemStack> main = new ArrayList<>(27);
        for (int i = 0; i < inventory.items.size(); i++) {
            ItemStack copy = inventory.items.get(i).copy();
            if (i < 9) hotbar.add(copy);
            else main.add(copy);
        }
        return new OnlineInv(profile, armor, offhand, hotbar, main, full);
    }

    private static OnlineInv fromEquipment(GameProfile profile, Player player) {
        List<ItemStack> armor = List.of(
            player.getItemBySlot(EquipmentSlot.FEET).copy(),
            player.getItemBySlot(EquipmentSlot.LEGS).copy(),
            player.getItemBySlot(EquipmentSlot.CHEST).copy(),
            player.getItemBySlot(EquipmentSlot.HEAD).copy());
        List<ItemStack> offhand = List.of(player.getItemBySlot(EquipmentSlot.OFFHAND).copy());
        List<ItemStack> hotbar = List.of(player.getItemBySlot(EquipmentSlot.MAINHAND).copy());
        return new OnlineInv(profile, armor, offhand, hotbar, List.of(), false);
    }

    /** 写入完整 GameProfile（含 textures），头颅才会显示本地那张皮。 */
    private static ItemStack skullOf(GameProfile profile) {
        ItemStack head = new ItemStack(Items.PLAYER_HEAD);
        CompoundTag owner = NbtUtils.writeGameProfile(new CompoundTag(), profile);
        head.getOrCreateTag().put("SkullOwner", owner);
        String name = profile.getName() == null ? "Player" : profile.getName();
        head.setHoverName(Component.literal(name)
            .withStyle(style -> style.withItalic(false).withColor(ChatFormatting.AQUA)));
        return head;
    }

    private static ItemStack hint(net.minecraft.world.item.Item item, String text, ChatFormatting color) {
        ItemStack stack = new ItemStack(item);
        stack.setHoverName(Component.literal(text)
            .withStyle(style -> style.withItalic(false).withColor(color)));
        return stack;
    }

    /** 标签页标题：就是玩家名。 */
    private static String labelFor(int index) {
        refreshCache();
        if (index < CACHE.size()) {
            String name = CACHE.get(index).profile().getName();
            return name == null || name.isEmpty() ? "Player" : name;
        }
        return "空位";
    }

    private record OnlineInv(
        GameProfile profile,
        List<ItemStack> armor,
        List<ItemStack> offhand,
        List<ItemStack> hotbar,
        List<ItemStack> main,
        boolean full
    ) {
    }

    /** 每次取显示名都现查在线列表，标题跟着玩家进出变。 */
    private static final class DynamicTitle implements Component {
        private final int index;

        private DynamicTitle(int index) {
            this.index = index;
        }

        private Component live() {
            return Component.literal(labelFor(index));
        }

        @Override
        public net.minecraft.network.chat.Style getStyle() {
            return live().getStyle();
        }

        @Override
        public net.minecraft.network.chat.ComponentContents getContents() {
            return live().getContents();
        }

        @Override
        public List<Component> getSiblings() {
            return live().getSiblings();
        }

        @Override
        public net.minecraft.util.FormattedCharSequence getVisualOrderText() {
            return live().getVisualOrderText();
        }

        @Override
        public String getString() {
            return labelFor(index);
        }
    }
}
