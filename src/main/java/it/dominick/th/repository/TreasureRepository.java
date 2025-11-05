package it.dominick.th.repository;

import it.dominick.th.TreasureHunt;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class TreasureRepository {

    private final TreasureHunt plugin;
    private final DatabaseRepository db;
    private final String tableName;

    public TreasureRepository(DatabaseRepository db) {
        plugin = TreasureHunt.getInstance();
        this.db = db;
        this.tableName = "th_redeemed";
    }

    public CompletableFuture<Void> createTableIfNotExists() {
        String sql = String.format("""
                CREATE TABLE IF NOT EXISTS `%s` (
                  `id` INT UNSIGNED NOT NULL AUTO_INCREMENT,
                  `player_uuid` CHAR(36) NOT NULL,
                  `treasure_id` VARCHAR(128) NOT NULL,
                  `redeemed_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  PRIMARY KEY (`id`),
                  INDEX (`player_uuid`),
                  UNIQUE KEY `player_treasure_unique` (`player_uuid`, `treasure_id`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
                """, tableName);

        return db.runAsync(() -> {
            try (Connection conn = db.getDataSource().getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute(sql);
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, "Failed to create table: " + tableName, ex);
            }
        });
    }

    public CompletableFuture<Boolean> addRedeemed(UUID player, String treasureId) {
        String sql = String.format("""
                INSERT INTO `%s` (player_uuid, treasure_id) VALUES (?, ?)
                """, tableName);

        return db.supplyAsync(() -> {
            try (Connection conn = db.getDataSource().getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, player.toString());
                ps.setString(2, treasureId);
                ps.executeUpdate();
                return true;
            } catch (SQLException ex) {
                if (ex.getSQLState() != null && ex.getSQLState().startsWith("23")) {
                    return false;
                }
                plugin.getLogger().log(Level.SEVERE, "Failed to insert redeemed record", ex);
                return false;
            }
        });
    }

    public CompletableFuture<Boolean> removeRedeemed(UUID player, String treasureId) {
        String sql = String.format("""
                DELETE FROM `%s` WHERE player_uuid = ? AND treasure_id = ?
                """, tableName);

        return db.supplyAsync(() -> {
            try (Connection conn = db.getDataSource().getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, player.toString());
                ps.setString(2, treasureId);
                int updated = ps.executeUpdate();
                return updated > 0;
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, "Failed to delete redeemed record", ex);
                return false;
            }
        });
    }

    public CompletableFuture<Boolean> isRedeemed(UUID player, String treasureId) {
        String sql = String.format("""
                SELECT 1 FROM `%s` WHERE player_uuid = ? AND treasure_id = ? LIMIT 1
                """, tableName);

        return db.supplyAsync(() -> {
            try (Connection conn = db.getDataSource().getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, player.toString());
                ps.setString(2, treasureId);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, "Failed to query redeemed record", ex);
                return false;
            }
        });
    }

    public CompletableFuture<List<String>> getRedeemedForPlayer(UUID player) {
        String sql = String.format("""
                SELECT treasure_id FROM `%s` WHERE player_uuid = ?
                """, tableName);

        return db.supplyAsync(() -> {
            List<String> list = new ArrayList<>();
            try (Connection conn = db.getDataSource().getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, player.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(rs.getString(1));
                    }
                }
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, "Failed to list redeemed records for player", ex);
            }
            return list;
        });
    }
}
