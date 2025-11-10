package it.dominick.th.manager;

import it.dominick.th.TreasureHunt;
import it.dominick.th.model.TreasureRecord;
import it.dominick.th.util.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.Map;
import java.util.Objects;

public class TreasureClaimManager implements Listener {

    private final TreasureManager treasureManager;
    private final TreasurePlacementManager placementManager;
    private final TreasureHunt plugin;

    public TreasureClaimManager(TreasureHunt plugin, TreasureManager treasureManager, TreasurePlacementManager placementManager) {
        this.plugin = plugin;
        this.treasureManager = treasureManager;
        this.placementManager = placementManager;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getClickedBlock() == null) return;
        Player player = event.getPlayer();

        if (placementManager != null && placementManager.isPlayerPending(player.getUniqueId())) return;

        Location loc = event.getClickedBlock().getLocation();
        String world = Objects.requireNonNull(loc.getWorld()).getName();
        int x = loc.getBlockX();
        int y = loc.getBlockY();
        int z = loc.getBlockZ();

        Map<String, TreasureRecord> cached = treasureManager.getCachedTreasures();
        TreasureRecord found = null;
        for (TreasureRecord r : cached.values()) {
            if (r.getWorld().equals(world) && r.getX() == x && r.getY() == y && r.getZ() == z) {
                found = r;
                break;
            }
        }

        if (found == null) return;

        final String id = found.getId();
        final String cmdTemplate = found.getCommand();

        var config = plugin.getConfigManager();

        treasureManager.isRedeemed(player.getUniqueId(), id).thenAccept(already -> {
            if (already) {
                ChatUtils.send(player, config.getString("claimTreasure.already-claimed"));
                return;
            }

            treasureManager.redeemTreasure(player.getUniqueId(), id).thenAccept(success -> {
                if (!success) {
                    ChatUtils.send(player, config.getString("claimTreasure.claim-failed"));
                    return;
                }

                Bukkit.getScheduler().runTask(plugin, () -> {
                    if (cmdTemplate != null) {
                        String exec = cmdTemplate.replace("%player%", player.getName());
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), exec);
                    }
                });

                ChatUtils.send(player, config.getString("claimTreasure.claim-success"), "%id%", id);
            });
        });

        event.setCancelled(true);
    }
}
