package it.dominick.th.command.args;

import it.dominick.th.config.ConfigManager;
import org.bukkit.entity.Player;

public class HelpArgument extends Argument {

    public HelpArgument(ConfigManager config) {
        super(config, "/th help", "treasurehunt.admin");
    }

    @Override
    public void execute(Player player, String[] args) {
        config.printHelp(player);
    }

    @Override
    public int minimumArgs() {
        return 0;
    }
}