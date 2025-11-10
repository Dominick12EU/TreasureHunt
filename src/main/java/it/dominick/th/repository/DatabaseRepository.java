package it.dominick.th.repository;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import it.dominick.th.TreasureHunt;
import it.dominick.th.config.ConfigFile;
import it.dominick.th.config.ConfigManager;
import lombok.Getter;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.logging.Level;

public class DatabaseRepository {

    private final TreasureHunt plugin;

    @Getter
    private final HikariDataSource dataSource;
    private final ExecutorService executor;

    public DatabaseRepository() {
        plugin = TreasureHunt.getInstance();
        ConfigManager config = plugin.getConfigManager();

        String host = config.getString(ConfigFile.CONFIG, "database.host");
        int port = config.getIntOrDefault(ConfigFile.CONFIG, "database.port", 3306);
        String database = config.getString(ConfigFile.CONFIG, "database.name");
        String user = config.getString(ConfigFile.CONFIG, "database.user");
        String pass = config.getString(ConfigFile.CONFIG, "database.password");
        boolean useSsl = config.getBoolean(ConfigFile.CONFIG, "database.useSSL");
        int poolSize = config.getIntOrDefault(ConfigFile.CONFIG, "database.maximumPoolSize", 4);

        String jdbcUrl = String.format("jdbc:mysql://%s:%d/%s?useSSL=%s&serverTimezone=UTC", host, port, database, useSsl);

        try {
            HikariConfig cfg = new HikariConfig();
            cfg.setJdbcUrl(jdbcUrl);
            cfg.setUsername(user);
            cfg.setPassword(pass);
            cfg.setMaximumPoolSize(Math.max(1, poolSize));
            cfg.setPoolName("TH-Hikari-Pool");
            cfg.addDataSourceProperty("cachePrepStmts", "true");
            cfg.addDataSourceProperty("prepStmtCacheSize", "250");
            cfg.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

            this.dataSource = new HikariDataSource(cfg);

            AtomicInteger counter = new AtomicInteger(1);
            this.executor = Executors.newFixedThreadPool(Math.max(1, poolSize), r -> {
                Thread t = new Thread(r, "TH-DB-Worker-" + counter.getAndIncrement());
                t.setDaemon(true);
                return t;
            });

            plugin.getLogger().info("Database initialized successfully");
        } catch (Exception ex) {
            plugin.getLogger().log(Level.SEVERE, "Failed to initialize Database", ex);
            throw ex;
        }
    }

    public <T> CompletableFuture<T> supplyAsync(Supplier<T> supplier) {
        return CompletableFuture.supplyAsync(supplier, executor);
    }

    public CompletableFuture<Void> runAsync(Runnable runnable) {
        return CompletableFuture.runAsync(runnable, executor);
    }

    public void close() {
        try {
            if (dataSource != null && !dataSource.isClosed()) {
                dataSource.close();
            }
        } catch (Exception ex) {
            plugin.getLogger().log(Level.WARNING, "Error closing HikariDataSource", ex);
        }

        try {
            executor.shutdownNow();
        } catch (Exception ex) {
            plugin.getLogger().log(Level.WARNING, "Error shutting down DB executor", ex);
        }
    }
}

