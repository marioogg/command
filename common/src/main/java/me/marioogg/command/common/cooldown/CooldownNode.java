package me.marioogg.command.common.cooldown;

import lombok.Data;

/**
 * Stores cooldown metadata read from a {@link Cooldown} annotation.
 */
@Data
public class CooldownNode {
    private final int seconds;
    private final String bypassPermission;
}
