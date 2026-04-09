package me.marioogg.command.common.node;

import lombok.Data;

import java.lang.reflect.Parameter;

/**
 * Stores metadata for a command argument parameter.
 */
@Data
public class ArgumentNode {
    private final String name;
    private final boolean concated;
    private final boolean required;
    private final String defaultValue;
    private final Parameter parameter;
}

