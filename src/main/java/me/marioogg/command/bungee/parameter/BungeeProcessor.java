package me.marioogg.command.bungee.parameter;

import lombok.Getter;
import lombok.SneakyThrows;
import net.md_5.bungee.api.CommandSender;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

@Getter
public abstract class BungeeProcessor<T> {

    private final Class<?> type;

    @SneakyThrows
    public BungeeProcessor() {
        Type type = ((ParameterizedType) this.getClass().getGenericSuperclass()).getActualTypeArguments()[0];
        this.type = Class.forName(type.getTypeName());
        BungeeParamProcessor.createProcessor(this);
    }

    @SneakyThrows
    public BungeeProcessor(Class<?> type) {
        this.type = type;
        BungeeParamProcessor.createProcessor(this);
    }

    public abstract T process(CommandSender sender, String supplied);

    public List<String> tabComplete(CommandSender sender, String supplied) {
        return new ArrayList<>();
    }
}

