package me.marioogg.command;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Command {
    /**
     * Root command names
     */
    String[] names();

    /**
     * Command's permission
     */
    String permission() default "";

    /**
     * Whether or not the command is ran asynchronously
     */
    boolean async() default false;

    /**
     * Gets the description of the command
     */
    String description() default "";

    /**
     * Checks if command is console only
     */
    boolean consoleOnly() default false;

    /**
     * Checks if command is player only
     */
    boolean playerOnly() default false;

    /**
     * Checks if the command can be tab-completed
     */
    boolean allowComplete() default true;

    /**
     * Checks if the command should be hidden to non-op players (sending an "unknown command" message)
     */
    boolean hidden() default false;
}
