package me.marioogg.command.bukkit.parameter.impl;

import me.marioogg.command.bukkit.parameter.Processor;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class FloatProcessor extends Processor<Float> {
    public Float process(CommandSender sender, String supplied) {
        try {
            return Float.parseFloat(supplied);
        } catch(Exception ex) {
            sender.sendMessage(ChatColor.RED + "The value you entered '" + supplied + "' is an invalid float.");
            return 0F;
        }
    }
}
