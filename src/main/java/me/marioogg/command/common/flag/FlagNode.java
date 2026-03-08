package me.marioogg.command.common.flag;

import lombok.Data;

import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

/**
 * Holds runtime metadata for a single {@link Flag} annotated param
 */
@Data
public class FlagNode {
    /** Primary flag token, e.g. {@code "-s"}. */
    private final String value;

    /** All tokens that activate this flag (value + aliases). */
    private final List<String> tokens;

    /** readable description. */
    private final String description;


    /** The underlying reflection param */
    private final Parameter parameter;

    public FlagNode(Flag flag, Parameter parameter) {
        this.value = flag.value();
        this.description = flag.description();
        this.parameter = parameter;

        List<String> t = new ArrayList<>();
        t.add(flag.value());
        t.addAll(Arrays.asList(flag.aliases()));
        this.tokens = t;
    }

    /**
     * Returns {@code true} if the given argument token activates this flag.
     */
    public boolean matches(String arg) {
        for (String token : tokens) {
            if (token.equalsIgnoreCase(arg)) return true;
        }
        return false;
    }
}

