package me.marioogg.command.common;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class as a subcommand group. All {@link Command}-annotated methods inside
 * the class will automatically have the root names defined here prepended to their own names.
 *
 * <p>A blank root name ({@code ""}) is treated as the root command itself — the method names
 * are registered as-is, without any prefix. This is useful when a single class needs to
 * handle both the base command and its subcommands side-by-side
 **/
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Subcommand {
    String[] names();
}

