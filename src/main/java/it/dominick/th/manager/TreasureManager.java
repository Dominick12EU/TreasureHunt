package it.dominick.th.manager;

import it.dominick.th.TreasureHunt;
import it.dominick.th.repository.DatabaseRepository;
import it.dominick.th.repository.TreasureRepository;
import lombok.Getter;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class TreasureManager {

    private final TreasureHunt plugin;
    private final DatabaseRepository dbRepo;
    private final TreasureRepository treasureRepo;
    @Getter
    private final TreasurePlacementManager placementManager;

    public TreasureManager(TreasureHunt plugin, DatabaseRepository dbRepo) {
        this.plugin = plugin;
        this.dbRepo = dbRepo;
        this.treasureRepo = new TreasureRepository(dbRepo);
        this.placementManager = new TreasurePlacementManager(plugin, treasureRepo);
    }

    public CompletableFuture<Void> init() {
        return treasureRepo.createTableIfNotExists()
                .thenRun(() -> plugin.getLogger().info("TreasureManager initialized and table ensured."))
                .exceptionally(ex -> {
                    plugin.getLogger().log(Level.SEVERE, "Failed to initialize TreasureManager", ex);
                    return null;
                });
    }

    public CompletableFuture<Boolean> redeemTreasure(UUID player, String treasureId) {
        return treasureRepo.addRedeemed(player, treasureId);
    }

    public CompletableFuture<Boolean> unredeemTreasure(UUID player, String treasureId) {
        return treasureRepo.removeRedeemed(player, treasureId);
    }

    public CompletableFuture<Boolean> isRedeemed(UUID player, String treasureId) {
        return treasureRepo.isRedeemed(player, treasureId);
    }

    public CompletableFuture<List<String>> getRedeemed(UUID player) {
        return treasureRepo.getRedeemedForPlayer(player);
    }

    public void close() {
        try {
            dbRepo.close();
        } catch (Exception ex) {
            plugin.getLogger().log(Level.WARNING, "Error closing TreasureManager resources", ex);
        }
    }
}
