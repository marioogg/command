package me.marioogg.command.bukkit.scheduler;

import me.marioogg.command.bukkit.BukkitCommandHandler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;

public class SchedulerUtil {

    private static final Logger logger = LoggerFactory.getLogger(SchedulerUtil.class);

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
                // noinspection JavaReflectionMemberAccess
                Object asyncScheduler = Bukkit.class.getMethod("getAsyncScheduler").invoke(null);
                Method runNow = asyncScheduler.getClass().getMethod("runNow", org.bukkit.plugin.Plugin.class, java.util.function.Consumer.class);
                runNow.invoke(asyncScheduler, BukkitCommandHandler.getPlugin(), (java.util.function.Consumer<Object>) s -> task.run());
            } catch (Exception e) {
                logger.error("Error running an asynchronous task for folia: ", e);
            }
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(BukkitCommandHandler.getPlugin(), task);
        }
    }

    public static void runSync(Runnable task) {
        if (FOLIA) {
            try {
                // noinspection JavaReflectionMemberAccess
                Object globalScheduler = Bukkit.class.getMethod("getGlobalRegionScheduler").invoke(null);
                Method run = globalScheduler.getClass().getMethod("run", org.bukkit.plugin.Plugin.class, java.util.function.Consumer.class);
                run.invoke(globalScheduler, BukkitCommandHandler.getPlugin(), (java.util.function.Consumer<Object>) s -> task.run());
            } catch (Exception e) {
                logger.error("Error running an synchronous task for folia: ", e);
            }
        } else {
            Bukkit.getScheduler().runTask(BukkitCommandHandler.getPlugin(), task);
        }
    }

    public static void runSyncForEntity(Entity entity, Runnable task) {
        if (FOLIA) {
            try {
                // noinspection JavaReflectionMemberAccess
                Object entityScheduler = Entity.class.getMethod("getScheduler").invoke(entity);
                Method run = entityScheduler.getClass().getMethod("run", org.bukkit.plugin.Plugin.class, java.util.function.Consumer.class, Runnable.class);
                run.invoke(entityScheduler, BukkitCommandHandler.getPlugin(), (java.util.function.Consumer<Object>) s -> task.run(), null);
            } catch (Exception e) {
                logger.error("Error running an synchronous task (entity specific) for folia: ", e);
            }
        } else {
            Bukkit.getScheduler().runTask(BukkitCommandHandler.getPlugin(), task);
        }
    }
}
