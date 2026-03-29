package me.marioogg.command.velocity;

import com.google.common.reflect.ClassPath;
import com.velocitypowered.api.proxy.ProxyServer;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import me.marioogg.command.Command;
import me.marioogg.command.Subcommand;
import me.marioogg.command.common.help.Help;
import me.marioogg.command.common.help.HelpNode;
import me.marioogg.command.velocity.node.VelocityCommandNode;
import me.marioogg.command.velocity.parameter.VelocityParamProcessor;
import me.marioogg.command.velocity.parameter.VelocityProcessor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;


/**
 * Handles command and processor registration for Velocity.
 */
@SuppressWarnings("all")
public class VelocityCommandHandler {
    @Getter @Setter private static Object plugin;
    @Getter @Setter private static ProxyServer proxy;

    private static Logger logger;

    @Setter @Getter private static Component noPermissionMessage = Component.text("I'm sorry, but you do not have permission to perform this command.", NamedTextColor.RED);
    @Setter @Getter private static Component playerOnlyMessage = Component.text("You must be a player to execute this command.", NamedTextColor.RED);
    @Setter @Getter private static Component consoleOnlyMessage = Component.text("This command can only be executed by console.", NamedTextColor.RED);
    @Setter @Getter private static Component internalErrorMessage = Component.text("An internal error occurred while executing this command.", NamedTextColor.RED);
    
    public static void init(Object plugin, ProxyServer proxy) {
        VelocityCommandHandler.plugin = plugin;
        VelocityCommandHandler.proxy = proxy;
        String pluginName = proxy.getPluginManager().fromInstance(plugin)
                .map(container -> container.getDescription().getId())
                .orElse(plugin.getClass().getSimpleName()); //backup because in some old versions it breaks
        VelocityCommandHandler.logger = LoggerFactory.getLogger(pluginName);
    }

    @SneakyThrows
    public static void registerCommands(String path, Object plugin, ProxyServer proxy) {
        VelocityCommandHandler.init(plugin, proxy);
        ClassPath.from(plugin.getClass().getClassLoader()).getAllClasses().stream()
                .filter(info -> info.getPackageName().startsWith(path))
                .map(ClassPath.ClassInfo::load)
                .filter(clazz -> !clazz.isAnonymousClass()
                        && !clazz.isLocalClass()
                        && !clazz.isInterface()
                        && !java.lang.reflect.Modifier.isAbstract(clazz.getModifiers()))
                .forEach(clazz -> registerCommands(clazz, plugin, proxy));
    }

    @SneakyThrows
    public static void registerCommands(Class<?> commandClass, Object plugin, ProxyServer proxy) {
        if (commandClass.isAnonymousClass() || commandClass.isLocalClass()
                || commandClass.isInterface() || java.lang.reflect.Modifier.isAbstract(commandClass.getModifiers())) return;
        VelocityCommandHandler.init(plugin, proxy);
        registerCommands(commandClass.getDeclaredConstructor().newInstance());
    }

    @SneakyThrows
    public static void registerCommands(Object plugin, ProxyServer proxy, Class<?>... commandClasses) {
        VelocityCommandHandler.init(plugin, proxy);
        for (Class<?> commandClass : commandClasses) {
            if (commandClass.isAnonymousClass() || commandClass.isLocalClass()
                    || commandClass.isInterface() || java.lang.reflect.Modifier.isAbstract(commandClass.getModifiers())) continue;
            registerCommands(commandClass.getDeclaredConstructor().newInstance());
        }
    }

    private static void registerCommands(Object commandClass) {
        Subcommand subcommand = commandClass.getClass().getAnnotation(Subcommand.class);

        Arrays.stream(commandClass.getClass().getDeclaredMethods()).forEach(method -> {
            me.marioogg.command.Command command = method.getAnnotation(me.marioogg.command.Command.class);
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

            new VelocityCommandNode(commandClass, method, command);
        });

        Arrays.stream(commandClass.getClass().getDeclaredMethods()).forEach(method -> {
            Help help = method.getAnnotation(Help.class);
            if (help == null) return;

            HelpNode helpNode = new HelpNode(commandClass, help.names(), help.permission(), method);
            VelocityCommandNode.getNodes().forEach(node -> node.getNames().forEach(name -> Arrays.stream(help.names())
                    .map(String::toLowerCase)
                    .filter(helpName -> name.toLowerCase().startsWith(helpName))
                    .forEach(helpName -> node.getHelpNodes().add(helpNode))));
        });
    }

    @SneakyThrows
    public static void registerProcessors(String path, Object plugin) {
        ClassPath.from(plugin.getClass().getClassLoader()).getAllClasses().stream()
                .filter(info -> info.getPackageName().startsWith(path))
                .filter(info -> info.load().getSuperclass().equals(VelocityProcessor.class))
                .forEach(info -> {
                    try { VelocityParamProcessor.createProcessor((VelocityProcessor<?>) info.load().getDeclaredConstructor().newInstance());
                    } catch (Exception e) { logger.error("Error registering command processors: ", e); }
                });
    }

    public static void registerProcessor(VelocityProcessor<?> processor) {
        VelocityParamProcessor.createProcessor(processor);
    }

    public static void registerProcessors(VelocityProcessor<?>... processors) {
        Arrays.stream(processors).forEach(VelocityCommandHandler::registerProcessor);
    }

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
}
