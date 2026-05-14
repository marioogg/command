package me.marioogg.command.brigadier.parameter;

import lombok.Getter;
import lombok.SneakyThrows;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Base processor for Brigadier command parameter types.
 */
@Getter
public abstract class BrigadierProcessor<S, T> {

    private final Class<?> type;

    @SneakyThrows
    public BrigadierProcessor() {
        Type type = ((ParameterizedType) this.getClass().getGenericSuperclass()).getActualTypeArguments()[1];
        this.type = Class.forName(type.getTypeName());
        BrigadierParamProcessor.createProcessor(this);
    }

    public BrigadierProcessor(Class<?> type) {
        this.type = type;
        BrigadierParamProcessor.createProcessor(this);
    }

    public abstract T process(S source, String supplied);

    public List<String> tabComplete(S source, String supplied) {
        return new ArrayList<>();
    }
}

