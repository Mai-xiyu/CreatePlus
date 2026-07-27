package org.xiyu.yee.createplus.commands.studio;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.commands.SharedSuggestionProvider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/** 把本地已同步指令树压成给 AI 的文档。 */
public final class CommandTreeDocs {
    private static final int MAX_CHARS = 80_000;

    private CommandTreeDocs() {
    }

    public static List<String> listRootCommands() {
        CommandDispatcher<SharedSuggestionProvider> dispatcher = CommandValidator.dispatcher();
        if (dispatcher == null) return List.of();
        List<String> names = new ArrayList<>();
        for (CommandNode<SharedSuggestionProvider> child : dispatcher.getRoot().getChildren()) {
            if (child instanceof LiteralCommandNode<?> lit) {
                names.add(lit.getLiteral());
            }
        }
        names.sort(String.CASE_INSENSITIVE_ORDER);
        return names;
    }

    public static String dumpForAi() {
        CommandDispatcher<SharedSuggestionProvider> dispatcher = CommandValidator.dispatcher();
        if (dispatcher == null) {
            return "(no command tree — player not in world)";
        }
        StringBuilder sb = new StringBuilder(8192);
        sb.append("Available Minecraft commands on this client (synced from server, includes mod commands):\n");
        List<CommandNode<SharedSuggestionProvider>> roots = new ArrayList<>(dispatcher.getRoot().getChildren());
        roots.sort(Comparator.comparing(CommandNode::getName, String.CASE_INSENSITIVE_ORDER));
        for (CommandNode<SharedSuggestionProvider> node : roots) {
            appendNode(sb, node, 0);
            if (sb.length() > MAX_CHARS) {
                sb.append("\n... (truncated)\n");
                break;
            }
        }
        sb.append("\nNotes:\n");
        sb.append("- Prefer vanilla 1.20.1 syntax.\n");
        sb.append("- Cover items, potions, entities, spawn eggs, summon, execute, fill, setblock, data, effect, give as needed.\n");
        sb.append("- Output ONLY the raw command without leading slash or markdown.\n");
        return sb.toString();
    }

    private static void appendNode(StringBuilder sb, CommandNode<SharedSuggestionProvider> node, int depth) {
        if (depth > 4) return;
        indent(sb, depth);
        if (node instanceof LiteralCommandNode<?> lit) {
            sb.append(lit.getLiteral());
        } else if (node instanceof ArgumentCommandNode<?, ?> arg) {
            sb.append('<').append(arg.getName()).append(':')
                .append(arg.getType().getClass().getSimpleName()).append('>');
        } else {
            sb.append(node.getName());
        }
        if (node.getCommand() != null) sb.append(" [executable]");
        sb.append('\n');

        Collection<CommandNode<SharedSuggestionProvider>> children = node.getChildren();
        if (children.isEmpty() || depth >= 3) return;
        int shown = 0;
        for (CommandNode<SharedSuggestionProvider> child : children) {
            appendNode(sb, child, depth + 1);
            if (++shown >= 12 && depth > 0) {
                indent(sb, depth + 1);
                sb.append("...\n");
                break;
            }
            if (sb.length() > MAX_CHARS) return;
        }
    }

    private static void indent(StringBuilder sb, int depth) {
        for (int i = 0; i < depth; i++) sb.append("  ");
    }
}
