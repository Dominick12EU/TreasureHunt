package it.dominick.th.manager;

import it.dominick.th.TreasureHunt;
import it.dominick.th.config.ConfigManager;
import it.dominick.th.repository.TreasureRepository;
import it.dominick.th.util.ChatUtils;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class TreasurePlacementManager implements Listener {

    private final TreasureHunt plugin;
    private final ConfigManager config;
    private final TreasureRepository treasureRepo;
    private final Map<java.util.UUID, PendingPlacement> pending = new ConcurrentHashMap<>();

    public TreasurePlacementManager(TreasureHunt plugin, TreasureRepository treasureRepo) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();
        this.treasureRepo = treasureRepo;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public void waitForPlacement(Player player, String id, String command, long timeoutSeconds) {
        PendingPlacement placement = new PendingPlacement(id, command);
        pending.put(player.getUniqueId(), placement);

        Title.Times times = Title.Times.times(
                Duration.ofMillis(10 * 50L),
                Duration.ofMillis(20 * 5 * 50L),
                Duration.ofMillis(10 * 50L)
        );

        Title title = Title.title(
                ChatUtils.colorize(config.getString("createCmd.title")),
                ChatUtils.colorize(config.getString("createCmd.subtitle")),
                times
        );

        player.showTitle(title);

        ChatUtils.send(player, config.getString("createCmd.info-message"),
                "%seconds%", String.valueOf(timeoutSeconds));

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            PendingPlacement p = pending.remove(player.getUniqueId());
            if (p != null) {
                ChatUtils.send(player, config.getString("createCmd.timeout"));
            }
        }, TimeUnit.SECONDS.toSeconds(timeoutSeconds) * 20);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        PendingPlacement p = pending.get(player.getUniqueId());
        if (p == null) return;

        if (event.getClickedBlock() == null) return;

        Location loc = event.getClickedBlock().getLocation();
        String world = loc.getWorld() != null ? loc.getWorld().getName() : "world";
        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();

        treasureRepo.insertTreasure(p.id, world, x, y, z, p.command).thenAccept(success -> {
            if (success) {
                ChatUtils.send(player, config.getString("createCmd.success"), "%id%", p.id, "%x%", String.valueOf(x), "%y%", String.valueOf(y), "%z%", String.valueOf(z), "%world%", world);
            } else {
                ChatUtils.send(player, config.getString("createCmd.error"));
            }

            player.resetTitle();
        });

        pending.remove(player.getUniqueId());
        event.setCancelled(true);
    }

    private static class PendingPlacement {
        final String id;
        final String command;

        PendingPlacement(String id, String command) {
            this.id = id;
            this.command = command;
        }
    }
}
