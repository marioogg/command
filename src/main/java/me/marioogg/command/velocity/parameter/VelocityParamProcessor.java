package me.marioogg.command.velocity.parameter;

import lombok.Data;
import lombok.Getter;
import me.marioogg.command.bukkit.node.ArgumentNode;
import com.velocitypowered.api.command.CommandSource;
import me.marioogg.command.common.validation.*;
import me.marioogg.command.velocity.VelocityCommandHandler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Resolves and tab-completes command parameters for Velocity.
 */
@Data
public class VelocityParamProcessor {
    @Getter private static final HashMap<Class<?>, VelocityProcessor<?>> processors = new HashMap<>();
    private static boolean loaded = false;

    private final ArgumentNode node;
    private final String supplied;
    private final CommandSource source;

    public Object get() {
        if (!loaded) loadProcessors();
        VelocityProcessor<?> processor = processors.get(node.getParameter().getType());
        if (processor == null) return supplied;
        Object result = processor.process(source, supplied);
        if (result == null) return null;

        ValidationResult validation = Validator.validate(node.getParameter(), result);
        if (!validation.isValid()) {
            if (validation instanceof Min) {
                source.sendMessage(replacePlaceholder(
                        VelocityCommandHandler.getMinValidationMessage(),
                        "{min}",
                        ((Min) validation).value()
                ));
            } else if (validation instanceof Max) {
                source.sendMessage(replacePlaceholder(
                        VelocityCommandHandler.getMaxValidationMessage(),
                        "{max}",
                        ((Max) validation).value()
                ));
            } else if (validation instanceof Matches) {
                source.sendMessage(VelocityCommandHandler.getMatchesValidationMessage());
            }
            return null;
        }
        return result;
    }

    public List<String> getTabComplete() {
        if (!loaded) loadProcessors();
        VelocityProcessor<?> processor = processors.get(node.getParameter().getType());
        if (processor == null) return new ArrayList<>();
        return processor.tabComplete(source, supplied);
    }

    public static void createProcessor(VelocityProcessor<?> processor) {
        processors.put(processor.getType(), processor);
    }

    private static Component replacePlaceholder(Component template, String placeholder, Object value) {
        return template.replaceText(config -> config
                .matchLiteral(placeholder)
                .replacement(String.valueOf(value)));
    }

    public static void loadProcessors() {
        loaded = true;
        processors.put(int.class, new VelocityProcessor<Integer>(int.class) {
            public Integer process(CommandSource source, String supplied) {
                try { return Integer.parseInt(supplied); }
                catch (NumberFormatException e) {
                    source.sendMessage(Component.text("Invalid number: " + supplied, NamedTextColor.RED));
                    return null;
                }
            }
        });
        processors.put(long.class, new VelocityProcessor<Long>(long.class) {
            public Long process(CommandSource source, String supplied) {
                try { return Long.parseLong(supplied); }
                catch (NumberFormatException e) {
                    source.sendMessage(Component.text("Invalid number: " + supplied, NamedTextColor.RED));
                    return null;
                }
            }
        });
        processors.put(double.class, new VelocityProcessor<Double>(double.class) {
            public Double process(CommandSource source, String supplied) {
                try { return Double.parseDouble(supplied); }
                catch (NumberFormatException e) {
                    source.sendMessage(Component.text("Invalid number: " + supplied, NamedTextColor.RED));
                    return null;
                }
            }
        });
        processors.put(float.class, new VelocityProcessor<Float>(float.class) {
            public Float process(CommandSource source, String supplied) {
                try { return Float.parseFloat(supplied); }
                catch (NumberFormatException e) {
                    source.sendMessage(Component.text("Invalid number: " + supplied, NamedTextColor.RED));
                    return null;
                }
            }
        });
        processors.put(boolean.class, new VelocityProcessor<Boolean>(boolean.class) {
            public Boolean process(CommandSource source, String supplied) {
                return Boolean.parseBoolean(supplied);
            }
        });
    }
}
