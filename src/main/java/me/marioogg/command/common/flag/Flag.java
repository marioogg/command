package me.marioogg.command.common.flag;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a {@code boolean} parameter as a command flag (e.g. {@code -s} / {@code --silent}).
 * <p>
 * Usage example:
 * <pre>{@code
 * @Command(names = "ban")
 * public void ban(CommandSender sender,
 *                 @Param(name = "player")  Player player,
 *                 @Param(name = "reason")  String reason,
 *                 @Flag(value = "-s", aliases = "--silent", description = "Silently ban the player") boolean silent) { ... }
 * }</pre>
 *
 * Flags are always optional and default to {@code false}.
 * They can appear anywhere in the argument list and are stripped out
 * before positional parameters are resolved.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Flag {
    /**
     * The short flag name e.g. {@code "-s"}.
     */
    String value();

    /**
     * Optional long-form aliases e.g. {@code "--silent"}.
     */
    String[] aliases() default {};

    /**
     * human-readable description shown in usage/help messages.
     */
    String description() default "";

    boolean defaultValue() default true;
}

