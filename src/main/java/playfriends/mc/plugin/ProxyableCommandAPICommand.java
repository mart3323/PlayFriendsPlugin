package playfriends.mc.plugin;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.executors.ExecutorType;
import dev.jorel.commandapi.executors.PlayerCommandExecutor;
import dev.jorel.commandapi.executors.PlayerResultingCommandExecutor;
import org.bukkit.command.ProxiedCommandSender;
import org.bukkit.entity.Player;

/**
 * Same as CommandAPICommand, but overrides {@link #executesPlayer}
 * to automatically support `/execute as player run command`
 */
public class ProxyableCommandAPICommand extends CommandAPICommand {
    public ProxyableCommandAPICommand(String commandName) {
        super(commandName);
    }

    @Override
    public CommandAPICommand executesPlayer(PlayerCommandExecutor executor) {
        return super.executes((sender, args) -> {
            if (sender instanceof ProxiedCommandSender proxy) {
                if (proxy.getCallee() instanceof Player player) {
                    executor.run(player, args);
                    return;
                }
            }
            if (sender instanceof Player player) {
                executor.run(player, args);
                return;
            }
            throw CommandAPI.fail("This command can only be run by or as players");
        }, ExecutorType.PLAYER, ExecutorType.PROXY);
    }

    @Override
    public CommandAPICommand executesPlayer(PlayerResultingCommandExecutor executor) {
        return super.executes((sender, args) -> {
            if (sender instanceof ProxiedCommandSender proxy) {
                if (proxy.getCallee() instanceof Player player) {
                    return executor.run(player, args);
                }
            }
            if (sender instanceof Player player) {
                return executor.run(player, args);
            }
            throw CommandAPI.fail("This command can only be run by or as players");
        }, ExecutorType.PLAYER, ExecutorType.PROXY);
    }
}
