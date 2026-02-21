package me.marioogg.command.bungee.bukkit;

import lombok.Getter;
import lombok.SneakyThrows;
import me.marioogg.command.bungee.node.BungeeCommandNode;
import me.marioogg.command.bungee.parameter.BungeeParamProcessor;
import me.marioogg.command.help.HelpNode;
import me.marioogg.command.node.ArgumentNode;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.util.*;
import java.util.stream.Collectors;

public class BungeeBukkitCommand extends Command implements TabExecutor {
    @Getter private static final HashMap<String, BungeeBukkitCommand> commands = new HashMap<>();

    public BungeeBukkitCommand(String root) {
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
                sender.sendMessage(new TextComponent(ChatColor.RED + "I'm sorry, although you do not have permission to execute this command."));
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
                int arg = (args.length - extraLength) - 1;

                if (arg < 0 || node.getParameters().size() < arg + 1) return new ArrayList<>();

                ArgumentNode argumentNode = node.getParameters().get(arg);
                return new BungeeParamProcessor(argumentNode, args[args.length - 1], sender).getTabComplete();
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

