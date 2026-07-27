package org.xiyu.yee.createplus.commands.studio;

/**
 * 导出指令。
 * <p>
 * 「OOC」在本模组里指<strong>一条可直接粘贴执行的命令</strong>，
 * 不再把简单指令强行包成 FallingBlock 安装包（那会又臭又长还容易错）。
 * 若确实需要一键落地命令方块，用 {@link #toInstaller(String)} 或 {@link #toGiveCommandBlock(String)}。
 */
public final class OocExporter {
    private OocExporter() {
    }

    public static String toRaw(String cmd) {
        return CommandValidator.normalize(cmd);
    }

    /** OOC = 单条命令本身。 */
    public static String toOoc(String cmd) {
        return toRaw(cmd);
    }

    public static String toGiveCommandBlock(String cmd) {
        String c = escapeNbt(toRaw(cmd));
        return "give @p command_block{BlockEntityTag:{Command:\"" + c
            + "\",auto:0b}} 1";
    }

    /**
     * 经典 FallingBlock 乘客式「安装包」：落地后留下装好指令的命令方块。
     * 仅当用户明确要一键安装机时使用。
     */
    public static String toInstaller(String cmd) {
        String c = escapeNbt(toRaw(cmd));
        return "summon falling_block ~ ~1 ~ {Time:1,BlockState:{Name:\"minecraft:redstone_block\"},Passengers:["
            + "{id:\"minecraft:falling_block\",Time:1,BlockState:{Name:\"minecraft:activator_rail\"},Passengers:["
            + "{id:\"minecraft:command_block_minecart\",Command:\"" + c + "\"},"
            + "{id:\"minecraft:command_block_minecart\",Command:\"setblock ~ ~-2 ~ command_block{Command:\\\""
            + c + "\\\"}\"},"
            + "{id:\"minecraft:command_block_minecart\",Command:\"kill @e[type=command_block_minecart,distance=..1]\"}"
            + "]}]}";
    }

    private static String escapeNbt(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
