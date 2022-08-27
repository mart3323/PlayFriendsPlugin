package playfriends.mc.plugin.features.playerprofile;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.Plugin;
import playfriends.mc.plugin.MessageUtils;
import playfriends.mc.plugin.ProxyableCommandAPICommand;
import playfriends.mc.plugin.api.ConfigAwareListener;
import playfriends.mc.plugin.playerdata.PlayerData;
import playfriends.mc.plugin.playerdata.PlayerDataManager;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;


public class PlayerProfileHandler implements ConfigAwareListener {

    private final PlayerDataManager playerDataManager;
    private final Plugin plugin;
    private String nameplate;
    private String message;
    private String discordSetMessage;
    private String pronounsSetMessage;
    private String listResponseMessage;
    private String listResponseSeparator;

    public PlayerProfileHandler(Plugin plugin, PlayerDataManager playerDataManager) {
        this.playerDataManager = playerDataManager;
        this.plugin = plugin;

        new ProxyableCommandAPICommand("discord")
            .withArguments(new GreedyStringArgument("discord name"))
            .executesPlayer((player, args) -> {
                this.setDiscord(player, player, (String) args[0]);
            })
            .register();
        new ProxyableCommandAPICommand("pronouns")
            .withArguments(new GreedyStringArgument("pronouns"))
            .executesPlayer((player, args) -> {
                this.setDiscord(player, player, (String) args[0]);
            })
            .register();
        new ProxyableCommandAPICommand("list")
            .executes((sender, args) -> {
                list(sender);
            })
            .register();
    }

    @Override
    public void updateConfig(FileConfiguration newConfig) {
        message = newConfig.getString("playerprofile.chat.message");
        nameplate = newConfig.getString("playerprofile.nameplate");
        discordSetMessage = newConfig.getString("playerprofile.commandfeedback.discord-set");
        pronounsSetMessage = newConfig.getString("playerprofile.commandfeedback.pronouns-set");
        listResponseMessage = newConfig.getString("playerprofile.list.message");
        listResponseSeparator = newConfig.getString("playerprofile.list.separator");
    }

    private String getPlayerNameplate(PlayerData playerData) {
        String discord = playerData.getDiscordName();
        String pronouns = playerData.getPronouns();
        if (discord == null && pronouns == null) return null;
        return MessageUtils.formatMessageWithPlaceholder(
            MessageUtils.formatMessageWithPlaceholder(
                nameplate,
                "{{DISCORD}}", discord == null ? "-" : discord
            ),
            "{{PRONOUNS}}", pronouns == null ? "-" : pronouns
        );
    }

    private TextComponent getPlayernameWithNameplate(PlayerData playerData) {
        String nameplate = getPlayerNameplate(playerData);
        if (nameplate == null) {
            return new TextComponent(playerData.getPlayerName());
        }
        return MessageUtils.formatWithHover(
            playerData.getPlayerName(),
            nameplate
        );
    }

    /**
     * Modify chat messages to display the nameplate on hover
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        PlayerData playerData = this.playerDataManager.getPlayerData(event.getPlayer().getUniqueId());
        TextComponent nameWithNameplate = getPlayernameWithNameplate(playerData);
        TextComponent message = MessageUtils.formatMessageWithPlaceholder(
            MessageUtils.formatMessageWithPlaceholder(
                this.message,
                "{{MESSAGE}}",
                event.getMessage()
            ),
            "{{PLAYER}}",
            nameWithNameplate
        );

        event.getRecipients().forEach(player -> {
            player.spigot().sendMessage(message);
        });
        event.setCancelled(true);
    }

    public void setPronouns(CommandSender sender, Player player, String pronouns) {
        playerDataManager.getPlayerData(player.getUniqueId())
            .setPronouns(pronouns);
        String message = MessageUtils.formatMessageWithPlaceholder(
            pronounsSetMessage,
            "{{PRONOUNS}}",
            pronouns
        );
        player.sendMessage(message);
        if (sender != player) sender.sendMessage(message);
    }

    public void setDiscord(CommandSender sender, Player player, String discord) {
        playerDataManager.getPlayerData(player.getUniqueId())
            .setDiscordName(discord);
        String message = MessageUtils.formatMessageWithPlaceholder(
            discordSetMessage,
            "{{DISCORD}}",
            discord
        );
        player.sendMessage(message);
        if (sender != player) sender.sendMessage(message);
    }

    public void list(CommandSender sender) {
        // Get a list of players sorted by their display names
        final List<? extends Player> sortedPlayers = new ArrayList<>(plugin.getServer().getOnlinePlayers());
        sortedPlayers.sort(Comparator.comparing(Player::getDisplayName));

        // Create a list of components consisting of nameplates and separators
        final String separator = MessageUtils.formatMessage(listResponseSeparator);
        final List<BaseComponent> components = new ArrayList<>();
        for (Player player : sortedPlayers) {
            final PlayerData playerData = playerDataManager.getPlayerData(player.getUniqueId());
            final TextComponent nameWithNameplate = getPlayernameWithNameplate(playerData);

            // Append the name and nameplate, separated by separators
            if (!components.isEmpty()) {
                components.add(new TextComponent(separator));
            }
            components.add(nameWithNameplate);
        }

        // Add all components to a root component
        final TextComponent listComponent = new TextComponent();
        listComponent.setExtra(components);

        // Format the message as configured and send it
        sender.spigot().sendMessage(MessageUtils.formatMessageWithPlaceholder(listResponseMessage, "{{PLAYERS}}", listComponent));
    }
}
