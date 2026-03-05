package me.marioogg.command.velocity.node;

import lombok.Getter;
import lombok.SneakyThrows;
import me.marioogg.command.Command;
import me.marioogg.command.common.flag.Flag;
import me.marioogg.command.common.flag.FlagNode;
import me.marioogg.command.common.help.HelpNode;
import me.marioogg.command.bukkit.node.ArgumentNode;
import me.marioogg.command.bukkit.parameter.Param;
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
public class VelocityCommandNode{
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
    private final List<FlagNode> flagNodes = new ArrayList<>();
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

        Arrays.stream(method.getParameters()).forEach(parameter -> {
            Flag flag = parameter.getAnnotation(Flag.class);
            if (flag == null) return;
            flagNodes.add(new FlagNode(flag, parameter));
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

                if (source instanceof Player && consoleOnly)
                    probability.addAndGet(-15);

                if (!(source instanceof Player) && playerOnly)
                    probability.addAndGet(-15);

                if (!permission.isEmpty() && !source.hasPermission(permission))
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

        if (!permission.isEmpty() && !source.hasPermission(permission)) {
            source.sendMessage(Component.text("I'm sorry, you do not have permission to execute this command.", NamedTextColor.RED));
            return;
        }

        StringBuilder builder = new StringBuilder("Usage: /" + names.get(0) + " ");
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

        source.sendMessage(Component.text(builder.toString(), NamedTextColor.RED));
    }

    public int requiredArgumentsLength() {
        int requiredArgumentsLength = names.get(0).split(" ").length - 1;
        for (ArgumentNode node : parameters) if (node.isRequired()) requiredArgumentsLength++;
        return requiredArgumentsLength;
    }

    @SneakyThrows
    public void execute(CommandSource source, String[] args) {
        if (!permission.isEmpty() && !source.hasPermission(permission)) {
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
            sendUsageMessage(source);
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

            Object object = new VelocityParamProcessor(node, positionalArgs.get(i), source).get();
            if (object == null) return;
            positionalObjects.add(object);
        }

        // Fill in missing optional positional args
        for (int i = positionalObjects.size(); i < parameters.size(); i++) {
            ArgumentNode argumentNode = parameters.get(i);
            if (argumentNode.getDefaultValue() == null) {
                positionalObjects.add(null);
            } else {
                positionalObjects.add(new VelocityParamProcessor(argumentNode, argumentNode.getDefaultValue(), source).get());
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
                objects.add(source);
            }
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

