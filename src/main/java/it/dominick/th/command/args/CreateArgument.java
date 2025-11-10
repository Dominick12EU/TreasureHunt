package it.dominick.th.command.args;

import it.dominick.th.TreasureHunt;
import it.dominick.th.config.ConfigManager;
import it.dominick.th.manager.TreasurePlacementManager;
import org.bukkit.entity.Player;

public class CreateArgument extends Argument {

    public CreateArgument(ConfigManager config) {
        super(config, "/th create <id> <cmd>", "treasurehunt.admin");
    }

    @Override
    public void execute(Player player, String[] args) {
        String id = args[1];
        StringBuilder sb = new StringBuilder();
        for (int i = 2; i < args.length; i++) {
            if (i > 2) sb.append(' ');
            sb.append(args[i]);
        }
        String cmd = sb.toString();

        TreasureHunt plugin = TreasureHunt.getInstance();
        TreasurePlacementManager manager = plugin.getTreasureManager().getPlacementManager();

        manager.waitForPlacement(player, id, cmd, config.getInt("creationTimeout"));
    }

    @Override
    public int minimumArgs() {
        return 3;
    }
}
