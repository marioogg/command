package me.marioogg.command.velocity.parameter;

import lombok.Getter;
import lombok.SneakyThrows;
import com.velocitypowered.api.command.CommandSource;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

@Getter
public abstract class VelocityProcessor<T> {

    private final Class<?> type;

    @SneakyThrows
    public VelocityProcessor() {
        Type type = ((ParameterizedType) this.getClass().getGenericSuperclass()).getActualTypeArguments()[0];
        this.type = Class.forName(type.getTypeName());
        VelocityParamProcessor.createProcessor(this);
    }

    @SneakyThrows
    public VelocityProcessor(Class<?> type) {
        this.type = type;
        VelocityParamProcessor.createProcessor(this);
    }

    public abstract T process(CommandSource source, String supplied);

    public List<String> tabComplete(CommandSource source, String supplied) {
        return new ArrayList<>();
    }
}

