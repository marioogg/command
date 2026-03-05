package me.marioogg.command.bungee;

import com.google.common.reflect.ClassPath;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import me.marioogg.command.bungee.node.BungeeCommandNode;
import me.marioogg.command.bungee.parameter.BungeeParamProcessor;
import me.marioogg.command.bungee.parameter.BungeeProcessor;
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
                .forEach(info -> registerCommands(info.load(), plugin));
    }

    @SneakyThrows
    public static void registerCommands(Class<?> commandClass, Plugin plugin) {
        BungeeCommandHandler.setPlugin(plugin);
        registerCommands(commandClass.getDeclaredConstructor().newInstance());
    }

    @SneakyThrows
    public static void registerCommands(Plugin plugin, Class<?>... commandClasses) {
        BungeeCommandHandler.setPlugin(plugin);
        for (Class<?> commandClass : commandClasses) {
            registerCommands(commandClass.getDeclaredConstructor().newInstance());
        }
    }

    public static void registerCommands(Object commandClass) {
        Arrays.stream(commandClass.getClass().getDeclaredMethods()).forEach(method -> {
            me.marioogg.command.Command command = method.getAnnotation(me.marioogg.command.Command.class);
            if (command == null) return;
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
}

