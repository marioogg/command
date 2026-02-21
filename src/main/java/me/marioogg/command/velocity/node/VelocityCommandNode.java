package me.marioogg.command.velocity.node;

import lombok.Getter;
import lombok.SneakyThrows;
import me.marioogg.command.Command;
import me.marioogg.command.help.HelpNode;
import me.marioogg.command.node.ArgumentNode;
import me.marioogg.command.parameter.Param;
import me.marioogg.command.velocity.VelocityCommandHandler;
import me.marioogg.command.velocity.command.VelocityRawCommand;
import me.marioogg.command.velocity.parameter.VelocityParamProcessor;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Getter
public class VelocityCommandNode {
    @Getter private static final List<VelocityCommandNode> nodes = new ArrayList<>();

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

    public VelocityCommandNode(Object parentClass, Method method, Command command) {
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
            if (!VelocityRawCommand.getCommands().containsKey(name.split(" ")[0].toLowerCase()))
                new VelocityRawCommand(name.split(" ")[0].toLowerCase());
        });

        List<String> toAdd = new ArrayList<>();
        names.forEach(name -> toAdd.add(VelocityCommandHandler.getPlugin().getClass().getSimpleName().toLowerCase() + ":" + name.toLowerCase()));
        names.addAll(toAdd);

        nodes.add(this);
    }

    public int getMatchProbability(CommandSource source, String label, String[] args, boolean tabbed) {
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

                if (this.parameters.size() > 0) {
                    ArgumentNode lastArgument = this.parameters.get(this.parameters.size() - 1);
                    if (lastArgument.isConcated() && actualLength > requiredParameters) {
                        probability.addAndGet(125);
                        return;
                    }
                }

                if (!tabbed || splitName.length > 1 || parameters.size() > 0)
                    probability.addAndGet(50);

                if (actualLength > requiredParameters)
                    probability.addAndGet(15);

                if (source instanceof Player && consoleOnly)
                    probability.addAndGet(-15);

                if (!(source instanceof Player) && playerOnly)
                    probability.addAndGet(-15);

                if (!permission.equals("") && !source.hasPermission(permission))
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

    public void sendUsageMessage(CommandSource source) {
        if (consoleOnly && source instanceof Player) {
            source.sendMessage(Component.text("This command can only be executed by console.", NamedTextColor.RED));
            return;
        }

        if (playerOnly && !(source instanceof Player)) {
            source.sendMessage(Component.text("You must be a player to execute this command.", NamedTextColor.RED));
            return;
        }

        if (!permission.equals("") && !source.hasPermission(permission)) {
            source.sendMessage(Component.text("I'm sorry, you do not have permission to execute this command.", NamedTextColor.RED));
            return;
        }

        StringBuilder builder = new StringBuilder("Usage: /" + names.get(0) + " ");
        parameters.forEach(param -> {
            if (param.isRequired()) builder.append("<").append(param.getName()).append(param.isConcated() ? ".." : "").append(">");
            else builder.append("[").append(param.getName()).append(param.isConcated() ? ".." : "").append("]");
            builder.append(" ");
        });

        source.sendMessage(Component.text(builder.toString(), NamedTextColor.RED));
    }

    public int requiredArgumentsLength() {
        int requiredArgumentsLength = names.get(0).split(" ").length - 1;
        for (ArgumentNode node : parameters) if (node.isRequired()) requiredArgumentsLength++;
        return requiredArgumentsLength;
    }

    @SneakyThrows
    public void execute(CommandSource source, String[] args) {
        if (!permission.equals("") && !source.hasPermission(permission)) {
            source.sendMessage(Component.text("I'm sorry, although you do not have permission to execute this command.", NamedTextColor.RED));
            return;
        }

        if (!(source instanceof Player) && playerOnly) {
            source.sendMessage(Component.text("You must be a player to execute this command.", NamedTextColor.RED));
            return;
        }

        if (source instanceof Player && consoleOnly) {
            source.sendMessage(Component.text("This command is only executable by console.", NamedTextColor.RED));
            return;
        }

        int nameArgs = (names.get(0).split(" ").length - 1);

        List<Object> objects = new ArrayList<>(Collections.singletonList(source));
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
            Object object = new VelocityParamProcessor(node, suppliedArgument, source).get();

            if (object == null) return;
            objects.add(object);
        }

        if (args.length < requiredArgumentsLength()) {
            sendUsageMessage(source);
            return;
        }

        int difference = (parameters.size() - requiredArgumentsLength()) - ((args.length - nameArgs) - requiredArgumentsLength());
        for (int i = 0; i < difference; i++) {
            ArgumentNode argumentNode = parameters.get(requiredArgumentsLength() + i);

            if (argumentNode.getDefaultValue() == null) {
                objects.add(null);
                continue;
            }

            objects.add(new VelocityParamProcessor(argumentNode, argumentNode.getDefaultValue(), source).get());
        }

        if (async) {
            final List<Object> asyncObjects = objects;
            VelocityCommandHandler.getProxy().getScheduler()
                    .buildTask(VelocityCommandHandler.getPlugin(), () -> {
                        try { method.invoke(parentClass, asyncObjects.toArray()); } catch (Exception e) { e.printStackTrace(); }
                    }).schedule();
            return;
        }

        method.invoke(parentClass, objects.toArray());
    }
}

