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
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.PARAMETER)
public @interface Flag {
    /**
     * The short flag name ex: {@code "-s"}.
     */
    String value();

    /**
     * Optional long-form aliases ex: {@code "--silent"}.
     */
    String[] aliases() default {};

    /**
     * human-readable description shown in usage/help messages.
     */
    String description() default "";


    /**
     * Default return value of a flag if not present in the command
     */
    boolean defaultValue() default true;
}

