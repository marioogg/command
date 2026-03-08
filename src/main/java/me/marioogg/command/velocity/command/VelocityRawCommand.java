package me.marioogg.command.velocity.command;

import lombok.Getter;
import lombok.SneakyThrows;
import me.marioogg.command.common.flag.FlagNode;
import me.marioogg.command.common.help.HelpNode;
import me.marioogg.command.bukkit.node.ArgumentNode;
import me.marioogg.command.velocity.VelocityCommandHandler;
import me.marioogg.command.velocity.node.VelocityCommandNode;
import me.marioogg.command.velocity.parameter.VelocityParamProcessor;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.RawCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.*;
import java.util.stream.Collectors;

public class VelocityRawCommand implements RawCommand {
    @Getter private static final HashMap<String, VelocityRawCommand> commands = new HashMap<>();

    private final String root;

    public VelocityRawCommand(String root) {
        this.root = root;
        commands.put(root.toLowerCase(), this);
        VelocityCommandHandler.getProxy().getCommandManager().register(
                VelocityCommandHandler.getProxy().getCommandManager().metaBuilder(root)
                        .plugin(VelocityCommandHandler.getPlugin())
                        .build(),
                this);
    }

    @SneakyThrows
    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments().isEmpty() ? new String[0] : invocation.arguments().split(" ", -1);

        List<VelocityCommandNode> sortedNodes = VelocityCommandNode.getNodes().stream()
                .sorted(Comparator.comparingInt(node -> node.getMatchProbability(source, root, args, false)))
                .collect(Collectors.toList());

        VelocityCommandNode node = sortedNodes.get(sortedNodes.size() - 1);

        if (node.getMatchProbability(source, root, args, false) < 90) {
            if (node.getHelpNodes().isEmpty()) {
                node.sendUsageMessage(source);
                return;
            }

            HelpNode helpNode = node.getHelpNodes().get(0);

            if (!helpNode.getPermission().isEmpty() && !source.hasPermission(helpNode.getPermission())) {
                source.sendMessage(Component.text("I'm sorry, although you do not have permission to execute this command.", NamedTextColor.RED));
                return;
            }

            helpNode.getMethod().invoke(helpNode.getParentClass(), source);
            return;
        }

        node.execute(source, args);
    }

    @Override
    public List<String> suggest(Invocation invocation) {
        try {
            CommandSource source = invocation.source();
            String[] args = invocation.arguments().isEmpty() ? new String[0] : invocation.arguments().split(" ", -1);

            List<VelocityCommandNode> sortedNodes = VelocityCommandNode.getNodes().stream()
                    .sorted(Comparator.comparingInt(node -> node.getMatchProbability(source, root, args, true)))
                    .collect(Collectors.toList());

            VelocityCommandNode node = sortedNodes.get(sortedNodes.size() - 1);

            if (!node.isAllowComplete()) return new ArrayList<>();

            if (node.getMatchProbability(source, root, args, true) >= 50) {
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
                    completions.addAll(new VelocityParamProcessor(argumentNode, currentArg, source).getTabComplete());
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
                    .filter(sortedNode -> sortedNode.getPermission().isEmpty() || source.hasPermission(sortedNode.getPermission()))
                    .map(sortedNode -> sortedNode.getNames().stream()
                            .map(name -> name.split(" "))
                            .filter(splitName -> splitName[0].equalsIgnoreCase(root))
                            .filter(splitName -> splitName.length > args.length)
                            .map(splitName -> splitName[args.length])
                            .collect(Collectors.toList()))
                    .flatMap(List::stream)
                    .filter(name -> name.toLowerCase().startsWith(args.length > 0 ? args[args.length - 1].toLowerCase() : ""))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }
}

