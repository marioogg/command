package me.marioogg.command.velocity;

import com.google.common.reflect.ClassPath;
import com.velocitypowered.api.proxy.ProxyServer;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import me.marioogg.command.help.Help;
import me.marioogg.command.help.HelpNode;
import me.marioogg.command.velocity.node.VelocityCommandNode;
import me.marioogg.command.velocity.parameter.VelocityParamProcessor;
import me.marioogg.command.velocity.parameter.VelocityProcessor;

import java.util.Arrays;

public class VelocityCommandHandler {
    @Getter @Setter private static Object plugin;
    @Getter @Setter private static ProxyServer proxy;

    public static void init(Object plugin, ProxyServer proxy) {
        VelocityCommandHandler.plugin = plugin;
        VelocityCommandHandler.proxy = proxy;
    }

    @SneakyThrows
    public static void registerCommands(String path, Object plugin, ProxyServer proxy) {
        VelocityCommandHandler.init(plugin, proxy);
        ClassPath.from(plugin.getClass().getClassLoader()).getAllClasses().stream()
                .filter(info -> info.getPackageName().startsWith(path))
                .forEach(info -> registerCommands(info.load(), plugin, proxy));
    }

    @SneakyThrows
    public static void registerCommands(Class<?> commandClass, Object plugin, ProxyServer proxy) {
        VelocityCommandHandler.init(plugin, proxy);
        registerCommands(commandClass.newInstance());
    }

    @SneakyThrows
    public static void registerCommands(Object plugin, ProxyServer proxy, Class<?>... commandClasses) {
        VelocityCommandHandler.init(plugin, proxy);
        for (Class<?> commandClass : commandClasses) {
            registerCommands(commandClass.newInstance());
        }
    }

    public static void registerCommands(Object commandClass) {
        Arrays.stream(commandClass.getClass().getDeclaredMethods()).forEach(method -> {
            me.marioogg.command.Command command = method.getAnnotation(me.marioogg.command.Command.class);
            if (command == null) return;
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
                    try { VelocityParamProcessor.createProcessor((VelocityProcessor<?>) info.load().newInstance());
                    } catch (Exception e) { e.printStackTrace(); }
                });
    }

    public static void registerProcessor(VelocityProcessor<?> processor) {
        VelocityParamProcessor.createProcessor(processor);
    }

    public static void registerProcessors(VelocityProcessor<?>... processors) {
        Arrays.stream(processors).forEach(VelocityCommandHandler::registerProcessor);
    }
}

