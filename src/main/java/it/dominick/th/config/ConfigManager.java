package it.dominick.th.config;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import it.dominick.th.util.ChatUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class ConfigManager {

    private final JavaPlugin plugin;
    private final Path pluginFolder;

    private final Map<String, FileConfiguration> configCache = new ConcurrentHashMap<>();
    private final Map<String, Object> valueCache = new ConcurrentHashMap<>();

    private String cachedPrefix = null;

    public ConfigManager(@NotNull JavaPlugin plugin) {
        this.plugin = plugin;
        this.pluginFolder = plugin.getDataFolder().toPath();

        plugin.saveDefaultConfig();

        for (ConfigFile cfg : ConfigFile.VALUES) {
            if (cfg == ConfigFile.CONFIG) continue;
            createAndCopyResource(cfg.getFilePath(), cfg.getFilePath());
        }

        initializeConfigs();
    }

    private void initializeConfigs() {
        try {
            if (!Files.exists(pluginFolder)) {
                Files.createDirectories(pluginFolder);
            }

            loadAllConfigsToCache();

            cachedPrefix = getString(ConfigFile.MESSAGES, "global.prefix");
            plugin.getLogger().info("ConfigManager successfully initialized");
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Error during initialization of ConfigManager", e);
        }
    }

    public void loadAllConfigsToCache() {
        try {
            if (Files.exists(pluginFolder)) {
                loadConfigsFromFolder(pluginFolder.toFile(), "");
            }
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error while loading config files", e);
        }
    }

    private void loadConfigsFromFolder(@NotNull File folder, @NotNull String baseDir) {
        if (!folder.isDirectory()) {
            return;
        }

        File[] files = folder.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.isFile() && file.getName().endsWith(".yml")) {
                loadConfigToCache(file.getName(), baseDir);
            } else if (file.isDirectory()) {
                String newBaseDir = baseDir.isEmpty() ? file.getName() : baseDir + File.separator + file.getName();
                loadConfigsFromFolder(file, newBaseDir);
            }
        }
    }

    public void loadConfigToCache(@NotNull String fileName, @NotNull String baseDir) {
        try {
            String separator = baseDir.isEmpty() ? "" : File.separator;
            File configFile = pluginFolder.resolve(baseDir + separator + fileName).toFile();

            if (!configFile.exists()) {
                plugin.getLogger().warning("Configuration file not found: " + configFile.getPath());
                return;
            }

            FileConfiguration config = YamlConfiguration.loadConfiguration(configFile);

            if (config.getKeys(true).isEmpty()) {
                plugin.getLogger().warning("Empty configuration file: " + configFile.getPath());
                return;
            }

            String cacheKey = baseDir.isEmpty() ? fileName : baseDir + "/" + fileName;
            configCache.put(cacheKey, config);

        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Error while loading config: " + fileName, e);
        }
    }

    public String getString(@NotNull String key) {
        return getString(ConfigFile.MESSAGES, key, true);
    }

    @Nullable
    public String getString(@NotNull ConfigFile configFile, @NotNull String key) {
        return getString(configFile, key, configFile == ConfigFile.MESSAGES);
    }

    @Nullable
    public String getString(@NotNull ConfigFile configFile, @NotNull String key, boolean includePrefix) {
        Object value = get(configFile, key);

        if (!(value instanceof String string)) {
            return null;
        }

        if (includePrefix && cachedPrefix != null && !key.equals("global.prefix")) {
            string = ChatUtils.placeholder(string, "{prefix}", cachedPrefix);
        }

        return string;
    }

    @NotNull
    public String getStringOrDefault(@NotNull ConfigFile configFile, @NotNull String key, @NotNull String defaultValue) {
        String value = getString(configFile, key);
        return value != null ? value : defaultValue;
    }

    public boolean getBoolean(@NotNull String key) {
        return getBoolean(ConfigFile.CONFIG, key);
    }

    public boolean getBoolean(@NotNull ConfigFile configFile, @NotNull String key) {
        return getBooleanOrDefault(configFile, key, false);
    }

    public boolean getBooleanOrDefault(@NotNull ConfigFile configFile, @NotNull String key, boolean defaultValue) {
        FileConfiguration config = configCache.get(configFile.getFilePath());
        if (config == null) {
            return defaultValue;
        }

        if (!config.contains(key)) {
            return defaultValue;
        }

        return config.getBoolean(key, defaultValue);
    }

    public int getInt(@NotNull String key) {
        return getInt(ConfigFile.CONFIG, key);
    }

    public int getInt(@NotNull ConfigFile configFile, @NotNull String key) {
        return getIntOrDefault(configFile, key, 0);
    }

    public int getIntOrDefault(@NotNull ConfigFile configFile, @NotNull String key, int defaultValue) {
        FileConfiguration config = configCache.get(configFile.getFilePath());
        return config != null ? config.getInt(key, defaultValue) : defaultValue;
    }

    public double getDouble(@NotNull String key) {
        return getDouble(ConfigFile.CONFIG, key);
    }

    public double getDouble(@NotNull ConfigFile configFile, @NotNull String key) {
        return getDoubleOrDefault(configFile, key, 0.0);
    }

    public double getDoubleOrDefault(@NotNull ConfigFile configFile, @NotNull String key, double defaultValue) {
        FileConfiguration config = configCache.get(configFile.getFilePath());
        return config != null ? config.getDouble(key, defaultValue) : defaultValue;
    }

    public long getLong(@NotNull ConfigFile configFile, @NotNull String key) {
        return getLongOrDefault(configFile, key, 0L);
    }

    public long getLongOrDefault(@NotNull ConfigFile configFile, @NotNull String key, long defaultValue) {
        FileConfiguration config = configCache.get(configFile.getFilePath());
        return config != null ? config.getLong(key, defaultValue) : defaultValue;
    }

    @Nullable
    public Object get(@NotNull String key) {
        return get(ConfigFile.CONFIG, key);
    }

    @Nullable
    public Object get(@NotNull ConfigFile configFile, @NotNull String key) {
        FileConfiguration config = configCache.get(configFile.getFilePath());
        return config != null ? config.get(key) : null;
    }

    @Nullable
    public ConfigurationSection getConfigurationSection(@NotNull ConfigFile configFile, @NotNull String key) {
        FileConfiguration config = configCache.get(configFile.getFilePath());
        return config != null ? config.getConfigurationSection(key) : null;
    }

    @NotNull
    public List<String> getList(@NotNull String key) {
        return getList(ConfigFile.CONFIG, key);
    }

    @NotNull
    public List<String> getList(@NotNull ConfigFile configFile, @NotNull String key) {
        FileConfiguration config = configCache.get(configFile.getFilePath());
        if (config == null) {
            return new ArrayList<>();
        }

        List<String> list = config.getStringList(key);
        return list != null ? list : new ArrayList<>();
    }

    @NotNull
    public Set<String> getKeys(@NotNull ConfigFile configFile, @NotNull String section, boolean deep) {
        ConfigurationSection configSection = getConfigurationSection(configFile, section);
        if (configSection == null) {
            return new HashSet<>();
        }
        return configSection.getKeys(deep);
    }

    public boolean contains(@NotNull ConfigFile configFile, @NotNull String key) {
        FileConfiguration config = configCache.get(configFile.getFilePath());
        return config != null && config.contains(key);
    }

    public void set(@NotNull ConfigFile configFile, @NotNull String key, @Nullable Object value) {
        FileConfiguration config = getConfig(configFile);
        if (config == null) {
            plugin.getLogger().warning("Unable to set the value for the key: " + key);
            return;
        }

        config.set(key, value);
        saveConfig(configFile, config);

        if (key.equals("global.prefix")) {
            cachedPrefix = value != null ? value.toString() : null;
        }
    }

    public void saveConfig(@NotNull ConfigFile configFile, @NotNull FileConfiguration config) {
        saveConfig(configFile, config, configFile.getSubDirectory());
    }

    public void saveConfig(@NotNull ConfigFile configFile, @NotNull FileConfiguration config, @NotNull String subDirectory) {
        try {
            File configFileObject = pluginFolder.resolve(subDirectory).resolve(configFile.getFileName()).toFile();

            File parentDir = configFileObject.getParentFile();
            if (!parentDir.exists()) {
                parentDir.mkdirs();
            }

            config.save(configFileObject);
            loadConfigToCache(configFile.getFileName(), subDirectory);

        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Error while saving the configuration: " + configFile.getFileName(), e);
        }
    }

    @Nullable
    public FileConfiguration getConfig(@NotNull ConfigFile configFile) {
        FileConfiguration config = configCache.get(configFile.getFilePath());

        if (config == null) {
            config = createConfigIfNotExists(configFile);
        }

        return config;
    }

    @Nullable
    private FileConfiguration createConfigIfNotExists(@NotNull ConfigFile configFile) {
        try {
            Path configPath = pluginFolder.resolve(configFile.getSubDirectory()).resolve(configFile.getFileName());
            File configFileObject = configPath.toFile();

            if (!configFileObject.getParentFile().exists()) {
                configFileObject.getParentFile().mkdirs();
            }

            if (!configFileObject.exists()) {
                configFileObject.createNewFile();
            }

            FileConfiguration config = YamlConfiguration.loadConfiguration(configFileObject);
            configCache.put(configFile.getFilePath(), config);

            return config;

        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Unable to create configuration file: " + configFile.getFileName(), e);
            return null;
        }
    }

    public void deleteConfig(@NotNull ConfigFile configFile) {
        deleteConfig(configFile, configFile.getSubDirectory());
    }

    public void deleteConfig(@NotNull ConfigFile configFile, @NotNull String subDirectory) {
        try {
            Path configPath = pluginFolder.resolve(subDirectory).resolve(configFile.getFileName());
            Files.deleteIfExists(configPath);
            configCache.remove(configFile.getFilePath());

        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Error while deleting config: " + configFile.getFileName(), e);
        }
    }

    public void createFolderIfNotExists(@NotNull String folderName) {
        try {
            Path folder = pluginFolder.resolve(folderName);
            if (!Files.exists(folder)) {
                Files.createDirectories(folder);
            }
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Error creating folder: " + folderName, e);
        }
    }

    public void createAndCopyResource(@NotNull String resourceName, @NotNull String targetPath) {
        Path targetFile = pluginFolder.resolve(targetPath);

        if (Files.exists(targetFile)) {
            return;
        }

        try {
            Files.createDirectories(targetFile.getParent());

            try (InputStream inputStream = plugin.getResource(resourceName)) {
                if (inputStream == null) {
                    plugin.getLogger().warning("Resource not found: " + resourceName);
                    return;
                }

                Files.copy(inputStream, targetFile);
                plugin.getLogger().info("Resource '" + resourceName + "' copied successfully");
            }

            reloadConfigs();

        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Error while copying the resource: " + resourceName, e);
        }
    }

    public void printHelp(Player player) {
        String version = plugin.getDescription().getVersion();

        List<String> helpLines = Arrays.asList(
                "&r", 
                "<gradient:#209966:#67CB70><bold>TreasureHunt</bold> &7v" + version + "</gradient>",
                "&7By Dominick12",
                "&r",
                "&e/th help &8- &7Show this menu",
                "&e/th create <id> <command> &8- &7Create a treasure",
                "&e/th list &8- &7Show the list of all treasures",
                "&r"
        );

        ChatUtils.sendList(player, helpLines);
    }

    @NotNull
    public List<String> getFilesInFolder(@NotNull String folderName) {
        try {
            Path folder = pluginFolder.resolve(folderName);

            if (!Files.exists(folder) || !Files.isDirectory(folder)) {
                return new ArrayList<>();
            }

            return Files.list(folder)
                    .filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .collect(Collectors.toList());

        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Error reading folder: " + folderName, e);
            return new ArrayList<>();
        }
    }

    public void reloadConfigs() {
        configCache.clear();
        valueCache.clear();
        loadAllConfigsToCache();

        cachedPrefix = getString(ConfigFile.MESSAGES, "global.prefix");
        plugin.getLogger().info("All configurations have been reloaded.");
    }

    public void reloadConfig(@NotNull ConfigFile configFile) {
        configCache.remove(configFile.getFilePath());
        loadConfigToCache(configFile.getFileName(), configFile.getSubDirectory());

        if (configFile == ConfigFile.MESSAGES) {
            cachedPrefix = getString(ConfigFile.MESSAGES, "global.prefix");
        }
    }

    public int getLoadedConfigsCount() {
        return configCache.size();
    }

    public boolean isConfigLoaded(@NotNull ConfigFile configFile) {
        return configCache.containsKey(configFile.getFilePath());
    }

    public void clearValueCache() {
        valueCache.clear();
    }
}
