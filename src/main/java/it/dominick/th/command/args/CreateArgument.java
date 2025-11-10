package it.dominick.th.command.args;

import it.dominick.th.config.ConfigManager;
import it.dominick.th.manager.TreasurePlacementManager;
import it.dominick.th.TreasureHunt;
import it.dominick.th.util.ChatUtils;
import org.bukkit.entity.Player;

public class CreateArgument extends Argument {

    public CreateArgument(ConfigManager config) {
        super(config, "/th create <id> <command>", "treasurehunt.admin");
    }

    @Override
    public void execute(Player player, String[] args) {
        if (args.length < 3) {
            ChatUtils.send(player, config.getString("global.wrong-command-syntax"), "%command%", command());
            return;
        }

        String id = args[1];
        StringBuilder sb = new StringBuilder();
        for (int i = 2; i < args.length; i++) {
            if (i > 2) sb.append(' ');
            sb.append(args[i]);
        }
        String cmd = sb.toString();

        TreasureHunt plugin = TreasureHunt.getInstance();
        TreasurePlacementManager manager = plugin.getTreasureManager().getPlacementManager();

        manager.waitForPlacement(player, id, cmd, 30);
    }

    @Override
    public int minimumArgs() {
        return 3;
    }
}
