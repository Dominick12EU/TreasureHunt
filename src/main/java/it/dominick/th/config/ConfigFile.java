package it.dominick.th.config;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Getter
public class ConfigFile {

    public static final ConfigFile CONFIG = new ConfigFile("config.yml", "");
    public static final ConfigFile MESSAGES = new ConfigFile("messages.yml", "");

    public static final List<ConfigFile> VALUES;

    static {
        List<ConfigFile> vals = new ArrayList<>();
        for (Field f : ConfigFile.class.getDeclaredFields()) {
            if (!Modifier.isStatic(f.getModifiers())) continue;
            if (!f.getType().equals(ConfigFile.class)) continue;
            try {
                f.setAccessible(true);
                Object obj = f.get(null);
                if (obj instanceof ConfigFile cf) {
                    vals.add(cf);
                }
            } catch (IllegalAccessException ignored) {
            }
        }

        VALUES = Collections.unmodifiableList(vals);
    }

    private final String fileName;
    private final String subDirectory;

    public ConfigFile(@NotNull String fileName, @NotNull String subDirectory) {
        this.fileName = Objects.requireNonNull(fileName, "fileName cannot be null");
        this.subDirectory = Objects.requireNonNull(subDirectory, "subDirectory cannot be null");
    }

    @NotNull
    public String getFilePath() {
        return subDirectory.isEmpty() ? fileName : subDirectory + "/" + fileName;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ConfigFile that = (ConfigFile) obj;
        return fileName.equals(that.fileName) && subDirectory.equals(that.subDirectory);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fileName, subDirectory);
    }

    @Override
    public String toString() {
        return "ConfigFile{" +
                "fileName='" + fileName + '\'' +
                ", subDirectory='" + subDirectory + '\'' +
                ", filePath='" + getFilePath() + '\'' +
                '}';
    }
}
