package org.xiyu.yee.createplus.commands.studio;

import com.google.gson.JsonObject;

/**
 * 记住上次 AI 提示词与生成结果，跨开关界面 / 重启保留。
 */
public final class AiSessionHistory {
    private static String prompt = "";
    private static String command = "";
    private static String ooc = "";
    private static String status = "";
    private static AiCommandGenerator.Mode mode = AiCommandGenerator.Mode.COMMAND;

    private AiSessionHistory() {
    }

    public static String getPrompt() {
        return prompt;
    }

    public static void setPrompt(String value) {
        prompt = value == null ? "" : value;
    }

    public static String getCommand() {
        return command;
    }

    public static void setCommand(String value) {
        command = value == null ? "" : value;
    }

    public static String getOoc() {
        return ooc;
    }

    public static void setOoc(String value) {
        ooc = value == null ? "" : value;
    }

    public static String getStatus() {
        return status;
    }

    public static void setStatus(String value) {
        status = value == null ? "" : value;
    }

    public static AiCommandGenerator.Mode getMode() {
        return mode;
    }

    public static void setMode(AiCommandGenerator.Mode value) {
        mode = value == null ? AiCommandGenerator.Mode.COMMAND : value;
    }

    public static void remember(String promptValue, AiCommandGenerator.Mode modeValue,
                                String commandValue, String oocValue, String statusValue) {
        setPrompt(promptValue);
        setMode(modeValue);
        setCommand(commandValue);
        setOoc(oocValue);
        setStatus(statusValue);
    }

    public static JsonObject toJson() {
        JsonObject o = new JsonObject();
        o.addProperty("prompt", prompt);
        o.addProperty("command", command);
        o.addProperty("ooc", ooc);
        o.addProperty("status", status);
        o.addProperty("mode", mode.name());
        return o;
    }

    public static void fromJson(JsonObject o) {
        if (o == null) return;
        if (o.has("prompt")) prompt = o.get("prompt").getAsString();
        if (o.has("command")) command = o.get("command").getAsString();
        if (o.has("ooc")) ooc = o.get("ooc").getAsString();
        if (o.has("status")) status = o.get("status").getAsString();
        if (o.has("mode")) {
            try {
                mode = AiCommandGenerator.Mode.valueOf(o.get("mode").getAsString());
            } catch (IllegalArgumentException ignored) {
                mode = AiCommandGenerator.Mode.COMMAND;
            }
        }
    }
}
