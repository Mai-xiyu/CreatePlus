package org.xiyu.yee.createplus.utils;

import java.io.IOException;
import java.nio.file.*;

public class FileUtils {
    private static final String MOD_FOLDER = "createplus";
    private static final String BUILDINGS_FOLDER = "buildings";
    
    public static Path getModDirectory() throws IOException {
        Path modDir = Paths.get(MOD_FOLDER);
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