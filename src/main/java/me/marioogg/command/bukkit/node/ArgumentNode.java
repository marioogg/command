package me.marioogg.command.bukkit.node;

import lombok.Data;

import java.lang.reflect.Parameter;

@Data
public class ArgumentNode {
    private final String name;
    private final boolean concated;
    private final boolean required;
    private final String defaultValue;
    private final Parameter parameter;
}
