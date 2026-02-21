package me.marioogg.command.scheduler;

import me.marioogg.command.CommandHandler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;

import java.lang.reflect.Method;

public class SchedulerUtil {

    private static final boolean FOLIA;

    static {
        boolean folia;
        try {
            Class.forName("io.papermc.paper.threadedregions.RegionizedServer");
            folia = true;
        } catch (ClassNotFoundException e) {
            folia = false;
        }
        FOLIA = folia;
    }

    public static boolean isFolia() {
        return FOLIA;
    }

    public static void runAsync(Runnable task) {
        if (FOLIA) {
            try {
                Object asyncScheduler = Bukkit.class.getMethod("getAsyncScheduler").invoke(null);
                Method runNow = asyncScheduler.getClass().getMethod("runNow", org.bukkit.plugin.Plugin.class, java.util.function.Consumer.class);
                runNow.invoke(asyncScheduler, CommandHandler.getPlugin(), (java.util.function.Consumer<Object>) s -> task.run());
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(CommandHandler.getPlugin(), task);
        }
    }

    public static void runSync(Runnable task) {
        if (FOLIA) {
            try {
                Object globalScheduler = Bukkit.class.getMethod("getGlobalRegionScheduler").invoke(null);
                Method run = globalScheduler.getClass().getMethod("run", org.bukkit.plugin.Plugin.class, java.util.function.Consumer.class);
                run.invoke(globalScheduler, CommandHandler.getPlugin(), (java.util.function.Consumer<Object>) s -> task.run());
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Bukkit.getScheduler().runTask(CommandHandler.getPlugin(), task);
        }
    }

    public static void runSyncForEntity(Entity entity, Runnable task) {
        if (FOLIA) {
            try {
                Object entityScheduler = Entity.class.getMethod("getScheduler").invoke(entity);
                Method run = entityScheduler.getClass().getMethod("run", org.bukkit.plugin.Plugin.class, java.util.function.Consumer.class, Runnable.class);
                run.invoke(entityScheduler, CommandHandler.getPlugin(), (java.util.function.Consumer<Object>) s -> task.run(), null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Bukkit.getScheduler().runTask(CommandHandler.getPlugin(), task);
        }
    }
}
