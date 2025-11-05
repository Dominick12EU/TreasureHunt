package it.dominick.th.command;

import com.google.common.collect.ImmutableList;
import it.dominick.th.TreasureHunt;
import it.dominick.th.command.args.*;
import it.dominick.th.config.ConfigManager;
import it.dominick.th.util.ChatUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CmdTreasureHunt implements TabExecutor {

    private final ConfigManager config;
    private final Argument helpArgument;
    private final Map<String, Argument> argumentMap;

    public CmdTreasureHunt() {
        config = TreasureHunt.getInstance().getConfigManager();
        argumentMap = new HashMap<>();

        helpArgument = new HelpArgument(config);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            ChatUtils.send(sender, config.getString("global.no-console"));
            return true;
        }
        if (args.length == 0) {
            config.printHelp(player);
            return true;
        }

        Argument argument = getArgument(args[0].toLowerCase());

        if (!argument.hasPermission(player)) {
            ChatUtils.send(player, config.getString("global.insufficient-permission"));
            return true;
        }

        if (argument.invalidArgs(args)) {
            ChatUtils.send(player, config.getString("global.wrong-command-syntax"), "%command%", argument.command());
            return true;
        }

        argument.execute(player, args);
        return true;
    }

    private Argument getArgument(String name) {
        return argumentMap.getOrDefault(name, helpArgument);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if(!(sender instanceof Player player) || args.length == 0) return ImmutableList.of();

        if (args.length == 1) {
            return argumentMap.keySet().parallelStream().filter(arg -> arg.startsWith(args[0])).toList();
        } else {
            Argument argument = argumentMap.get(args[0]);
            if(argument == null) return argumentMap.keySet().parallelStream().filter(arg -> arg.startsWith(args[0])).toList();
            return argument.completation(player, args);
        }
    }
}

