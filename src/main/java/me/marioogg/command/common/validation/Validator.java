package me.marioogg.command.common.validation;

import java.lang.reflect.Parameter;

public class Validator {
    public static ValidationResult validate(Parameter parameter, Object value) {
        Min min = parameter.getAnnotation(Min.class);
        Max max = parameter.getAnnotation(Max.class);
        Matches matches = parameter.getAnnotation(Matches.class);

        if (min != null && value instanceof Number n && n.longValue() < min.value())
            return ValidationResult.fail("min");
        if (max != null && value instanceof Number n && n.longValue() > max.value())
            return ValidationResult.fail("max");
        if (matches != null && value instanceof String s && !s.matches(matches.value()))
            return ValidationResult.fail("matches");

        return ValidationResult.OK;
    }
}
