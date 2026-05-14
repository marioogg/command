package me.marioogg.command.brigadier.parameter;

import lombok.Data;
import lombok.Getter;
import me.marioogg.command.brigadier.BrigadierCommandHandler;
import me.marioogg.command.common.node.ArgumentNode;
import me.marioogg.command.common.validation.Matches;
import me.marioogg.command.common.validation.Max;
import me.marioogg.command.common.validation.Min;
import me.marioogg.command.common.validation.ValidationResult;
import me.marioogg.command.common.validation.Validator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Resolves and tab-completes command parameters for Brigadier.
 */
@Data
public class BrigadierParamProcessor<S> {
    @Getter private static final HashMap<Class<?>, BrigadierProcessor<?, ?>> processors = new HashMap<>();
    private static boolean loaded = false;

    private final BrigadierCommandHandler<S> handler;
    private final ArgumentNode node;
    private final String supplied;
    private final S source;

    @SuppressWarnings("unchecked")
    public Object get() {
        if (!loaded) loadProcessors();

        BrigadierProcessor<S, ?> processor = (BrigadierProcessor<S, ?>) processors.get(node.getParameter().getType());
        if (processor == null) return supplied;

        Object result = processor.process(source, supplied);
        if (result == null) return null;

        ValidationResult validation = Validator.validate(node.getParameter(), result);
        if (!validation.isValid()) {
            if (validation instanceof Min min) {
                handler.getSourceAdapter().sendMessage(source,
                        handler.getMinValidationMessage().replace("{min}", String.valueOf(min.value())));
            } else if (validation instanceof Max max) {
                handler.getSourceAdapter().sendMessage(source,
                        handler.getMaxValidationMessage().replace("{max}", String.valueOf(max.value())));
            } else if (validation instanceof Matches) {
                handler.getSourceAdapter().sendMessage(source, handler.getMatchesValidationMessage());
            }
            return null;
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    public List<String> getTabComplete() {
        if (!loaded) loadProcessors();

        BrigadierProcessor<S, ?> processor = (BrigadierProcessor<S, ?>) processors.get(node.getParameter().getType());
        if (processor == null) return new ArrayList<>();

        return processor.tabComplete(source, supplied);
    }

    public static void createProcessor(BrigadierProcessor<?, ?> processor) {
        processors.put(processor.getType(), processor);
    }

    public static void loadProcessors() {
        loaded = true;

        processors.put(int.class, new BrigadierProcessor<Object, Integer>(int.class) {
            public Integer process(Object source, String supplied) {
                try {
                    return Integer.parseInt(supplied);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        });

        processors.put(long.class, new BrigadierProcessor<Object, Long>(long.class) {
            public Long process(Object source, String supplied) {
                try {
                    return Long.parseLong(supplied);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        });

        processors.put(double.class, new BrigadierProcessor<Object, Double>(double.class) {
            public Double process(Object source, String supplied) {
                try {
                    return Double.parseDouble(supplied);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        });

        processors.put(float.class, new BrigadierProcessor<Object, Float>(float.class) {
            public Float process(Object source, String supplied) {
                try {
                    return Float.parseFloat(supplied);
                } catch (NumberFormatException e) {
                    return null;
                }
            }
        });

        processors.put(boolean.class, new BrigadierProcessor<Object, Boolean>(boolean.class) {
            public Boolean process(Object source, String supplied) {
                return Boolean.parseBoolean(supplied);
            }
        });
    }
}

