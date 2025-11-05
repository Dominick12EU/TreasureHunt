package it.dominick.th;

import it.dominick.th.config.ConfigManager;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

public final class TreasureHunt extends JavaPlugin {
    @Getter
    private static volatile TreasureHunt instance;
    @Getter
    private ConfigManager configManager;

    @Override
    public void onLoad() {
        instance = this;
    }

    @Override
    public void onEnable() {
        this.configManager = new ConfigManager(this);
    }

    @Override
    public void onDisable() {
        if (configManager != null) {
            configManager.clearValueCache();
        }
    }
}
