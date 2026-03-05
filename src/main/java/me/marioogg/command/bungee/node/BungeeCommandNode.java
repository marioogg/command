package me.marioogg.command.bungee.node;

import lombok.Getter;
import lombok.SneakyThrows;
import me.marioogg.command.Command;
import me.marioogg.command.bungee.BungeeCommandHandler;
import me.marioogg.command.bungee.bukkit.BungeeCommand;
import me.marioogg.command.bungee.parameter.BungeeParamProcessor;
import me.marioogg.command.common.flag.Flag;
import me.marioogg.command.common.flag.FlagNode;
import me.marioogg.command.common.help.HelpNode;
import me.marioogg.command.bukkit.node.ArgumentNode;
import me.marioogg.command.bukkit.parameter.Param;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.slf4j.Logger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Getter
public class BungeeCommandNode {
    @Getter private static final List<BungeeCommandNode> nodes = new ArrayList<>();

    private static final Logger log = BungeeCommandHandler.getLogger();

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
    private final List<FlagNode> flagNodes = new ArrayList<>();
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

        Arrays.stream(method.getParameters()).forEach(parameter -> {
            Flag flag = parameter.getAnnotation(Flag.class);
            if (flag == null) return;
            flagNodes.add(new FlagNode(flag, parameter));
        });

        names.forEach(name -> {
            if (!BungeeCommand.getCommands().containsKey(name.split(" ")[0].toLowerCase()))
                new BungeeCommand(name.split(" ")[0].toLowerCase());
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
                int flagCount = 0;
                for(String arg : args) {
                    final String a = arg;
                    if(flagNodes.stream().anyMatch(fn -> fn.matches(a))) flagCount++;
                }
                int actualLength = args.length - (nameLength - 1) - flagCount;

                if (requiredParameters == actualLength || parameters.size() == actualLength) {
                    probability.addAndGet(125);
                    return;
                }

                if (!this.parameters.isEmpty()) {
                    ArgumentNode lastArgument = this.parameters.get(this.parameters.size() - 1);
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

        StringBuilder builder = new StringBuilder(ChatColor.RED + "Usage: /" + names.get(0) + " ");
        parameters.forEach(param -> {
            if (param.isRequired()) builder.append("<").append(param.getName()).append(param.isConcated() ? ".." : "").append(">");
            else builder.append("[").append(param.getName()).append(param.isConcated() ? ".." : "").append("]");
            builder.append(" ");
        });
        flagNodes.forEach(flag -> {
            builder.append("[").append(flag.getValue());
            if (!flag.getDescription().isEmpty()) builder.append(" (").append(flag.getDescription()).append(")");
            builder.append("] ");
        });

        sender.sendMessage(new TextComponent(builder.toString()));
    }

    public int requiredArgumentsLength() {
        int requiredArgumentsLength = names.get(0).split(" ").length - 1;
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

        int nameArgs = (names.get(0).split(" ").length - 1);

        // Separate flag tokens from positional args
        Set<String> activatedFlags = new HashSet<>();
        List<String> positionalArgs = new ArrayList<>();
        for (int i = nameArgs; i < args.length; i++) {
            String arg = args[i];
            FlagNode matched = null;
            for (FlagNode fn : flagNodes) {
                if (fn.matches(arg)) { matched = fn; break; }
            }
            if (matched != null) activatedFlags.add(matched.getValue());
            else positionalArgs.add(arg);
        }

        if (positionalArgs.size() < requiredArgumentsLength() - nameArgs) {
            sendUsageMessage(sender);
            return;
        }

        // Build positional objects
        List<Object> positionalObjects = new ArrayList<>();
        for (int i = 0; i < positionalArgs.size(); i++) {
            if (parameters.size() < i + 1) break;
            ArgumentNode node = parameters.get(i);

            if (node.isConcated()) {
                StringBuilder stringBuilder = new StringBuilder();
                for (int x = i; x < positionalArgs.size(); x++) {
                    stringBuilder.append(positionalArgs.get(x)).append(" ");
                }
                positionalObjects.add(stringBuilder.substring(0, stringBuilder.toString().length() - 1));
                break;
            }

            Object object = new BungeeParamProcessor(node, positionalArgs.get(i), sender).get();
            if (object == null) return;
            positionalObjects.add(object);
        }

        // Fill in missing optional positional args
        for (int i = positionalObjects.size(); i < parameters.size(); i++) {
            ArgumentNode argumentNode = parameters.get(i);
            if (argumentNode.getDefaultValue() == null) {
                positionalObjects.add(null);
            } else {
                positionalObjects.add(new BungeeParamProcessor(argumentNode, argumentNode.getDefaultValue(), sender).get());
            }
        }

        // Build final invocation list in method parameter declaration order
        List<Object> objects = new ArrayList<>();
        int positionalIndex = 0;
        for (java.lang.reflect.Parameter mp : method.getParameters()) {
            Flag flagAnn = mp.getAnnotation(Flag.class);
            Param paramAnn = mp.getAnnotation(Param.class);

            if (flagAnn != null) {
                FlagNode fn = flagNodes.stream()
                        .filter(f -> f.getValue().equals(flagAnn.value()))
                        .findFirst().orElse(null);
                objects.add(fn != null && activatedFlags.contains(fn.getValue()));
            } else if (paramAnn != null) {
                objects.add(positionalIndex < positionalObjects.size() ? positionalObjects.get(positionalIndex++) : null);
            } else {
                objects.add(sender);
            }
        }

        if (async) {
            final List<Object> asyncObjects = objects;
            BungeeCommandHandler.getPlugin().getProxy().getScheduler().runAsync(BungeeCommandHandler.getPlugin(), () -> {
                try {
                    method.invoke(parentClass, asyncObjects.toArray());
                } catch (IllegalAccessException | InvocationTargetException e) {
                    Throwable cause = (e instanceof InvocationTargetException) ? e.getCause() : e;
                    log.error("An exception occurred while executing command '{}' (Sender: {})", names.get(0), sender.getName(), cause);
                    sender.sendMessage(new ComponentBuilder(
                            "An internal error occurred while executing this command.")
                            .color(ChatColor.RED)
                            .bold(true)
                            .create());
            }});
            return;
        }

        method.invoke(parentClass, objects.toArray());
    }
}
