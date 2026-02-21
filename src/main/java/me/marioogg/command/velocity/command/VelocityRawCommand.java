package me.marioogg.command.velocity.command;

import lombok.Getter;
import lombok.SneakyThrows;
import me.marioogg.command.help.HelpNode;
import me.marioogg.command.node.ArgumentNode;
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
                int arg = (args.length - extraLength) - 1;

                if (arg < 0 || node.getParameters().size() < arg + 1) return new ArrayList<>();

                ArgumentNode argumentNode = node.getParameters().get(arg);
                return new VelocityParamProcessor(argumentNode, args[args.length - 1], source).getTabComplete();
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

