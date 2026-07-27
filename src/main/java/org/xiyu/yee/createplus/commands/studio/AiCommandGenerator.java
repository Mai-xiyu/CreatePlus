package org.xiyu.yee.createplus.commands.studio;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import org.xiyu.yee.createplus.Createplus;
import org.xiyu.yee.createplus.features.CommandStudio;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * OpenAI 兼容 Chat Completions。支持普通指令 / OOC / 两者。
 * 返回前用游戏指令树校验；失败自动带错误重试一次。绝不执行。
 */
public final class AiCommandGenerator {
    private static final HttpClient HTTP = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(15))
        .build();

    public enum Mode {
        COMMAND,
        OOC,
        BOTH
    }

    public record AiResult(boolean ok, String command, String ooc, String message, boolean retried) {
    }

    private AiCommandGenerator() {
    }

    public static void generateAsync(String playerRequest, Mode mode, Consumer<AiResult> callback) {
        CompletableFuture.supplyAsync(() -> generate(playerRequest, mode == null ? Mode.COMMAND : mode))
            .whenComplete((result, err) -> Minecraft.getInstance().execute(() -> {
                if (err != null) {
                    callback.accept(new AiResult(false, null, null,
                        "请求失败: " + err.getMessage(), false));
                } else {
                    callback.accept(result);
                }
            }));
    }

    private static AiResult generate(String playerRequest, Mode mode) {
        CommandStudio studio = Createplus.FEATURE_MANAGER.get(CommandStudio.class);
        if (studio == null) {
            return new AiResult(false, null, null, "CommandStudio 未注册", false);
        }
        if (studio.getAiApiKey() == null || studio.getAiApiKey().isBlank()) {
            return new AiResult(false, null, null, "请先在设置中填写 AI API Key", false);
        }

        String docs = CommandTreeDocs.dumpForAi();
        String first = callModel(studio, systemPrompt(mode), userPrompt(playerRequest, docs, null, mode));
        if (first.startsWith("ERROR:")) {
            return new AiResult(false, null, null, first.substring(6).trim(), false);
        }

        Parsed parsed = parseOutput(first, mode);
        CommandValidator.ValidationResult v1 = CommandValidator.validate(parsed.primary);
        if (v1.ok()) {
            return success(v1.command(), parsed, mode, "校验通过", false);
        }

        String retryExtra = "Previous AI output:\n" + first
            + "\n\nValidation error at cursor " + v1.cursor() + ":\n" + v1.error()
            + "\nPlease fix and output ONLY the corrected content in the required format.";
        String second = callModel(studio, systemPrompt(mode), userPrompt(playerRequest, docs, retryExtra, mode));
        if (second.startsWith("ERROR:")) {
            return new AiResult(false, parsed.command, parsed.ooc,
                "首次校验失败: " + v1.error() + "；重试请求失败: " + second.substring(6).trim(), true);
        }
        Parsed parsed2 = parseOutput(second, mode);
        CommandValidator.ValidationResult v2 = CommandValidator.validate(parsed2.primary);
        if (v2.ok()) {
            return success(v2.command(), parsed2, mode, "重试后校验通过（首次: " + v1.error() + "）", true);
        }
        return new AiResult(false, parsed2.command, parsed2.ooc,
            "重试后仍失败: " + v2.error() + " (cursor=" + v2.cursor() + ")", true);
    }

    private static AiResult success(String validatedPrimary, Parsed parsed, Mode mode, String msg, boolean retried) {
        String command = parsed.command;
        String ooc = parsed.ooc;
        if (mode == Mode.COMMAND) {
            command = validatedPrimary;
            ooc = command;
        } else if (mode == Mode.OOC) {
            ooc = validatedPrimary;
            // 尝试从 AI 输出里拆出内层指令；没有就留空
            if (command == null || command.isBlank()) {
                command = "";
            }
        } else {
            // BOTH：校验的是 command；ooc 优先用 AI 给的，否则本地包装
            command = validatedPrimary;
                if (ooc == null || ooc.isBlank()) {
                    ooc = command;
                } else {
                    CommandValidator.ValidationResult vo = CommandValidator.validate(ooc);
                    if (!vo.ok()) {
                        ooc = command;
                    } else {
                        ooc = vo.command();
                    }
                }
        }
        return new AiResult(true, command, ooc, msg, retried);
    }

    private static String systemPrompt(Mode mode) {
        String base = "You are a Minecraft 1.20.1 (Java Edition) command expert. "
            + "Do not use markdown fences. Do not add a leading slash unless required by format. "
            + "Cover items, potions, entities, spawn eggs, summon, execute, fill, setblock, data, effect, give as needed. "
            + "Use only commands present in the provided local command tree when possible. ";
        return switch (mode) {
            case COMMAND -> base + "Output ONLY one raw command line.";
            case OOC -> base
                + "Output ONLY one single executable command line that fully achieves the request. "
                + "Do NOT output falling_block / command_block_minecart installer packs unless the player explicitly asks for a one-command installer. "
                + "Prefer a clean execute/particle/scoreboard/etc. command.";
            case BOTH -> base
                + "Output exactly two lines:\n"
                + "COMMAND: <single raw command>\n"
                + "OOC: <the same or an improved single executable command — NOT a falling_block installer unless asked>";
        };
    }

    private static String userPrompt(String request, String docs, String extra, Mode mode) {
        StringBuilder sb = new StringBuilder();
        sb.append("Output mode: ").append(mode.name()).append("\n");
        sb.append("Player request:\n").append(request).append("\n\n");
        sb.append("Local command tree / docs:\n").append(docs).append("\n");
        if (extra != null) sb.append("\n").append(extra).append("\n");
        return sb.toString();
    }

    private record Parsed(String primary, String command, String ooc) {
    }

    private static Parsed parseOutput(String content, Mode mode) {
        String raw = stripFence(content);
        if (mode == Mode.BOTH) {
            String command = "";
            String ooc = "";
            for (String line : raw.split("\n")) {
                String t = line.trim();
                if (t.regionMatches(true, 0, "COMMAND:", 0, 8)) {
                    command = CommandValidator.normalize(t.substring(8).trim());
                } else if (t.regionMatches(true, 0, "OOC:", 0, 4)) {
                    ooc = CommandValidator.normalize(t.substring(4).trim());
                }
            }
            if (command.isEmpty() && ooc.isEmpty()) {
                command = CommandValidator.normalize(raw);
            }
            String primary = !command.isEmpty() ? command : ooc;
            return new Parsed(primary, command, ooc);
        }
        if (mode == Mode.OOC) {
            String ooc = CommandValidator.normalize(raw);
            return new Parsed(ooc, "", ooc);
        }
        String command = CommandValidator.normalize(raw);
        return new Parsed(command, command, "");
    }

    private static String stripFence(String content) {
        if (content == null) return "";
        String s = content.trim();
        if (s.startsWith("```")) {
            int start = s.indexOf('\n');
            int end = s.lastIndexOf("```");
            if (start >= 0 && end > start) {
                s = s.substring(start + 1, end).trim();
            }
        }
        return s;
    }

    private static String callModel(CommandStudio studio, String system, String user) {
        try {
            JsonObject body = new JsonObject();
            body.addProperty("model", studio.getAiModel());
            JsonArray messages = new JsonArray();
            JsonObject sys = new JsonObject();
            sys.addProperty("role", "system");
            sys.addProperty("content", system);
            messages.add(sys);
            JsonObject usr = new JsonObject();
            usr.addProperty("role", "user");
            usr.addProperty("content", user);
            messages.add(usr);
            body.add("messages", messages);
            body.addProperty("temperature", 0.2);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(studio.getAiEndpoint()))
                .timeout(Duration.ofSeconds(studio.getAiTimeoutSec()))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + studio.getAiApiKey())
                .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                .build();

            HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return "ERROR: HTTP " + response.statusCode() + " " + truncate(response.body(), 400);
            }
            JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
            JsonArray choices = json.getAsJsonArray("choices");
            if (choices == null || choices.isEmpty()) {
                return "ERROR: empty choices";
            }
            return choices.get(0).getAsJsonObject().getAsJsonObject("message").get("content").getAsString();
        } catch (Exception e) {
            return "ERROR: " + e.getMessage();
        }
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }
}
