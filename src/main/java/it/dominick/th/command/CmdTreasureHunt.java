package it.dominick.th.command;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import it.dominick.th.TreasureHunt;
import it.dominick.th.command.args.Argument;
import it.dominick.th.command.args.HelpArgument;
import it.dominick.th.command.args.CreateArgument;
import it.dominick.th.command.args.ListArgument;
import it.dominick.th.command.args.DeleteArgument;
import it.dominick.th.command.args.CompletedArgument;
import it.dominick.th.config.ConfigManager;
import it.dominick.th.util.ChatUtils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.*;

public abstract class CmdTreasureHunt implements BasicCommand {

    protected final TreasureHunt plugin;
    protected final ConfigManager config;

    @lombok.Getter(lombok.AccessLevel.PROTECTED)
    private final Argument helpArgument;
    @lombok.Getter(lombok.AccessLevel.PROTECTED)
    private final Map<String, Argument> argumentMap = new HashMap<>();

    public CmdTreasureHunt(TreasureHunt plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfigManager();

        this.helpArgument = new HelpArgument(config);
        registerArgument("help", helpArgument);
        registerArgument("create", new CreateArgument(config));
        registerArgument("list", new ListArgument(config));
        registerArgument("delete", new DeleteArgument(config));
        registerArgument("completed", new CompletedArgument(config));
    }

    protected void registerArgument(String name, Argument argument) {
        argumentMap.put(name.toLowerCase(Locale.ROOT), argument);
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        CommandSender sender = source.getSender();

        if (!(sender instanceof Player player)) {
            ChatUtils.send(sender, config.getString("global.no-console"));
            return;
        }

        if (args.length == 0) {
            config.printHelp(player);
            return;
        }

        Argument argument = argumentMap.getOrDefault(args[0].toLowerCase(Locale.ROOT), helpArgument);

        if (!argument.hasPermission(player)) {
            ChatUtils.send(player, config.getString("global.insufficient-permission"));
            return;
        }

        if (argument.invalidArgs(args)) {
            ChatUtils.send(
                    player,
                    config.getString("global.wrong-command-syntax"),
                    "%command%",
                    argument.command()
            );
            return;
        }

        argument.execute(player, args);
    }

    @Override
    public Collection<String> suggest(CommandSourceStack source, String[] args) {
        CommandSender sender = source.getSender();

        if (!(sender instanceof Player player)) {
            return Collections.emptyList();
        }

        if (args.length == 0) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            String input = args[0].toLowerCase(Locale.ROOT);
            return argumentMap.keySet()
                    .stream()
                    .filter(name -> name.startsWith(input))
                    .toList();
        }

        Argument argument = argumentMap.get(args[0].toLowerCase(Locale.ROOT));
        if (argument == null) {
            return argumentMap.keySet()
                    .stream()
                    .filter(name -> name.startsWith(args[0].toLowerCase(Locale.ROOT)))
                    .toList();
        }

        return argument.completation(player, args);
    }
}