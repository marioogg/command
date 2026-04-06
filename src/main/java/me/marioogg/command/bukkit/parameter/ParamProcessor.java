package me.marioogg.command.bukkit.parameter;

import lombok.Data;
import lombok.Getter;
import me.marioogg.command.bukkit.BukkitCommandHandler;
import me.marioogg.command.common.duration.Duration;
import me.marioogg.command.bukkit.node.ArgumentNode;
import me.marioogg.command.bukkit.parameter.impl.*;
import me.marioogg.command.common.validation.*;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Resolves and tab-completes command parameters for Bukkit.
 */
@Data
public class ParamProcessor {
    @Getter private static final HashMap<Class<?>, Processor<?>> processors = new HashMap<>();
    private static boolean loaded = false;

    private final Logger logger = BukkitCommandHandler.getLogger();

    private final ArgumentNode node;
    private final String supplied;
    private final CommandSender sender;

    /**
     * Processes the param into an object
     * @return Processed Object
     */
    public Object get() {
        if(!loaded) loadProcessors();

        Processor<?> processor = processors.get(node.getParameter().getType());
        if(processor == null) return supplied;

        Object result = processor.process(sender, supplied);
        if (result == null) return null;

        ValidationResult validation = Validator.validate(node.getParameter(), result);
        if (!validation.isValid()) {
            if (validation instanceof Min) {
                sender.sendMessage(BukkitCommandHandler.getMinValidationMessage().replace("{min}", String.valueOf(((Min) validation).value())));
            } else if (validation instanceof Max) {
                sender.sendMessage(BukkitCommandHandler.getMinValidationMessage().replace("{max}", String.valueOf(((Max) validation).value())));
            } else if (validation instanceof Matches) {
                sender.sendMessage(BukkitCommandHandler.getMatchesValidationMessage());
            }
            return null;
        }
        return result;
    }

    /**
     * Gets the tab completions for the param processor
     * @return Tab Completions
     */
    public List<String> getTabComplete() {
        if(!loaded) loadProcessors();

        Processor<?> processor = processors.get(node.getParameter().getType());
        if(processor == null) return new ArrayList<>();

        return processor.tabComplete(sender, supplied);
    }

    /**
     * Creates a new processor
     * @param processor Processor
     */
    public static void createProcessor(Processor<?> processor) {
        processors.put(processor.getType(), processor);
    }

    /**
     * Loads the processors
     */
    public static void loadProcessors() {
        loaded = true;

        processors.put(int.class, new IntegerProcessor());
        processors.put(long.class, new LongProcessor());
        processors.put(double.class, new DoubleProcessor());
        processors.put(boolean.class, new BooleanProcessor());
        processors.put(float.class, new FloatProcessor());

        processors.put(ChatColor.class, new ChatColorProcessor());
        processors.put(Player.class, new PlayerProcessor());
        processors.put(OfflinePlayer.class, new OfflinePlayerProcessor());
        processors.put(World.class, new WorldProcessor());
        processors.put(Duration.class, new DurationProcessor());
        processors.put(GameMode.class, new GamemodeProcessor());
    }
}
