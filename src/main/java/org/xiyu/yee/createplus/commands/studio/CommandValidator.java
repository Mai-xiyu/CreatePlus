package org.xiyu.yee.createplus.commands.studio;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.commands.SharedSuggestionProvider;

/**
 * 用客户端已同步的指令树（含服务端模组指令）做可行性校验。
 * 不执行指令，只 parse。
 */
public final class CommandValidator {
    private CommandValidator() {
    }

    public record ValidationResult(boolean ok, String command, String error, int cursor) {
        public static ValidationResult success(String command) {
            return new ValidationResult(true, command, null, -1);
        }

        public static ValidationResult fail(String command, String error, int cursor) {
            return new ValidationResult(false, command, error, cursor);
        }
    }

    public static ValidationResult validate(String raw) {
        if (raw == null || raw.isBlank()) {
            return ValidationResult.fail("", "指令为空", 0);
        }
        String command = normalize(raw);
        CommandDispatcher<SharedSuggestionProvider> dispatcher = dispatcher();
        if (dispatcher == null) {
            return ValidationResult.fail(command, "尚未连接世界，无法获取指令树", 0);
        }

        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection == null) {
            return ValidationResult.fail(command, "连接不可用", 0);
        }

        try {
            ParseResults<SharedSuggestionProvider> results =
                dispatcher.parse(command, connection.getSuggestionsProvider());
            if (results.getReader().canRead()) {
                int cursor = results.getReader().getCursor();
                String leftover = results.getReader().getRemaining();
                return ValidationResult.fail(command,
                    "指令未完整解析，剩余: " + leftover, cursor);
            }
            if (!results.getExceptions().isEmpty()) {
                CommandSyntaxException ex = results.getExceptions().values().iterator().next();
                return ValidationResult.fail(command, ex.getMessage(),
                    ex.getCursor() >= 0 ? ex.getCursor() : results.getReader().getCursor());
            }
            // 无异常且读完 = 结构可行（权限/运行时参数另论）
            return ValidationResult.success(command);
        } catch (Exception e) {
            return ValidationResult.fail(command, e.getMessage() == null ? e.toString() : e.getMessage(), 0);
        }
    }

    public static String normalize(String raw) {
        String s = raw.trim();
        if (s.startsWith("/")) s = s.substring(1);
        // AI 有时返回多行，只取第一条非空
        int nl = s.indexOf('\n');
        if (nl >= 0) s = s.substring(0, nl).trim();
        return s;
    }

    @SuppressWarnings("unchecked")
    public static CommandDispatcher<SharedSuggestionProvider> dispatcher() {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection == null) return null;
        return (CommandDispatcher<SharedSuggestionProvider>) (CommandDispatcher<?>) connection.getCommands();
    }
}
