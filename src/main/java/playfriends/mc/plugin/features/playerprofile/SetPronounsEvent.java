package playfriends.mc.plugin.features.playerprofile;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import playfriends.mc.plugin.api.PlayerEvent;

public class SetPronounsEvent extends PlayerEvent {
    private static final HandlerList HANDLERS_LIST = new HandlerList();
    private String pronouns;


    public String getPronouns() {
        return pronouns;
    }

    public SetPronounsEvent(Player player, String pronouns) {
        super(player);
        this.pronouns = pronouns;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS_LIST;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS_LIST;
    }
}
