package it.dominick.th;

import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import it.dominick.th.command.TreasureHuntCommand;
import it.dominick.th.config.ConfigManager;
import it.dominick.th.manager.TreasureManager;
import it.dominick.th.repository.DatabaseRepository;
import lombok.Getter;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public final class TreasureHunt extends JavaPlugin {
    @Getter
    private static volatile TreasureHunt instance;
    @Getter
    private ConfigManager configManager;

    @Getter
    private DatabaseRepository databaseRepository;
    @Getter
    private TreasureManager treasureManager;

    @Override
    public void onLoad() {
        instance = this;
    }

    @Override
    public void onEnable() {
        this.configManager = new ConfigManager(this);

        try {
            this.databaseRepository = new DatabaseRepository();
            this.treasureManager = new TreasureManager(this, databaseRepository);

            this.treasureManager.init();

            TreasureHuntCommand cmd = new TreasureHuntCommand(this);

            this.getLifecycleManager().registerEventHandler(
                    LifecycleEvents.COMMANDS,
                    event -> {
                        Commands registrar = event.registrar();

                        registrar.register(
                                "th",
                                "Main command for TreasureHunt",
                                List.of("treasurehunt"),
                                cmd
                        );
                    }
            );
        } catch (Exception ex) {
            getLogger().severe("Failed to initialize database or treasure manager: " + ex.getMessage());
            ex.printStackTrace();
        }
    }

    @Override
    public void onDisable() {
        if (treasureManager != null) {
            treasureManager.close();
        }

        if (databaseRepository != null) {
            databaseRepository.close();
        }

        if (configManager != null) {
            configManager.clearValueCache();
        }
    }
}