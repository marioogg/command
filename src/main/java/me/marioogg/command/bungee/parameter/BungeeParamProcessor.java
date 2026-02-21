package me.marioogg.command.bungee.parameter;

import lombok.Data;
import lombok.Getter;
import me.marioogg.command.node.ArgumentNode;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Data
public class BungeeParamProcessor {
    @Getter private static final HashMap<Class<?>, BungeeProcessor<?>> processors = new HashMap<>();
    private static boolean loaded = false;

    private final ArgumentNode node;
    private final String supplied;
    private final CommandSender sender;

    public Object get() {
        if (!loaded) loadProcessors();

        BungeeProcessor<?> processor = processors.get(node.getParameter().getType());
        if (processor == null) return supplied;

        return processor.process(sender, supplied);
    }

    public List<String> getTabComplete() {
        if (!loaded) loadProcessors();

        BungeeProcessor<?> processor = processors.get(node.getParameter().getType());
        if (processor == null) return new ArrayList<>();

        return processor.tabComplete(sender, supplied);
    }

    public static void createProcessor(BungeeProcessor<?> processor) {
        processors.put(processor.getType(), processor);
    }

    public static void loadProcessors() {
        loaded = true;

        processors.put(int.class, new BungeeProcessor<Integer>(int.class) {
            public Integer process(CommandSender sender, String supplied) {
                try { return Integer.parseInt(supplied); }
                catch (NumberFormatException e) {
                    sender.sendMessage(new TextComponent("\u00a7cInvalid number: " + supplied));
                    return null;
                }
            }
        });

        processors.put(long.class, new BungeeProcessor<Long>(long.class) {
            public Long process(CommandSender sender, String supplied) {
                try { return Long.parseLong(supplied); }
                catch (NumberFormatException e) {
                    sender.sendMessage(new TextComponent("\u00a7cInvalid number: " + supplied));
                    return null;
                }
            }
        });

        processors.put(double.class, new BungeeProcessor<Double>(double.class) {
            public Double process(CommandSender sender, String supplied) {
                try { return Double.parseDouble(supplied); }
                catch (NumberFormatException e) {
                    sender.sendMessage(new TextComponent("\u00a7cInvalid number: " + supplied));
                    return null;
                }
            }
        });

        processors.put(float.class, new BungeeProcessor<Float>(float.class) {
            public Float process(CommandSender sender, String supplied) {
                try { return Float.parseFloat(supplied); }
                catch (NumberFormatException e) {
                    sender.sendMessage(new TextComponent("\u00a7cInvalid number: " + supplied));
                    return null;
                }
            }
        });

        processors.put(boolean.class, new BungeeProcessor<Boolean>(boolean.class) {
            public Boolean process(CommandSender sender, String supplied) {
                return Boolean.parseBoolean(supplied);
            }
        });
    }
}

