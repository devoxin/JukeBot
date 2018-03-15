package jukebot;

import jukebot.utils.Helpers;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class ActionWaiter extends ListenerAdapter {

    private HashMap<Long, Consumer<String>> selectionMenus = new HashMap<>();

    public void waitForSelection(long userID, Consumer<String> selection) {
        if (!selectionMenus.containsKey(userID)) {
            selectionMenus.put(userID, selection);
            Helpers.schedule(() -> {
                if (selectionMenus.containsValue(selection))
                    selectionMenus.remove(userID).accept(null);
            }, 10, TimeUnit.SECONDS);
        }
    }

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent e) {
        if (selectionMenus.containsKey(e.getAuthor().getIdLong()))
            selectionMenus.remove(e.getAuthor().getIdLong()).accept(e.getMessage().getContentDisplay());
    }

}
