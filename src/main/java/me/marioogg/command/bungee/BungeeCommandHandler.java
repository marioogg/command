package me.marioogg.command.bungee;

import com.google.common.reflect.ClassPath;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import me.marioogg.command.bungee.node.BungeeCommandNode;
import me.marioogg.command.bungee.parameter.BungeeParamProcessor;
import me.marioogg.command.bungee.parameter.BungeeProcessor;
import me.marioogg.command.Command;
import me.marioogg.command.Subcommand;
import me.marioogg.command.common.help.Help;
import me.marioogg.command.common.help.HelpNode;
import net.md_5.bungee.api.plugin.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

public class BungeeCommandHandler {
    @Getter @Setter private static Plugin plugin;

    @Getter private static final Logger logger = LoggerFactory.getLogger(plugin.getDescription().getName());

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
        BungeeCommandHandler.setPlugin(plugin);
        registerCommands(commandClass.getDeclaredConstructor().newInstance());
    }

    @SneakyThrows
    public static void registerCommands(Plugin plugin, Class<?>... commandClasses) {
        BungeeCommandHandler.setPlugin(plugin);
        for (Class<?> commandClass : commandClasses) {
            if (commandClass.isAnonymousClass() || commandClass.isLocalClass()
                    || commandClass.isInterface() || java.lang.reflect.Modifier.isAbstract(commandClass.getModifiers())) continue;
            registerCommands(commandClass.getDeclaredConstructor().newInstance());
        }
    }

    public static void registerCommands(Object commandClass) {
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

            new BungeeCommandNode(commandClass, method, command);
        });

        Arrays.stream(commandClass.getClass().getDeclaredMethods()).forEach(method -> {
            Help help = method.getAnnotation(Help.class);
            if (help == null) return;

            HelpNode helpNode = new HelpNode(commandClass, help.names(), help.permission(), method);
            BungeeCommandNode.getNodes().forEach(node -> node.getNames().forEach(name -> Arrays.stream(help.names())
                    .map(String::toLowerCase)
                    .filter(helpName -> name.toLowerCase().startsWith(helpName))
                    .forEach(helpName -> node.getHelpNodes().add(helpNode))));
        });
    }

    @SneakyThrows
    public static void registerProcessors(String path, Plugin plugin) {
        ClassPath.from(plugin.getClass().getClassLoader()).getAllClasses().stream()
                .filter(info -> info.getPackageName().startsWith(path))
                .filter(info -> info.load().getSuperclass().equals(BungeeProcessor.class))
                .forEach(info -> {
                    try { BungeeParamProcessor.createProcessor((BungeeProcessor<?>) info.load().getDeclaredConstructor().newInstance());
                    } catch (Exception e) { logger.error("Error registering command processors: ", e); }
                });
    }

    public static void registerProcessor(BungeeProcessor<?> processor) {
        BungeeParamProcessor.createProcessor(processor);
    }

    public static void registerProcessors(BungeeProcessor<?>... processors) {
        Arrays.stream(processors).forEach(BungeeCommandHandler::registerProcessor);
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

