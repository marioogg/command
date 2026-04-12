package me.marioogg.command.brigadier;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import me.marioogg.command.brigadier.node.BrigadierCommandNode;
import me.marioogg.command.common.help.HelpNode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Root Brigadier node that routes execution and suggestions to registered command nodes.
 */
public class BrigadierCommand<S> {

    private final BrigadierCommandHandler<S> handler;
    private final String root;

    public BrigadierCommand(BrigadierCommandHandler<S> handler, String root) {
        this.handler = handler;
        this.root = root.toLowerCase();
    }

    public LiteralArgumentBuilder<S> build() {
        return LiteralArgumentBuilder.<S>literal(root)
                .executes(context -> {
                    try {
                        return execute(context, new String[0]);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .then(RequiredArgumentBuilder.<S, String>argument("args", StringArgumentType.greedyString())
                        .suggests(this::suggest)
                        .executes(context -> {
                            String raw = StringArgumentType.getString(context, "args");
                            String[] args = raw.isEmpty() ? new String[0] : raw.split(" ");
                            try {
                                return execute(context, args);
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        }));
    }

    private int execute(CommandContext<S> context, String[] args) throws Exception {
        S source = context.getSource();

        List<BrigadierCommandNode<S>> sortedNodes = handler.getNodesForRoot(root).stream()
                .sorted(Comparator.comparingInt(node -> node.getMatchProbability(source, root, args, false)))
                .toList();

        if (sortedNodes.isEmpty()) return 0;

        BrigadierCommandNode<S> node = sortedNodes.get(sortedNodes.size() - 1);

        if (node.getMatchProbability(source, root, args, false) < 90) {
            if (node.getHelpNodes().isEmpty()) {
                node.sendUsageMessage(source);
                return 0;
            }

            HelpNode helpNode = node.getHelpNodes().get(0);
            if (!helpNode.getPermission().isEmpty() && !handler.getSourceAdapter().hasPermission(source, helpNode.getPermission())) {
                handler.getSourceAdapter().sendMessage(source, handler.getNoPermissionMessage());
                return 0;
            }

            helpNode.getMethod().invoke(helpNode.getParentClass(), source);
            return 1;
        }

        node.execute(source, args);
        return 1;
    }

    private CompletableFuture<Suggestions> suggest(CommandContext<S> context, SuggestionsBuilder builder) {
        S source = context.getSource();
        String remaining = builder.getRemaining();
        String[] args = remaining.isEmpty() ? new String[]{""} : remaining.split(" ", -1);

        List<BrigadierCommandNode<S>> sortedNodes = handler.getNodesForRoot(root).stream()
                .sorted(Comparator.comparingInt(node -> node.getMatchProbability(source, root, args, true)))
                .collect(Collectors.toList());

        if (sortedNodes.isEmpty()) return builder.buildFuture();

        BrigadierCommandNode<S> node = sortedNodes.get(sortedNodes.size() - 1);
        if (!node.isAllowComplete()) return builder.buildFuture();

        Set<String> suggestions = new LinkedHashSet<>();

        if (node.getMatchProbability(source, root, args, true) >= 50) {
            suggestions.addAll(node.getArgumentCompletions(source, args));
        } else {
            String currentArg = args.length > 0 ? args[args.length - 1] : "";
            suggestions.addAll(sortedNodes.stream()
                    .filter(sortedNode -> sortedNode.getPermission().isEmpty() || handler.getSourceAdapter().hasPermission(source, sortedNode.getPermission()))
                    .map(sortedNode -> sortedNode.getNames().stream()
                            .map(name -> name.split(" "))
                            .filter(splitName -> splitName[0].equalsIgnoreCase(root))
                            .filter(splitName -> splitName.length > args.length)
                            .map(splitName -> splitName[args.length])
                            .collect(Collectors.toList()))
                    .flatMap(List::stream)
                    .filter(name -> name.toLowerCase().startsWith(currentArg.toLowerCase()))
                    .collect(Collectors.toCollection(ArrayList::new)));
        }

        suggestions.forEach(builder::suggest);
        return builder.buildFuture();
    }
}
