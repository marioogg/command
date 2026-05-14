package me.marioogg.command.brigadier;

import com.google.common.annotations.Beta;
import com.mojang.brigadier.CommandDispatcher;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import me.marioogg.command.brigadier.node.BrigadierCommandNode;
import me.marioogg.command.brigadier.parameter.BrigadierParamProcessor;
import me.marioogg.command.brigadier.parameter.BrigadierProcessor;
import me.marioogg.command.common.Command;
import me.marioogg.command.common.Subcommand;
import me.marioogg.command.common.help.Help;
import me.marioogg.command.common.help.HelpNode;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Registers annotation-based commands into a Mojang Brigadier dispatcher.
 */
@Getter
@Beta
public class BrigadierCommandHandler<S> {

    @FunctionalInterface
    public interface AsyncExecutor {
        void execute(Runnable runnable);
    }

    public interface SourceAdapter<S> {
        default boolean hasPermission(S source, String permission) {
            return true;
        }

        default boolean isPlayer(S source) {
            return true;
        }

        default UUID getUniqueId(S source) {
            return null;
        }

        default String getName(S source) {
            return String.valueOf(source);
        }

        default void sendMessage(S source, String message) {
            // No-op by default: consumers can inject platform-specific messaging.
        }
    }

    private final CommandDispatcher<S> dispatcher;
    private final SourceAdapter<S> sourceAdapter;

    @Setter
    private AsyncExecutor asyncExecutor = Runnable::run;

    @Setter
    private Logger logger = Logger.getLogger(BrigadierCommandHandler.class.getName());

    @Setter private String noPermissionMessage = "I'm sorry, but you do not have permission to perform this command.";
    @Setter private String playerOnlyMessage = "You must be a player to execute this command.";
    @Setter private String consoleOnlyMessage = "This command can only be executed by console.";
    @Setter private String internalErrorMessage = "An internal error occurred while executing this command.";
    @Setter private String cooldownMessage = "You must wait {seconds} more second(s) before using this command again.";
    @Setter private String minValidationMessage = "The value must be at least {min}.";
    @Setter private String maxValidationMessage = "The value must be at most {max}.";
    @Setter private String matchesValidationMessage = "Invalid format.";
    @Setter private String unknownCommandMessage = "Unknown command.";

    private final List<BrigadierCommandNode<S>> nodes = new ArrayList<>();
    private final Set<String> registeredRoots = new LinkedHashSet<>();

    public BrigadierCommandHandler(CommandDispatcher<S> dispatcher) {
        this(dispatcher, new SourceAdapter<>() {});
    }

    public BrigadierCommandHandler(CommandDispatcher<S> dispatcher, SourceAdapter<S> sourceAdapter) {
        this.dispatcher = dispatcher;
        this.sourceAdapter = sourceAdapter;
    }

    @SafeVarargs
    public final void registerCommands(Object... commandInstances) {
        Arrays.stream(commandInstances).forEach(this::registerCommandInstance);
    }

    @SneakyThrows
    public void registerCommands(Class<?>... commandClasses) {
        for (Class<?> commandClass : commandClasses) {
            if (!isInstantiable(commandClass)) continue;
            registerCommandInstance(commandClass.getDeclaredConstructor().newInstance());
        }
    }

    public void registerProcessor(BrigadierProcessor<?, ?> processor) {
        BrigadierParamProcessor.createProcessor(processor);
    }

    public void registerProcessors(BrigadierProcessor<?, ?>... processors) {
        Arrays.stream(processors).forEach(this::registerProcessor);
    }

    public List<BrigadierCommandNode<S>> getNodesForRoot(String root) {
        return nodes.stream()
                .filter(node -> node.getNames().stream().anyMatch(name -> name.startsWith(root.toLowerCase() + " ") || name.equalsIgnoreCase(root)))
                .toList();
    }

    private void registerCommandInstance(Object commandInstance) {
        Subcommand subcommand = commandInstance.getClass().getAnnotation(Subcommand.class);

        Arrays.stream(commandInstance.getClass().getDeclaredMethods()).forEach(method -> {
            Command command = method.getAnnotation(Command.class);
            if (command == null) return;

            if (subcommand != null) {
                String[] rootNames = subcommand.names();
                String[] methodNames = command.names();
                String[] fullNames = new String[rootNames.length * methodNames.length];
                int i = 0;
                for (String root : rootNames)
                    for (String sub : methodNames)
                        fullNames[i++] = root.isEmpty() ? sub.toLowerCase() : root.toLowerCase() + " " + sub.toLowerCase();
                command = buildDerivedCommand(command, fullNames);
            }

            BrigadierCommandNode<S> node = new BrigadierCommandNode<>(this, commandInstance, method, command);
            nodes.add(node);
            registerRoots(node);
        });

        Arrays.stream(commandInstance.getClass().getDeclaredMethods()).forEach(method -> {
            Help help = method.getAnnotation(Help.class);
            if (help == null) return;

            HelpNode helpNode = new HelpNode(commandInstance, help.names(), help.permission(), method);
            nodes.forEach(node -> node.getNames().forEach(name -> Arrays.stream(help.names())
                    .map(String::toLowerCase)
                    .filter(helpName -> name.toLowerCase().startsWith(helpName))
                    .forEach(helpName -> node.getHelpNodes().add(helpNode))));
        });
    }

    private void registerRoots(BrigadierCommandNode<S> node) {
        node.getNames().forEach(name -> {
            String root = name.split(" ")[0].toLowerCase();
            if (registeredRoots.contains(root)) return;
            dispatcher.register(new BrigadierCommand<>(this, root).build());
            registeredRoots.add(root);
        });
    }

    private Command buildDerivedCommand(Command original, String[] newNames) {
        return new Command() {
            public Class<? extends java.lang.annotation.Annotation> annotationType() { return Command.class; }
            public String[] names() { return newNames; }
            public String permission() { return original.permission(); }
            public boolean async() { return original.async(); }
            public String description() { return original.description(); }
            public boolean consoleOnly() { return original.consoleOnly(); }
            public boolean playerOnly() { return original.playerOnly(); }
            public boolean allowComplete() { return original.allowComplete(); }
            public boolean hidden() { return original.hidden(); }
        };
    }

    private boolean isInstantiable(Class<?> clazz) {
        return !clazz.isAnonymousClass()
                && !clazz.isLocalClass()
                && !clazz.isInterface()
                && !clazz.isEnum()
                && !Modifier.isAbstract(clazz.getModifiers())
                && (!clazz.isMemberClass() || Modifier.isStatic(clazz.getModifiers()))
                && Arrays.stream(clazz.getDeclaredConstructors()).anyMatch(c -> c.getParameterCount() == 0);
    }

    public void logExecutionException(String commandName, S source, Throwable throwable) {
        logger.log(Level.SEVERE,
                "An exception occurred while executing command '" + commandName + "' (Source: " + sourceAdapter.getName(source) + ")",
                throwable);
    }
}

