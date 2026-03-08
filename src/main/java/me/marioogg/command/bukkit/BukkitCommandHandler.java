package me.marioogg.command.bukkit;

import com.google.common.reflect.ClassPath;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import me.marioogg.command.Command;
import me.marioogg.command.common.help.Help;
import me.marioogg.command.common.help.HelpNode;
import me.marioogg.command.bukkit.node.CommandNode;
import me.marioogg.command.bukkit.parameter.ParamProcessor;
import me.marioogg.command.bukkit.parameter.Processor;
import org.bukkit.plugin.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class BukkitCommandHandler {
    @Getter @Setter private static Plugin plugin;

    @Getter private static final Logger logger = LoggerFactory.getLogger(plugin.getName());

    /**
     * Registers commands based off a file path
     * @param path Path
     */
    @SneakyThrows
    public static void registerCommands(String path, Plugin plugin) {
        ClassPath.from(plugin.getClass().getClassLoader()).getAllClasses().stream()
                .filter(info -> info.getPackageName().startsWith(path))
                .forEach(info -> registerCommands(info.load(), plugin));
    }

    /**
     * Registers the commands in the class
     * @param commandClass Class
     */
    @SneakyThrows
    public static void registerCommands(Class<?> commandClass, Plugin plugin) {
        BukkitCommandHandler.setPlugin(plugin);
        registerCommands(commandClass.getDeclaredConstructor().newInstance());
    }

    /**
     * Registers the commands in the class
     * @param commandClasses Classes
     */
    @SneakyThrows
    public static void registerCommands(Plugin plugin, Class<?>... commandClasses) {
        BukkitCommandHandler.setPlugin(plugin);
        for (Class<?> commandClass : commandClasses) {
            registerCommands(commandClass.getDeclaredConstructor().newInstance());
        }
    }

    /**
     * Registers the commands in the class
     * @param commandClass Class
     */
    public static void registerCommands(Object commandClass) {
        Arrays.stream(commandClass.getClass().getDeclaredMethods()).forEach(method -> {
            Command command = method.getAnnotation(Command.class);
            if(command == null) return;

            new CommandNode(commandClass, method, command);
        });

        Arrays.stream(commandClass.getClass().getDeclaredMethods()).forEach(method -> {
            Help help = method.getAnnotation(Help.class);
            if(help == null) return;

            HelpNode helpNode = new HelpNode(commandClass, help.names(), help.permission(), method);
            CommandNode.getNodes().forEach(node -> node.getNames().forEach(name -> Arrays.stream(help.names())
                    .map(String::toLowerCase)
                    .filter(helpName -> name.toLowerCase().startsWith(helpName))
                    .forEach(helpName -> node.getHelpNodes().add(helpNode))));
        });
    }

    /**
     * Registers processors based off a file path
     * @param path Path
     */
    @SneakyThrows
    public static void registerProcessors(String path, Plugin plugin) {
        ClassPath.from(plugin.getClass().getClassLoader()).getAllClasses().stream()
                .filter(info -> info.getPackageName().startsWith(path))
                .filter(info -> info.load().getSuperclass().equals(Processor.class))
                .forEach(info -> {
                    try { ParamProcessor.createProcessor((Processor<?>) info.load().getDeclaredConstructor().newInstance());
                    } catch(Exception e) { logger.error("Error registering command processors: ", e); }
                });
    }

    /**
     * Register processor
     * @param processor Processor
     */
    public static void registerProcessor(Processor<?> processor) {
        ParamProcessor.createProcessor(processor);
    }

    /**
     * Register processors
     * @param processors Processors
     */
    public static void registerProcessors(Processor<?>... processors) {
        Arrays.stream(processors).forEach(BukkitCommandHandler::registerProcessor);
    }
}
