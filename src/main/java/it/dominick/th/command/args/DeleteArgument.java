package it.dominick.th.command.args;

import it.dominick.th.config.ConfigManager;
import it.dominick.th.manager.TreasureManager;
import it.dominick.th.TreasureHunt;
import it.dominick.th.util.ChatUtils;
import org.bukkit.entity.Player;

public class DeleteArgument extends Argument {

    public DeleteArgument(ConfigManager config) {
        super(config, "/th delete <id>", "treasurehunt.admin");
    }

    @Override
    public void execute(Player player, String[] args) {
        if (args.length < 2) {
            ChatUtils.send(player, config.getString("global.wrong-command-syntax"), "%command%", command());
            return;
        }

        String id = args[1];
        TreasureHunt plugin = TreasureHunt.getInstance();
        TreasureManager manager = plugin.getTreasureManager();

        manager.deleteTreasure(id).thenAccept(affected -> {
            if (affected == null || affected == -1) {
                ChatUtils.send(player, config.getString("deleteCmd.delete-error"),
                        "%id%", id);
                return;
            }

            if (affected == 0) {
                ChatUtils.send(player, config.getString("deleteCmd.not-found"));
                return;
            }

            ChatUtils.send(player, config.getString("deleteCmd.deleted"),
                    "%id%", id);
        });
    }

    @Override
    public int minimumArgs() {
        return 2;
    }
}
