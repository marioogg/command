package me.marioogg.command.bukkit;

import com.google.common.reflect.ClassPath;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import me.marioogg.command.Command;
import me.marioogg.command.Subcommand;
import me.marioogg.command.common.help.Help;
import me.marioogg.command.common.help.HelpNode;
import me.marioogg.command.bukkit.node.CommandNode;
import me.marioogg.command.bukkit.parameter.ParamProcessor;
import me.marioogg.command.bukkit.parameter.Processor;
import org.bukkit.ChatColor;
import org.bukkit.plugin.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 * Handles command and processor registration for Bukkit.
 */
public class BukkitCommandHandler {

    @Getter
    private static Plugin plugin;
    @Getter
    private static Logger logger;


    @Setter @Getter private static String noPermissionMessage = ChatColor.RED + "I'm sorry, but you do not have permission to perform this command.";
    @Setter @Getter private static String playerOnlyMessage = ChatColor.RED + "You must be a player to execute this command.";
    @Setter @Getter private static String consoleOnlyMessage = ChatColor.RED + "This command can only be executed by console.";
    @Setter @Getter private static String internalErrorMessage = ChatColor.RED + "An internal error occurred while executing this command.";


    public static void setPlugin(Plugin plugin) {
        BukkitCommandHandler.plugin = plugin;
        logger = LoggerFactory.getLogger(plugin.getName());
    }

    @SneakyThrows
    public static void registerCommands(String path, Plugin plugin) {
        ClassPath.from(plugin.getClass().getClassLoader()).getAllClasses().stream()
                .filter(info -> info.getPackageName().startsWith(path))
                .map(ClassPath.ClassInfo::load)
                .filter(clazz -> !clazz.isAnonymousClass()
                        && !clazz.isLocalClass()
                        && !clazz.isInterface()
                        && !java.lang.reflect.Modifier.isAbstract(clazz.getModifiers()))
                .forEach(clazz -> registerCommands(clazz, plugin));
    }

    @SneakyThrows
    public static void registerCommands(Class<?> commandClass, Plugin plugin) {
        if (commandClass.isAnonymousClass() || commandClass.isLocalClass()
                || commandClass.isInterface() || java.lang.reflect.Modifier.isAbstract(commandClass.getModifiers())) return;
        BukkitCommandHandler.setPlugin(plugin);
        if (!isInstantiable(commandClass)) return;
        registerCommands(commandClass.getDeclaredConstructor().newInstance());
    }

    @SneakyThrows
    public static void registerCommands(Plugin plugin, Class<?>... commandClasses) {
        BukkitCommandHandler.setPlugin(plugin);
        for (Class<?> commandClass : commandClasses) {
            if (commandClass.isAnonymousClass() || commandClass.isLocalClass()
                    || commandClass.isInterface() || java.lang.reflect.Modifier.isAbstract(commandClass.getModifiers())) continue;
            registerCommands(commandClass.getDeclaredConstructor().newInstance());
        }
    }

    public static void registerCommands(Object commandClass) {
        Subcommand subcommand = commandClass.getClass().getAnnotation(Subcommand.class);

        Arrays.stream(commandClass.getClass().getDeclaredMethods()).forEach(method -> {
            Command command = method.getAnnotation(Command.class);
            if(command == null) return;

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

            new CommandNode(commandClass, method, command);
        });

        Arrays.stream(commandClass.getClass().getDeclaredMethods()).forEach(method -> {
            Help help = method.getAnnotation(Help.class);
            if (help == null) return;
            HelpNode helpNode = new HelpNode(commandClass, help.names(), help.permission(), method);
            CommandNode.getNodes().forEach(node -> node.getNames().forEach(name -> Arrays.stream(help.names())
                    .map(String::toLowerCase)
                    .filter(helpName -> name.toLowerCase().startsWith(helpName))
                    .forEach(helpName -> node.getHelpNodes().add(helpNode))));
        });
    }

    @SneakyThrows
    public static void registerProcessors(String path, Plugin plugin) {
        ClassPath.from(plugin.getClass().getClassLoader()).getAllClasses().stream()
                .filter(info -> info.getPackageName().startsWith(path))
                .filter(info -> isInstantiable(info.load()))
                .filter(info -> info.load().getSuperclass().equals(Processor.class))
                .forEach(info -> {
                    try {
                        ParamProcessor.createProcessor((Processor<?>) info.load().getDeclaredConstructor().newInstance());
                    } catch (Exception e) {
                        logger.error("Error registering command processors: ", e);
                    }
                });
    }

    public static void registerProcessor(Processor<?> processor) {
        ParamProcessor.createProcessor(processor);
    }

    public static void registerProcessors(Processor<?>... processors) {
        Arrays.stream(processors).forEach(BukkitCommandHandler::registerProcessor);
    }

    /**
     * Creates a proxy of a {@link Command} annotation with a replaced names array.
     * Used internally by the {@link Subcommand} system to synthesize compound names.
     */
    private static Command buildDerivedCommand(Command original, String[] newNames) {
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

    private static boolean isInstantiable(Class<?> clazz) {
        return !clazz.isAnonymousClass()
                && !clazz.isLocalClass()
                && !clazz.isInterface()
                && !clazz.isEnum()
                && !java.lang.reflect.Modifier.isAbstract(clazz.getModifiers())
                && (!clazz.isMemberClass() || java.lang.reflect.Modifier.isStatic(clazz.getModifiers()))
                && Arrays.stream(clazz.getDeclaredConstructors())
                .anyMatch(c -> c.getParameterCount() == 0);
    }

}
