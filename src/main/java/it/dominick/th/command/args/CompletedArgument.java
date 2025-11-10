package it.dominick.th.command.args;

import it.dominick.th.TreasureHunt;
import it.dominick.th.config.ConfigManager;
import it.dominick.th.manager.TreasureManager;
import it.dominick.th.util.ChatUtils;
import org.bukkit.entity.Player;

public class CompletedArgument extends Argument {

    public CompletedArgument(ConfigManager config) {
        super(config, "/th completed <id>", "treasurehunt.admin");
    }

    @Override
    public void execute(Player player, String[] args) {
        if (args.length < 2) {
            ChatUtils.send(player, config.getString("global.wrong-command-syntax"), "%command%", command());
            return;
        }

        String id = args[1];
        TreasureManager manager = TreasureHunt.getInstance().getTreasureManager();

        manager.getPlayersWhoRedeemed(id).thenAccept(list -> {
            if (list == null || list.isEmpty()) {
                ChatUtils.send(player, config.getString("completedCmd.empty"), "%id%", id);
                return;
            }

            ChatUtils.send(player, config.getString("completedCmd.header"), "%id%", id);

            String rawItem = config.getString("completedCmd.item");

            for (String name : list) {
                final String line = rawItem.replace("%player%", name);
                ChatUtils.send(player, line);
            }
        });
    }

    @Override
    public int minimumArgs() {
        return 2;
    }
}
