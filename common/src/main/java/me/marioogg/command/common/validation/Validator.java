package me.marioogg.command.common.validation;

import java.lang.reflect.Parameter;

public class Validator {
    public static ValidationResult validate(Parameter parameter, Object value) {
        Min min = parameter.getAnnotation(Min.class);
        Max max = parameter.getAnnotation(Max.class);
        Matches matches = parameter.getAnnotation(Matches.class);
        Range range = parameter.getAnnotation(Range.class);
        Length length = parameter.getAnnotation(Length.class);
        Confirm confirm = parameter.getAnnotation(Confirm.class);

        if (min != null && value instanceof Number n && n.longValue() < min.value())
            return ValidationResult.fail("min");
        if (max != null && value instanceof Number n && n.longValue() > max.value())
            return ValidationResult.fail("max");
        if (matches != null && value instanceof String s && !s.matches(matches.value()))
            return ValidationResult.fail("matches");
        if (range != null && value instanceof Number n && (n.doubleValue() < range.min() || n.doubleValue() > range.max()))
            return ValidationResult.fail("range");
        if (length != null && value instanceof String s &&
                (s.length() < length.min() || s.length() > length.max()))
            return ValidationResult.fail("length");
        if (confirm != null){// logic
            }
        return ValidationResult.OK;
    }
}
