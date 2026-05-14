package me.marioogg.command.brigadier.node;

import lombok.Getter;
import me.marioogg.command.brigadier.BrigadierCommandHandler;
import me.marioogg.command.brigadier.parameter.BrigadierParamProcessor;
import me.marioogg.command.common.Command;
import me.marioogg.command.common.cooldown.Cooldown;
import me.marioogg.command.common.cooldown.CooldownManager;
import me.marioogg.command.common.cooldown.CooldownNode;
import me.marioogg.command.common.flag.Flag;
import me.marioogg.command.common.flag.FlagNode;
import me.marioogg.command.common.help.HelpNode;
import me.marioogg.command.common.node.ArgumentNode;
import me.marioogg.command.common.parameter.Param;
import me.marioogg.command.common.parameter.ParamNodeParser;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Stores and executes Brigadier command metadata.
 */


// I'll most likely document this as this is quite a confusing class
@Getter
public class BrigadierCommandNode<S> {

    private final BrigadierCommandHandler<S> handler;

    private final ArrayList<String> names = new ArrayList<>();
    private final String permission;
    private final String description;
    private final boolean async;
    private final boolean allowComplete;
    private final boolean hidden;
    private final boolean playerOnly;
    private final boolean consoleOnly;

    private final Object parentClass;
    private final Method method;

    private final CooldownNode cooldownNode;

    private final List<ArgumentNode> parameters = new ArrayList<>();
    private final List<FlagNode> flagNodes = new ArrayList<>();
    private final List<HelpNode> helpNodes = new ArrayList<>();

    public BrigadierCommandNode(BrigadierCommandHandler<S> handler, Object parentClass, Method method, Command command) {
        this.handler = handler;
        Arrays.stream(command.names()).forEach(name -> names.add(name.toLowerCase()));

        this.permission = command.permission();
        this.description = command.description();
        this.async = command.async();
        this.playerOnly = command.playerOnly();
        this.consoleOnly = command.consoleOnly();
        this.allowComplete = command.allowComplete();
        this.hidden = command.hidden();

        this.parentClass = parentClass;
        this.method = method;

        // Parse parameters using centralized parser
        parameters.addAll(ParamNodeParser.parseParameters(method));

        Arrays.stream(method.getParameters()).forEach(parameter -> {
            Flag flag = parameter.getAnnotation(Flag.class);
            if (flag == null) return;
            flagNodes.add(new FlagNode(flag, parameter));
        });

        Cooldown cooldown = method.getAnnotation(Cooldown.class);
        this.cooldownNode = cooldown != null ? new CooldownNode(cooldown.seconds(), cooldown.bypassPermission()) : null;
    }

    public int getMatchProbability(S source, String label, String[] args, boolean tabbed) {
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
                for (String arg : args) {
                    final String a = arg;
                    if (flagNodes.stream().anyMatch(fn -> fn.matches(a))) flagCount++;
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

                if (handler.getSourceAdapter().isPlayer(source) && consoleOnly)
                    probability.addAndGet(-15);

                if (!handler.getSourceAdapter().isPlayer(source) && playerOnly)
                    probability.addAndGet(-15);

                if (!permission.isEmpty() && !handler.getSourceAdapter().hasPermission(source, permission))
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

    public void sendUsageMessage(S source) {
        if (consoleOnly && handler.getSourceAdapter().isPlayer(source)) {
            handler.getSourceAdapter().sendMessage(source, handler.getConsoleOnlyMessage());
            return;
        }

        if (playerOnly && !handler.getSourceAdapter().isPlayer(source)) {
            handler.getSourceAdapter().sendMessage(source, handler.getPlayerOnlyMessage());
            return;
        }

        if ((!permission.isEmpty() && !handler.getSourceAdapter().hasPermission(source, permission)) && hidden) {
            handler.getSourceAdapter().sendMessage(source, handler.getUnknownCommandMessage());
            return;
        }

        if (!permission.isEmpty() && !handler.getSourceAdapter().hasPermission(source, permission)) {
            handler.getSourceAdapter().sendMessage(source, handler.getNoPermissionMessage());
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

        handler.getSourceAdapter().sendMessage(source, builder.toString());
    }

    public int requiredArgumentsLength() {
        int requiredArgumentsLength = names.get(0).split(" ").length - 1;
        for (ArgumentNode node : parameters) if (node.isRequired()) requiredArgumentsLength++;
        return requiredArgumentsLength;
    }

    public List<String> getArgumentCompletions(S source, String[] args) {
        int extraLength = names.get(0).split(" ").length - 1;
        String currentArg = args.length > 0 ? args[args.length - 1] : "";

        List<String> positionalSoFar = new ArrayList<>();
        for (int i = extraLength; i < args.length - 1; i++) {
            String a = args[i];
            if (flagNodes.stream().anyMatch(fn -> fn.matches(a))) continue;
            positionalSoFar.add(a);
        }

        List<String> completions = new ArrayList<>();

        if (positionalSoFar.size() < parameters.size()) {
            ArgumentNode argumentNode = parameters.get(positionalSoFar.size());
            completions.addAll(new BrigadierParamProcessor<>(handler, argumentNode, currentArg, source).getTabComplete());
        }

        Set<String> usedFlags = new HashSet<>();
        for (int i = extraLength; i < args.length - 1; i++) {
            String a = args[i];
            flagNodes.stream().filter(fn -> fn.matches(a)).findFirst().ifPresent(fn -> usedFlags.add(fn.getValue()));
        }
        for (FlagNode fn : flagNodes) {
            if (usedFlags.contains(fn.getValue())) continue;
            for (String token : fn.getTokens()) {
                if (token.toLowerCase().startsWith(currentArg.toLowerCase())) {
                    completions.add(token);
                }
            }
        }

        return completions;
    }

    public void execute(S source, String[] args) {
        if (!permission.isEmpty() && !handler.getSourceAdapter().hasPermission(source, permission)) {
            handler.getSourceAdapter().sendMessage(source, handler.getNoPermissionMessage());
            return;
        }

        if (!handler.getSourceAdapter().isPlayer(source) && playerOnly) {
            handler.getSourceAdapter().sendMessage(source, handler.getPlayerOnlyMessage());
            return;
        }

        if (handler.getSourceAdapter().isPlayer(source) && consoleOnly) {
            handler.getSourceAdapter().sendMessage(source, handler.getConsoleOnlyMessage());
            return;
        }

        if (cooldownNode != null) {
            UUID uniqueId = handler.getSourceAdapter().getUniqueId(source);
            if (uniqueId != null && (cooldownNode.getBypassPermission().isEmpty()
                    || !handler.getSourceAdapter().hasPermission(source, cooldownNode.getBypassPermission()))) {
                if (CooldownManager.isOnCooldown(uniqueId, names.get(0))) {
                    long remaining = CooldownManager.getRemainingSeconds(uniqueId, names.get(0));
                    handler.getSourceAdapter().sendMessage(source,
                            handler.getCooldownMessage().replace("{seconds}", String.valueOf(remaining)));
                    return;
                }
                CooldownManager.setCooldown(uniqueId, names.get(0), cooldownNode.getSeconds());
            }
        }

        int nameArgs = (names.get(0).split(" ").length - 1);

        Set<String> activatedFlags = new HashSet<>();
        List<String> positionalArgs = new ArrayList<>();
        for (int i = nameArgs; i < args.length; i++) {
            String arg = args[i];
            FlagNode matched = null;
            for (FlagNode fn : flagNodes) {
                if (fn.matches(arg)) {
                    matched = fn;
                    break;
                }
            }
            if (matched != null) activatedFlags.add(matched.getValue());
            else positionalArgs.add(arg);
        }

        if (positionalArgs.size() < requiredArgumentsLength() - nameArgs) {
            sendUsageMessage(source);
            return;
        }

        List<Object> positionalObjects = new ArrayList<>();
        for (int i = 0; i < positionalArgs.size(); i++) {
            if (parameters.size() < i + 1) break;
            ArgumentNode node = parameters.get(i);

            if (node.isConcated()) {
                StringBuilder stringBuilder = new StringBuilder();
                for (int x = i; x < positionalArgs.size(); x++) {
                    stringBuilder.append(positionalArgs.get(x)).append(" ");
                }
                positionalObjects.add(stringBuilder.toString().trim());
                break;
            }

            Object object = new BrigadierParamProcessor<>(handler, node, positionalArgs.get(i), source).get();
            if (object == null) return;
            positionalObjects.add(object);
        }

        for (int i = positionalObjects.size(); i < parameters.size(); i++) {
            ArgumentNode argumentNode = parameters.get(i);
            if (argumentNode.getDefaultValue() == null) {
                positionalObjects.add(null);
            } else {
                positionalObjects.add(new BrigadierParamProcessor<>(handler, argumentNode, argumentNode.getDefaultValue(), source).get());
            }
        }

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
            handler.getAsyncExecutor().execute(() -> invokeMethod(source, objects));
            return;
        }

        invokeMethod(source, objects);
    }

    private void invokeMethod(S source, List<Object> params) {
        try {
            method.invoke(parentClass, params.toArray());
        } catch (IllegalAccessException | InvocationTargetException e) {
            Throwable cause = (e instanceof InvocationTargetException) ? e.getCause() : e;
            handler.logExecutionException(names.get(0), source, cause);
            handler.getSourceAdapter().sendMessage(source, handler.getInternalErrorMessage());
        }
    }
}

