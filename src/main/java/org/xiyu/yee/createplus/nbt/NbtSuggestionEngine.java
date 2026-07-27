package org.xiyu.yee.createplus.nbt;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.xiyu.yee.createplus.Createplus;
import org.xiyu.yee.createplus.features.CommandStudio;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 轻量 NBT 键补全（自研，参考 mc-nbtac 思路，不复制其 LGPL 源码）。
 */
public final class NbtSuggestionEngine {
    private static final Map<String, List<String>> TAGS = new LinkedHashMap<>();
    private static boolean loaded;

    private NbtSuggestionEngine() {
    }

    public static boolean enabled() {
        if (Createplus.FEATURE_MANAGER == null) return false;
        CommandStudio studio = Createplus.FEATURE_MANAGER.get(CommandStudio.class);
        return studio != null && studio.shouldSuggestNbt();
    }

    public static void ensureLoaded() {
        if (loaded) return;
        loaded = true;
        loadDefaults();
        try {
            var rm = Minecraft.getInstance().getResourceManager();
            var opt = rm.getResource(new ResourceLocation(Createplus.MODID, "nbt/suggestions.json"));
            if (opt.isPresent()) {
                try (var in = new InputStreamReader(opt.get().open(), StandardCharsets.UTF_8)) {
                    JsonObject root = JsonParser.parseReader(in).getAsJsonObject();
                    for (Map.Entry<String, JsonElement> e : root.entrySet()) {
                        List<String> list = new ArrayList<>();
                        JsonArray arr = e.getValue().getAsJsonArray();
                        for (JsonElement el : arr) list.add(el.getAsString());
                        TAGS.put(e.getKey(), list);
                    }
                }
            }
        } catch (Exception e) {
            Createplus.LOGGER.warn("CreatePlus: failed to load NBT suggestions json", e);
        }
    }

    private static void loadDefaults() {
        TAGS.put("entity", List.of(
            "CustomName", "CustomNameVisible", "NoAI", "Silent", "Invulnerable", "Glowing",
            "PersistenceRequired", "Health", "AbsorptionAmount", "Pos", "Motion", "Rotation",
            "HandItems", "ArmorItems", "HandDropChances", "ArmorDropChances", "ActiveEffects",
            "Attributes", "Tags", "UUID", "Fire", "Air", "Passengers", "Sitting"
        ));
        TAGS.put("item", List.of(
            "display", "Enchantments", "Damage", "Unbreakable", "CustomModelData",
            "AttributeModifiers", "HideFlags", "RepairCost", "SkullOwner", "BlockEntityTag",
            "EntityTag", "Potion", "CustomPotionEffects", "StoredEnchantments", "Items"
        ));
        TAGS.put("block", List.of(
            "Command", "LastOutput", "SuccessCount", "TrackOutput", "auto", "conditionMet",
            "Items", "Lock", "LootTable", "RecordItem", "Book", "Text1", "Text2", "Text3", "Text4"
        ));
        TAGS.put("generic", List.of(
            "id", "Count", "tag", "Name", "Value", "Level", "Ambient", "ShowParticles", "Duration"
        ));
    }

    public static CompletableFuture<Suggestions> suggest(SuggestionsBuilder builder, String category) {
        ensureLoaded();
        String remaining = builder.getRemaining().toLowerCase();
        // 取当前片段最后一个键前缀
        String prefix = remaining;
        int brace = Math.max(remaining.lastIndexOf('{'), remaining.lastIndexOf(','));
        if (brace >= 0) prefix = remaining.substring(brace + 1).trim();
        int colon = prefix.indexOf(':');
        if (colon >= 0) {
            return builder.buildFuture(); // 值部位，留给原版
        }
        prefix = prefix.replace("\"", "");

        List<String> keys = new ArrayList<>();
        keys.addAll(TAGS.getOrDefault(category, Collections.emptyList()));
        keys.addAll(TAGS.getOrDefault("generic", Collections.emptyList()));
        String p = prefix.toLowerCase();
        for (String key : keys) {
            if (p.isEmpty() || key.toLowerCase().startsWith(p)) {
                builder.suggest(key);
            }
        }
        return builder.buildFuture();
    }
}
