package it.dominick.th.repository;

import it.dominick.th.TreasureHunt;
import it.dominick.th.model.TreasureRecord;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class TreasureRepository {

    private final TreasureHunt plugin;
    private final DatabaseRepository db;
    private final String redeemedTable;
    private final String treasuresTable;

    public TreasureRepository(DatabaseRepository db) {
        plugin = TreasureHunt.getInstance();
        this.db = db;
        this.redeemedTable = "th_redeemed";
        this.treasuresTable = "th_treasures";
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
                """, redeemedTable);

        return db.runAsync(() -> {
            try (Connection conn = db.getDataSource().getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute(sql);
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, "Failed to create table: " + redeemedTable, ex);
            }
        }).thenCompose(v -> createTreasuresTableIfNotExists());
    }

    public CompletableFuture<Void> createTreasuresTableIfNotExists() {
        String sql = String.format("""
                CREATE TABLE IF NOT EXISTS `%s` (
                  `treasure_id` VARCHAR(128) NOT NULL,
                  `world` VARCHAR(128) NOT NULL,
                  `x` INT NOT NULL,
                  `y` INT NOT NULL,
                  `z` INT NOT NULL,
                  `command` TEXT NOT NULL,
                  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                  PRIMARY KEY (`treasure_id`),
                  INDEX (`world`, `x`, `y`, `z`)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
                """, treasuresTable);

        return db.runAsync(() -> {
            try (Connection conn = db.getDataSource().getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.execute(sql);
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, "Failed to create table: " + treasuresTable, ex);
            }
        });
    }

    public CompletableFuture<Boolean> addRedeemed(UUID player, String treasureId) {
        String sql = String.format("""
                INSERT INTO `%s` (player_uuid, treasure_id) VALUES (?, ?)
                """, redeemedTable);

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
                """, redeemedTable);

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
                """, redeemedTable);

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
                """, redeemedTable);

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

    public CompletableFuture<Boolean> insertTreasure(String treasureId, String world, int x, int y, int z, String command) {
        String sql = String.format("""
                INSERT INTO `%s` (treasure_id, world, x, y, z, command) VALUES (?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE world = VALUES(world), x = VALUES(x), y = VALUES(y), z = VALUES(z), command = VALUES(command)
                """, treasuresTable);

        return db.supplyAsync(() -> {
            try (Connection conn = db.getDataSource().getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, treasureId);
                ps.setString(2, world);
                ps.setInt(3, x);
                ps.setInt(4, y);
                ps.setInt(5, z);
                ps.setString(6, command);
                ps.executeUpdate();
                return true;
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, "Failed to insert/update treasure record", ex);
                return false;
            }
        });
    }

    public CompletableFuture<Integer> deleteTreasure(String treasureId) {
        String deleteTreasureSql = String.format("""
                DELETE FROM `%s` WHERE treasure_id = ?
                """, treasuresTable);

        String deleteRedeemedSql = String.format("""
                DELETE FROM `%s` WHERE treasure_id = ?
                """, redeemedTable);

        return db.supplyAsync(() -> {
            try (Connection conn = db.getDataSource().getConnection()) {
                conn.setAutoCommit(false);
                try (PreparedStatement ps1 = conn.prepareStatement(deleteRedeemedSql);
                     PreparedStatement ps2 = conn.prepareStatement(deleteTreasureSql)) {
                    ps1.setString(1, treasureId);
                    ps1.executeUpdate();

                    ps2.setString(1, treasureId);
                    int affected = ps2.executeUpdate();

                    conn.commit();
                    return affected;
                } catch (SQLException ex) {
                    try {
                        conn.rollback();
                    } catch (SQLException e) {
                        plugin.getLogger().log(Level.WARNING, "Failed to rollback deleteTreasure transaction", e);
                    }
                    plugin.getLogger().log(Level.SEVERE, "Failed to delete treasure: " + treasureId, ex);
                    return -1;
                } finally {
                    try {
                        conn.setAutoCommit(true);
                    } catch (SQLException ignored) {
                    }
                }
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, "Failed to delete treasure (connection): " + treasureId, ex);
                return -1;
            }
        });
    }

    public CompletableFuture<List<TreasureRecord>> getAllTreasures() {
        String sql = String.format("""
                SELECT treasure_id, world, x, y, z, command FROM `%s`
                """, treasuresTable);

        return db.supplyAsync(() -> {
            List<TreasureRecord> list = new ArrayList<>();
            try (Connection conn = db.getDataSource().getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String id = rs.getString(1);
                    String world = rs.getString(2);
                    int x = rs.getInt(3);
                    int y = rs.getInt(4);
                    int z = rs.getInt(5);
                    String command = rs.getString(6);
                    list.add(new TreasureRecord(id, world, x, y, z, command));
                }
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, "Failed to list treasures", ex);
            }
            return list;
        });
    }

    public CompletableFuture<List<String>> getPlayersRedeemed(String treasureId) {
        String sql = String.format("""
                SELECT player_uuid FROM `%s` WHERE treasure_id = ?
                """, redeemedTable);

        return db.supplyAsync(() -> {
            List<String> list = new ArrayList<>();
            try (Connection conn = db.getDataSource().getConnection();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, treasureId);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(rs.getString(1));
                    }
                }
            } catch (SQLException ex) {
                plugin.getLogger().log(Level.SEVERE, "Failed to list players redeemed for treasure: " + treasureId, ex);
            }
            return list;
        });
    }

}
