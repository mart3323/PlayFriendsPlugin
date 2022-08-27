package playfriends.mc.plugin;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import playfriends.mc.plugin.api.ConfigAwareListener;
import playfriends.mc.plugin.api.ScheduledTask;
import playfriends.mc.plugin.features.afkdetection.AfkDetectionHandler;
import playfriends.mc.plugin.features.afkdetection.AfkDetectionTask;
import playfriends.mc.plugin.features.greeting.PlayerGreetingHandler;
import playfriends.mc.plugin.features.keepinventory.KeepInventoryHandler;
import playfriends.mc.plugin.features.peaceful.PeacefulMobTargetingHandler;
import playfriends.mc.plugin.features.peaceful.PeacefulStateHandler;
import playfriends.mc.plugin.features.perf.PerformanceEvent;
import playfriends.mc.plugin.features.perf.PerformanceHandler;
import playfriends.mc.plugin.features.perf.PerformanceMonitor;
import playfriends.mc.plugin.features.perf.PerformanceMonitorTask;
import playfriends.mc.plugin.features.playerprofile.PlayerProfileHandler;
import playfriends.mc.plugin.features.sleepvoting.SleepVotingHandler;
import playfriends.mc.plugin.playerdata.PlayerDataManager;
import playfriends.mc.plugin.playerdata.SavePlayerDataTask;

import java.time.Clock;
import java.util.List;

/** Main entry point for the plugin. */
@SuppressWarnings("unused")
public class Main extends JavaPlugin implements TabCompleter {
    private static final String ONLY_PLAYERS_CAN_USE_THIS_COMMAND_MSG = ChatColor.RED + "Only players can use this command.";

    /** The player data manager, to manager the player. */
    private final PlayerDataManager playerDataManager;

    /** The performance monitor. */
    private final PerformanceMonitor monitor;

    /** The list of enabled config aware event listeners. */
    private final List<ConfigAwareListener> configAwareListeners;

    /** The list of scheduled tasks. */
    private final List<ScheduledTask> scheduledTasks;

    /** The server's plugin manager to register event listeners to. */
    private PluginManager pluginManager;

    /** Creates the plugin. */
    public Main() {
        final Clock clock = Clock.systemUTC();

        this.playerDataManager = new PlayerDataManager(getDataFolder(), getLogger(), clock);
        this.monitor = new PerformanceMonitor(clock);

        this.configAwareListeners = List.of(
                new AfkDetectionHandler(this, playerDataManager, clock),
                new PeacefulMobTargetingHandler(playerDataManager),
                new PeacefulStateHandler(playerDataManager, getLogger()),
                new PlayerGreetingHandler(playerDataManager),
                new SleepVotingHandler(this, playerDataManager),
                new PerformanceHandler(this.monitor),
                new KeepInventoryHandler(playerDataManager),
                new PlayerProfileHandler(this, playerDataManager)
        );

        this.scheduledTasks = List.of(
                new SavePlayerDataTask(playerDataManager),
                new AfkDetectionTask(this, playerDataManager, clock),
                new PerformanceMonitorTask(monitor)
        );
    }

    @Override
    public void onDisable() {
        playerDataManager.saveAll();
    }

    @Override
    public void onEnable() {
        pluginManager = getServer().getPluginManager();

        final FileConfiguration config = this.getConfig();
        config.options().copyDefaults(true);
        saveDefaultConfig();

        this.monitor.updateConfig(config);

        for (ConfigAwareListener configAwareListener : configAwareListeners) {
            configAwareListener.updateConfig(config);
            pluginManager.registerEvents(configAwareListener, this);
        }

        final BukkitScheduler scheduler = getServer().getScheduler();
        for (ScheduledTask scheduledTask : scheduledTasks) {
            scheduledTask.updateConfig(config);
            scheduler.runTaskTimer(this, scheduledTask, scheduledTask.getInitialDelayInTicks(), scheduledTask.getIntervalInTicks());
        }

        playerDataManager.loadAll();
    }


    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        switch(command.getName()) {
            // These commands have no arguments
            case "perf":
                return List.of();

            // all other commands should fall back to the default executor
            default:
                return null;
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        switch (command.getName()) {
            case "perf" -> pluginManager.callEvent(new PerformanceEvent(sender));
            default     -> {
                sender.sendMessage(ChatColor.RED + "I don't know a command named " + command.getName() + "!");
                return false;
            }
        }
        return true;
    }
}
