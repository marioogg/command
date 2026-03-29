package me.marioogg.command.common.cooldown;

import java.util.HashMap;
import java.util.UUID;

/**
 * Manages per-player, per-command cooldown state.
 */
public class CooldownManager {
    private static final HashMap<String, HashMap<UUID, Long>> cooldowns = new HashMap<>();

    /**
     * Returns true if the player is still on cooldown for the given command.
     */
    public static boolean isOnCooldown(UUID uuid, String commandName) {
        HashMap<UUID, Long> commandCooldowns = cooldowns.get(commandName);
        if (commandCooldowns == null) return false;

        Long expiry = commandCooldowns.get(uuid);
        if (expiry == null) return false;

        return System.currentTimeMillis() < expiry;
    }

    /**
     * Registers a cooldown for the player on the given command.
     */
    public static void setCooldown(UUID uuid, String commandName, int seconds) {
        cooldowns
                .computeIfAbsent(commandName, k -> new HashMap<>())
                .put(uuid, System.currentTimeMillis() + (seconds * 1000L));
    }

    /**
     * Returns the remaining cooldown in seconds, or 0 if none.
     */
    public static long getRemainingSeconds(UUID uuid, String commandName) {
        HashMap<UUID, Long> commandCooldowns = cooldowns.get(commandName);
        if (commandCooldowns == null) return 0;

        Long expiry = commandCooldowns.get(uuid);
        if (expiry == null) return 0;

        return Math.max(0, (expiry - System.currentTimeMillis()) / 1000);
    }
}