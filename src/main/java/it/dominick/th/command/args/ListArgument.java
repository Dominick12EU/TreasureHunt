package it.dominick.th.command.args;

import it.dominick.th.config.ConfigManager;
import it.dominick.th.manager.TreasureManager;
import it.dominick.th.model.TreasureRecord;
import it.dominick.th.TreasureHunt;
import it.dominick.th.util.ChatUtils;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class ListArgument extends Argument {

    public ListArgument(ConfigManager config) {
        super(config, "/th list", "treasurehunt.admin");
    }

    @Override
    public void execute(Player player, String[] args) {
        TreasureHunt plugin = TreasureHunt.getInstance();
        TreasureManager manager = plugin.getTreasureManager();

        List<TreasureRecord> records = new ArrayList<>(manager.getCachedTreasures().values());
        if (records.isEmpty()) {
            ChatUtils.send(player, config.getString("listCmd.no-treasures"));
            return;
        }

        ChatUtils.send(player, config.getString("listCmd.list-header"),
                "%count%", String.valueOf(records.size()));

        for (TreasureRecord r : records) {
            ChatUtils.send(player, config.getString("listCmd.list-item"),
                    "%id%", r.getId(),
                    "%world%", r.getWorld(),
                    "%pos%", r.getX() + "," + r.getY() + "," + r.getZ(),
                    "%cmd%", r.getCommand());
        }
    }

    @Override
    public int minimumArgs() {
        return 1;
    }
}
