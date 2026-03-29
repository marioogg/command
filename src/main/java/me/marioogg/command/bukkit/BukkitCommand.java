package me.marioogg.command.bukkit;

import lombok.Getter;
import lombok.SneakyThrows;
import me.marioogg.command.common.flag.FlagNode;
import me.marioogg.command.common.help.HelpNode;
import me.marioogg.command.bukkit.node.ArgumentNode;
import me.marioogg.command.bukkit.node.CommandNode;
import me.marioogg.command.bukkit.parameter.ParamProcessor;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.slf4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Raw Bukkit command entry point for registered command nodes.
 */
public class BukkitCommand extends Command {
    @Getter private static final HashMap<String, BukkitCommand> commands = new HashMap<>();
    private static final Logger log = BukkitCommandHandler.getLogger();

    @SneakyThrows
    public BukkitCommand(String root) {
        super(root);
        commands.put(root.toLowerCase(), this);

        Field commandMap = BukkitCommandHandler.getPlugin().getServer().getClass().getDeclaredField("commandMap");
        commandMap.setAccessible(true);
        ((org.bukkit.command.CommandMap) commandMap.get(BukkitCommandHandler.getPlugin().getServer())).register(BukkitCommandHandler.getPlugin().getName(), this);
    }

    @SneakyThrows
    public boolean execute(CommandSender sender, String label, String[] args) {
        List<CommandNode> sortedNodes = CommandNode.getNodes().stream()
                .sorted(Comparator.comparingInt(node -> node.getMatchProbability(sender, label, args, false)))
                .toList();

        CommandNode node = sortedNodes.get(sortedNodes.size() - 1);
        if(node.getMatchProbability(sender, label, args, false) < 90) {
            if(node.getHelpNodes().isEmpty()) {
                node.sendUsageMessage(sender);
                return false;
            }

            HelpNode helpNode = node.getHelpNodes().get(0);

            if(!helpNode.getPermission().isEmpty() && !sender.hasPermission(helpNode.getPermission())) {
                sender.sendMessage(BukkitCommandHandler.getNoPermissionMessage());
                return false;
            }

            helpNode.getMethod().invoke(helpNode.getParentClass(), sender);
            return false;
        }

        node.execute(sender, args);
        return false;
    }

    @Override
    public List<String> tabComplete(CommandSender sender, String label, String[] args) throws IllegalArgumentException {
        try {
            List<CommandNode> sortedNodes = CommandNode.getNodes().stream()
                    .sorted(Comparator.comparingInt(node -> node.getMatchProbability(sender, label, args, true)))
                    .toList();

            CommandNode node = sortedNodes.get(sortedNodes.size() - 1);
            if (!node.isAllowComplete()) {
                return new ArrayList<>();
            }
            if (node.getMatchProbability(sender, label, args, true) >= 50) {

                int extraLength = node.getNames().get(0).split(" ").length - 1;

                // Count positional (non-flag) args typed so far
                String currentArg = args[args.length - 1];
                List<String> positionalSoFar = new ArrayList<>();
                for (int i = extraLength; i < args.length - 1; i++) {
                    String a = args[i];
                    if (node.getFlagNodes().stream().anyMatch(fn -> fn.matches(a))) continue;
                    positionalSoFar.add(a);
                }
                int positionalArgIndex = positionalSoFar.size();

                List<String> completions = new ArrayList<>();

                // Positional param completions
                if (positionalArgIndex < node.getParameters().size()) {
                    ArgumentNode argumentNode = node.getParameters().get(positionalArgIndex);
                    completions.addAll(new ParamProcessor(argumentNode, currentArg, sender).getTabComplete());
                }

                // Flag completions (only suggest flags not yet present in args)
                Set<String> usedFlags = new HashSet<>();
                for (int i = extraLength; i < args.length - 1; i++) {
                    String a = args[i];
                    node.getFlagNodes().stream().filter(fn -> fn.matches(a)).findFirst()
                            .ifPresent(fn -> usedFlags.add(fn.getValue()));
                }
                for (FlagNode fn : node.getFlagNodes()) {
                    if (usedFlags.contains(fn.getValue())) continue;
                    for (String token : fn.getTokens()) {
                        if (token.toLowerCase().startsWith(currentArg.toLowerCase())) {
                            completions.add(token);
                        }
                    }
                }

                return completions;
            }

            return sortedNodes.stream()
                    .filter(sortedNode -> sortedNode.getPermission().isEmpty() || sender.hasPermission(sortedNode.getPermission()))
                    .map(sortedNode -> sortedNode.getNames().stream()
                            .map(name -> name.split(" "))
                            .filter(splitName -> splitName[0].equalsIgnoreCase(label))
                            .filter(splitName -> splitName.length > args.length)
                            .map(splitName -> splitName[args.length])
                            .collect(Collectors.toList()))
                    .flatMap(List::stream)
                    .filter(name -> name.toLowerCase().startsWith(args[args.length - 1].toLowerCase()))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("An exception occurred while executing tab completion for command '{}' (Sender: {})", label, sender.getName(), e);
            sender.sendMessage(ChatColor.RED + "An internal error occurred while executing this command.");
        }
        return new ArrayList<>();
    }
}
