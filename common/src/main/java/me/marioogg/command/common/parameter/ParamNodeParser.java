package me.marioogg.command.common.parameter;

import me.marioogg.command.common.node.ArgumentNode;
import me.marioogg.command.common.parameter.Param;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;

/**
 * Centralized parser for converting @Param annotations to ArgumentNode instances.
 */
public class ParamNodeParser {

    /**
     * Parses all @Param annotations from a method and converts them to ArgumentNode instances.
     *
     * @param method The method to parse
     * @return List of ArgumentNode instances
     */
    public static List<ArgumentNode> parseParameters(Method method) {
        List<ArgumentNode> parameters = new ArrayList<>();

        for (Parameter parameter : method.getParameters()) {
            Param param = parameter.getAnnotation(Param.class);
            if (param == null) continue;

            // If optional is true, mark as not required
            boolean isRequired = param.optional() ? false : param.required();
            String defaultValue = param.defaultValue().isEmpty() ? null : param.defaultValue();

            parameters.add(new ArgumentNode(
                    param.name(),
                    param.concated(),
                    isRequired,
                    param.optional(),
                    defaultValue,
                    parameter
            ));
        }

        return parameters;
    }
}

