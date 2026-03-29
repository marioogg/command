package me.marioogg.command.bungee;

import lombok.Getter;
import lombok.SneakyThrows;
import me.marioogg.command.bungee.node.BungeeCommandNode;
import me.marioogg.command.bungee.parameter.BungeeParamProcessor;
import me.marioogg.command.common.flag.FlagNode;
import me.marioogg.command.common.help.HelpNode;
import me.marioogg.command.bukkit.node.ArgumentNode;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Raw Bungee command entry point for registered command nodes.
 */
public class BungeeCommand extends Command implements TabExecutor {
    @Getter private static final HashMap<String, BungeeCommand> commands = new HashMap<>();

    public BungeeCommand(String root) {
        super(root);
        commands.put(root.toLowerCase(), this);
        me.marioogg.command.bungee.BungeeCommandHandler.getPlugin().getProxy().getPluginManager().registerCommand(
                me.marioogg.command.bungee.BungeeCommandHandler.getPlugin(), this);
    }

    @SneakyThrows
    @Override
    public void execute(CommandSender sender, String[] args) {
        List<BungeeCommandNode> sortedNodes = BungeeCommandNode.getNodes().stream()
                .sorted(Comparator.comparingInt(node -> node.getMatchProbability(sender, getName(), args, false)))
                .collect(Collectors.toList());

        BungeeCommandNode node = sortedNodes.get(sortedNodes.size() - 1);

        if (node.getMatchProbability(sender, getName(), args, false) < 90) {
            if (node.getHelpNodes().isEmpty()) {
                node.sendUsageMessage(sender);
                return;
            }

            HelpNode helpNode = node.getHelpNodes().get(0);

            if (!helpNode.getPermission().isEmpty() && !sender.hasPermission(helpNode.getPermission())) {
                sender.sendMessage(new TextComponent(BungeeCommandHandler.getNoPermissionMessage()));
                return;
            }

            helpNode.getMethod().invoke(helpNode.getParentClass(), sender);
            return;
        }

        node.execute(sender, args);
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        try {
            List<BungeeCommandNode> sortedNodes = BungeeCommandNode.getNodes().stream()
                    .sorted(Comparator.comparingInt(node -> node.getMatchProbability(sender, getName(), args, true)))
                    .collect(Collectors.toList());

            BungeeCommandNode node = sortedNodes.get(sortedNodes.size() - 1);

            if (!node.isAllowComplete()) return new ArrayList<>();

            if (node.getMatchProbability(sender, getName(), args, true) >= 50) {
                int extraLength = node.getNames().get(0).split(" ").length - 1;
                String currentArg = args.length > 0 ? args[args.length - 1] : "";

                List<String> positionalSoFar = new ArrayList<>();
                for (int i = extraLength; i < args.length - 1; i++) {
                    String a = args[i];
                    if (node.getFlagNodes().stream().anyMatch(fn -> fn.matches(a))) continue;
                    positionalSoFar.add(a);
                }

                List<String> completions = new ArrayList<>();

                if (positionalSoFar.size() < node.getParameters().size()) {
                    ArgumentNode argumentNode = node.getParameters().get(positionalSoFar.size());
                    completions.addAll(new BungeeParamProcessor(argumentNode, currentArg, sender).getTabComplete());
                }

                Set<String> usedFlags = new HashSet<>();
                for (int i = extraLength; i < args.length - 1; i++) {
                    String a = args[i];
                    node.getFlagNodes().stream().filter(fn -> fn.matches(a)).findFirst()
                            .ifPresent(fn -> usedFlags.add(fn.getValue()));
                }
                for (FlagNode fn : node.getFlagNodes()) {
                    if (usedFlags.contains(fn.getValue())) continue;
                    for (String token : fn.getTokens()) {
                        if (token.toLowerCase().startsWith(currentArg.toLowerCase())) completions.add(token);
                    }
                }

                return completions;
            }

            return sortedNodes.stream()
                    .filter(sortedNode -> sortedNode.getPermission().isEmpty() || sender.hasPermission(sortedNode.getPermission()))
                    .map(sortedNode -> sortedNode.getNames().stream()
                            .map(name -> name.split(" "))
                            .filter(splitName -> splitName[0].equalsIgnoreCase(getName()))
                            .filter(splitName -> splitName.length > args.length)
                            .map(splitName -> splitName[args.length])
                            .collect(Collectors.toList()))
                    .flatMap(List::stream)
                    .filter(name -> name.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
}
