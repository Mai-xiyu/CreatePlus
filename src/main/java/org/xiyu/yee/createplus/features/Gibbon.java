package org.xiyu.yee.createplus.features;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import java.util.Arrays;

public class Gibbon extends CreativePlusFeature {
    private ItemStack originalLeggings = ItemStack.EMPTY;
    private static final ItemStack GIBBON_LEGGINGS = createGibbonLeggings();

    public Gibbon() {
        super("gibbon", "通过特殊装备增加交互距离");
    }

    private static ItemStack createGibbonLeggings() {
        ItemStack leggings = new ItemStack(Items.CHAINMAIL_LEGGINGS);
        CompoundTag tag = new CompoundTag();

        // 设置显示信息
        CompoundTag display = new CompoundTag();
        display.putString("Name", "[{\"text\":\"CreatePlus\",\"color\":\"aqua\"},{\"text\":\"-\",\"italic\":false,\"bold\":true,\"color\":\"dark_red\"},{\"text\":\"长臂猿裤衩子\",\"italic\":false,\"bold\":true,\"color\":\"gold\"}]");
        
        ListTag lore = new ListTag();
        lore.add(StringTag.valueOf("[{\"text\":\"CreatePlus\",\"color\":\"aqua\"},{\"text\":\"-\",\"color\":\"dark_red\"},{\"text\":\"长臂猿裤衩子\",\"color\":\"gold\"}]"));
        lore.add(StringTag.valueOf("[{\"text\":\"作者：饩雨\",\"italic\":false}]"));
        display.put("Lore", lore);
        tag.put("display", display);

        // 添加附魔
        ListTag enchantments = new ListTag();
        String[] enchants = {
            "aqua_affinity:1", "bane_of_arthropods:255", "binding_curse:1", "blast_protection:255",
            "channeling:1", "depth_strider:255", "efficiency:255", "feather_falling:255",
            "fire_aspect:255", "fire_protection:255", "flame:1", "fortune:255", "frost_walker:255",
            "impaling:255", "infinity:1", "knockback:255", "looting:255", "loyalty:255",
            "luck_of_the_sea:255", "lure:255", "mending:1", "multishot:1", "piercing:255",
            "power:255", "projectile_protection:255", "protection:255", "punch:255",
            "quick_charge:255", "respiration:255", "riptide:255", "sharpness:255",
            "silk_touch:1", "smite:255", "soul_speed:255", "sweeping:255", "swift_sneak:255",
            "thorns:255", "unbreaking:255", "vanishing_curse:1"
        };

        for (String enchant : enchants) {
            String[] parts = enchant.split(":");
            CompoundTag enchantTag = new CompoundTag();
            enchantTag.putString("id", parts[0]);
            enchantTag.putInt("lvl", Integer.parseInt(parts[1]));
            enchantments.add(enchantTag);
        }
        tag.put("Enchantments", enchantments);

        // 添加属性修饰符
        ListTag attributes = new ListTag();
        String[] attrs = {
            "generic.armor_toughness:10240:-12522:68315:53515:-136630",
            "generic.attack_damage:10240:-12522:68615:53515:-137230",
            "generic.attack_knockback:10240:-12522:68915:53515:-137830",
            "generic.attack_speed:10240:-12522:69215:53515:-138430",
            "generic.knockback_resistance:10240:-12522:69515:53515:-139030",
            "forge:entity_reach:10240:-12522:69815:53515:-139630",
            "forge:block_reach:10240:-12522:70115:53515:-140230",
            "forge:nametag_distance:10240:-12522:70415:53515:-140830"
        };

        for (String attr : attrs) {
            String[] parts = attr.split(":");
            CompoundTag attrTag = new CompoundTag();
            
            String attributeName;
            if (parts[0].equals("forge")) {
                attributeName = "forge:" + parts[1];
                parts = Arrays.copyOfRange(parts, 2, parts.length);
            } else {
                attributeName = "minecraft:" + parts[0];
                parts = Arrays.copyOfRange(parts, 1, parts.length);
            }
            
            attrTag.putString("AttributeName", attributeName);
            attrTag.putDouble("Amount", Double.parseDouble(parts[0]));
            int[] uuid = {
                Integer.parseInt(parts[1]),
                Integer.parseInt(parts[2]),
                Integer.parseInt(parts[3]),
                Integer.parseInt(parts[4])
            };
            attrTag.putIntArray("UUID", uuid);
            attrTag.putString("Name", "1740865094613");
            attrTag.putString("Slot", "legs");
            attributes.add(attrTag);
        }
        tag.put("AttributeModifiers", attributes);

        // 其他标签
        tag.putBoolean("Unbreakable", true);
        tag.putInt("HideFlags", 7);
        
        leggings.setTag(tag);
        return leggings;
    }

    @Override
    public void onEnable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            // 保存原始装备并装备长臂猿裤子
            this.originalLeggings = mc.player.getInventory().armor.get(1).copy();
            mc.player.getInventory().armor.set(1, GIBBON_LEGGINGS.copy());
            mc.player.displayClientMessage(Component.literal("已装备长臂猿裤衩子"), true);
        }
    }

    @Override
    public void onDisable() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null) {
            // 恢复原始装备
            mc.player.getInventory().armor.set(1, this.originalLeggings);
            mc.player.displayClientMessage(Component.literal("已卸下长臂猿裤衩子"), true);
        }
    }

    @Override
    public void onTick() {
        // 不需要tick更新
    }
} 