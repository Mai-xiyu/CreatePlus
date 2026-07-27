package org.xiyu.yee.createplus.utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 可拖动 HUD 布局。
 * CreatePlus 面板用绝对坐标；原版 HUD（血量/快捷栏/Boss 等）用相对原版位置的 dx/dy。
 */
public final class HudLayout {
    public static final String MINI_HUD = "mini_hud";
    public static final String BLOCK_INFO = "block_info";
    public static final String PING = "ping";
    public static final String SCOREBOARD = "scoreboard";
    public static final String HEALTH = "health";       // 心+盔甲+饥饿+氧气（原版同一方法）
    public static final String HOTBAR = "hotbar";
    public static final String EXPERIENCE = "experience";
    public static final String BOSS_BAR = "boss_bar";
    public static final String EFFECTS = "effects";

    /** 使用相对偏移的原版 HUD。 */
    public static final String[] OFFSET_IDS = {
        SCOREBOARD, HEALTH, HOTBAR, EXPERIENCE, BOSS_BAR, EFFECTS
    };

    public static final class Pos {
        public int x;
        public int y;
        public boolean custom;

        public Pos(int x, int y, boolean custom) {
            this.x = x;
            this.y = y;
            this.custom = custom;
        }

        public Pos copy() {
            return new Pos(x, y, custom);
        }
    }

    private static final Map<String, Pos> POSITIONS = new LinkedHashMap<>();

    static {
        POSITIONS.put(MINI_HUD, new Pos(4, 4, false));
        POSITIONS.put(BLOCK_INFO, new Pos(-1, 4, false));
        POSITIONS.put(PING, new Pos(-1, 4, false));
        for (String id : OFFSET_IDS) {
            POSITIONS.put(id, new Pos(0, 0, false));
        }
    }

    private HudLayout() {
    }

    public static Pos get(String id) {
        Pos p = POSITIONS.get(id);
        return p == null ? null : p.copy();
    }

    public static void set(String id, int x, int y) {
        Pos p = POSITIONS.computeIfAbsent(id, k -> new Pos(x, y, true));
        p.x = x;
        p.y = y;
        p.custom = true;
    }

    public static void clear(String id) {
        Pos p = POSITIONS.get(id);
        if (p != null) {
            p.custom = false;
            p.x = 0;
            p.y = 0;
        }
    }

    public static boolean hasCustom(String id) {
        Pos p = POSITIONS.get(id);
        return p != null && p.custom;
    }

    public static boolean isOffsetStyle(String id) {
        for (String o : OFFSET_IDS) {
            if (o.equals(id)) return true;
        }
        return false;
    }

    public static Set<String> ids() {
        return Collections.unmodifiableSet(POSITIONS.keySet());
    }

    public static int dx(String id) {
        Pos p = POSITIONS.get(id);
        return p != null && p.custom ? p.x : 0;
    }

    public static int dy(String id) {
        Pos p = POSITIONS.get(id);
        return p != null && p.custom ? p.y : 0;
    }

    public static int scoreboardDx() {
        return dx(SCOREBOARD);
    }

    public static int scoreboardDy() {
        return dy(SCOREBOARD);
    }

    public static JsonObject toJson() {
        JsonObject root = new JsonObject();
        for (Map.Entry<String, Pos> e : POSITIONS.entrySet()) {
            JsonObject o = new JsonObject();
            o.addProperty("x", e.getValue().x);
            o.addProperty("y", e.getValue().y);
            o.addProperty("custom", e.getValue().custom);
            root.add(e.getKey(), o);
        }
        return root;
    }

    public static void fromJson(JsonObject root) {
        if (root == null) return;
        for (Map.Entry<String, JsonElement> e : root.entrySet()) {
            if (!e.getValue().isJsonObject()) continue;
            JsonObject o = e.getValue().getAsJsonObject();
            int x = o.has("x") ? o.get("x").getAsInt() : 0;
            int y = o.has("y") ? o.get("y").getAsInt() : 0;
            boolean custom = o.has("custom") && o.get("custom").getAsBoolean();
            POSITIONS.put(e.getKey(), new Pos(x, y, custom));
        }
    }
}
