package org.xiyu.yee.createplus.utils;

import net.minecraft.client.Minecraft;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class FileUtils {
    private static final String MOD_FOLDER = "createplus";
    private static final String BUILDINGS_FOLDER = "buildings";

    private FileUtils() {
    }

    /**
     * 模组数据目录，位于 .minecraft/createplus。
     *
     * 以前这里用的是相对路径，会跟着进程工作目录跑，
     * 导致不同启动器下配置存到不同地方。
     */
    public static Path getModDirectory() throws IOException {
        Path gameDir = Minecraft.getInstance().gameDirectory != null
            ? Minecraft.getInstance().gameDirectory.toPath()
            : Paths.get(".");
        Path modDir = gameDir.resolve(MOD_FOLDER);
        if (!Files.exists(modDir)) {
            Files.createDirectories(modDir);
        }
        return modDir;
    }

    public static Path getBuildingsDirectory() throws IOException {
        Path buildingsDir = getModDirectory().resolve(BUILDINGS_FOLDER);
        if (!Files.exists(buildingsDir)) {
            Files.createDirectories(buildingsDir);
        }
        return buildingsDir;
    }
}
