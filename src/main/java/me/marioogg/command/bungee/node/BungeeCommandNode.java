package me.marioogg.command.bungee.node;

import lombok.Getter;
import lombok.SneakyThrows;
import me.marioogg.command.Command;
import me.marioogg.command.bungee.BungeeCommandHandler;
import me.marioogg.command.bungee.bukkit.BungeeRawCommand;
import me.marioogg.command.bungee.parameter.BungeeParamProcessor;
import me.marioogg.command.help.HelpNode;
import me.marioogg.command.node.ArgumentNode;
import me.marioogg.command.parameter.Param;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Getter
public class BungeeCommandNode {
    @Getter private static final List<BungeeCommandNode> nodes = new ArrayList<>();

    private final ArrayList<String> names = new ArrayList<>();
    private final String permission;
    private final String description;
    private final boolean async;
    private final boolean allowComplete;
    private final boolean playerOnly;
    private final boolean consoleOnly;

    private final Object parentClass;
    private final Method method;

    private final List<ArgumentNode> parameters = new ArrayList<>();
    private final List<HelpNode> helpNodes = new ArrayList<>();

    public BungeeCommandNode(Object parentClass, Method method, Command command) {
        Arrays.stream(command.names()).forEach(name -> names.add(name.toLowerCase()));

        this.permission = command.permission();
        this.description = command.description();
        this.async = command.async();
        this.playerOnly = command.playerOnly();
        this.consoleOnly = command.consoleOnly();
        this.allowComplete = command.allowComplete();

        this.parentClass = parentClass;
        this.method = method;

        Arrays.stream(method.getParameters()).forEach(parameter -> {
            Param param = parameter.getAnnotation(Param.class);
            if (param == null) return;
            parameters.add(new ArgumentNode(param.name(), param.concated(), param.required(), param.defaultValue().isEmpty() ? null : param.defaultValue(), parameter));
        });

        names.forEach(name -> {
            if (!BungeeRawCommand.getCommands().containsKey(name.split(" ")[0].toLowerCase()))
                new BungeeRawCommand(name.split(" ")[0].toLowerCase());
        });

        List<String> toAdd = new ArrayList<>();
        names.forEach(name -> toAdd.add(BungeeCommandHandler.getPlugin().getDescription().getName() + ":" + name.toLowerCase()));
        names.addAll(toAdd);

        nodes.add(this);
    }

    public int getMatchProbability(CommandSender sender, String label, String[] args, boolean tabbed) {
        AtomicInteger probability = new AtomicInteger(0);

        this.names.forEach(name -> {
            StringBuilder nameLabel = new StringBuilder(label).append(" ");
            String[] splitName = name.split(" ");
            int nameLength = splitName.length;

            for (int i = 1; i < nameLength; i++)
                if (args.length >= i) nameLabel.append(args[i - 1]).append(" ");

            if (name.equalsIgnoreCase(nameLabel.toString().trim())) {
                int requiredParameters = (int) this.parameters.stream().filter(ArgumentNode::isRequired).count();
                int actualLength = args.length - (nameLength - 1);

                if (requiredParameters == actualLength || parameters.size() == actualLength) {
                    probability.addAndGet(125);
                    return;
                }

                if (!this.parameters.isEmpty()) {
                    ArgumentNode lastArgument = this.parameters.getLast();
                    if (lastArgument.isConcated() && actualLength > requiredParameters) {
                        probability.addAndGet(125);
                        return;
                    }
                }

                if (!tabbed || splitName.length > 1 || !parameters.isEmpty())
                    probability.addAndGet(50);

                if (actualLength > requiredParameters)
                    probability.addAndGet(15);

                if (sender instanceof ProxiedPlayer && consoleOnly)
                    probability.addAndGet(-15);

                if (!(sender instanceof ProxiedPlayer) && playerOnly)
                    probability.addAndGet(-15);

                if (!permission.isEmpty() && !sender.hasPermission(permission))
                    probability.addAndGet(-15);

                return;
            }

            String[] labelSplit = nameLabel.toString().split(" ");
            for (int i = 0; i < nameLength && i < labelSplit.length; i++)
                if (splitName[i].equalsIgnoreCase(labelSplit[i]))
                    probability.addAndGet(5);
        });

        return probability.get();
    }

    public void sendUsageMessage(CommandSender sender) {
        if (consoleOnly && sender instanceof ProxiedPlayer) {
            sender.sendMessage(new TextComponent(ChatColor.RED + "This command can only be executed by console."));
            return;
        }

        if (playerOnly && !(sender instanceof ProxiedPlayer)) {
            sender.sendMessage(new TextComponent(ChatColor.RED + "You must be a player to execute this command."));
            return;
        }

        if (!permission.isEmpty() && !sender.hasPermission(permission)) {
            sender.sendMessage(new TextComponent(ChatColor.RED + "I'm sorry, you do not have permission to execute this command."));
            return;
        }

        StringBuilder builder = new StringBuilder(ChatColor.RED + "Usage: /" + names.getFirst() + " ");
        parameters.forEach(param -> {
            if (param.isRequired()) builder.append("<").append(param.getName()).append(param.isConcated() ? ".." : "").append(">");
            else builder.append("[").append(param.getName()).append(param.isConcated() ? ".." : "").append("]");
            builder.append(" ");
        });

        sender.sendMessage(new TextComponent(builder.toString()));
    }

    public int requiredArgumentsLength() {
        int requiredArgumentsLength = names.getFirst().split(" ").length - 1;
        for (ArgumentNode node : parameters) if (node.isRequired()) requiredArgumentsLength++;
        return requiredArgumentsLength;
    }

    @SneakyThrows
    public void execute(CommandSender sender, String[] args) {
        if (!permission.isEmpty() && !sender.hasPermission(permission)) {
            sender.sendMessage(new TextComponent(ChatColor.RED + "I'm sorry, although you do not have permission to execute this command."));
            return;
        }

        if (!(sender instanceof ProxiedPlayer) && playerOnly) {
            sender.sendMessage(new TextComponent(ChatColor.RED + "You must be a player to execute this command."));
            return;
        }

        if (sender instanceof ProxiedPlayer && consoleOnly) {
            sender.sendMessage(new TextComponent(ChatColor.RED + "This command is only executable by console."));
            return;
        }

        int nameArgs = (names.getFirst().split(" ").length - 1);

        List<Object> objects = new ArrayList<>(Collections.singletonList(sender));
        for (int i = 0; i < args.length - nameArgs; i++) {
            if (parameters.size() < i + 1) break;
            ArgumentNode node = parameters.get(i);

            if (node.isConcated()) {
                StringBuilder stringBuilder = new StringBuilder();
                for (int x = i; x < args.length; x++) {
                    if (args.length - 1 < x + nameArgs) continue;
                    stringBuilder.append(args[x + nameArgs]).append(" ");
                }
                objects.add(stringBuilder.substring(0, stringBuilder.toString().length() - 1));
                break;
            }

            String suppliedArgument = args[i + nameArgs];
            Object object = new BungeeParamProcessor(node, suppliedArgument, sender).get();

            if (object == null) return;
            objects.add(object);
        }

        if (args.length < requiredArgumentsLength()) {
            sendUsageMessage(sender);
            return;
        }

        int difference = (parameters.size() - requiredArgumentsLength()) - ((args.length - nameArgs) - requiredArgumentsLength());
        for (int i = 0; i < difference; i++) {
            ArgumentNode argumentNode = parameters.get(requiredArgumentsLength() + i);

            if (argumentNode.getDefaultValue() == null) {
                objects.add(null);
                continue;
            }

            objects.add(new BungeeParamProcessor(argumentNode, argumentNode.getDefaultValue(), sender).get());
        }

        if (async) {
            final List<Object> asyncObjects = objects;
            BungeeCommandHandler.getPlugin().getProxy().getScheduler().runAsync(BungeeCommandHandler.getPlugin(), () -> {
                try { method.invoke(parentClass, asyncObjects.toArray()); } catch (Exception e) { e.printStackTrace(); }
            });
            return;
        }

        method.invoke(parentClass, objects.toArray());
    }
}

