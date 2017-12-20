package jukebot;

import jukebot.utils.Helpers;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import java.util.HashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class ActionWaiter extends ListenerAdapter {

    private HashMap<Long, Consumer<Integer>> selectionMenus = new HashMap<>();

    public void waitForSelection(long userID, Consumer<Integer> selection) {
        if (!selectionMenus.containsKey(userID)) {
            selectionMenus.put(userID, selection);
            Helpers.createDelay(t -> {
                if (selectionMenus.containsKey(userID)) {
                    selectionMenus.remove(userID);
                    selection.accept(0);
                }
            }, 10, TimeUnit.SECONDS);
        }
    }

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent e) {
        if (!selectionMenus.containsKey(e.getAuthor().getIdLong()))
            return;

        if (e.getGuild().getSelfMember().hasPermission(e.getChannel(), Permission.MESSAGE_MANAGE))
            e.getMessage().delete().queue();

        selectionMenus
                .remove(e.getAuthor().getIdLong())
                .accept(Helpers.parseNumber(e.getMessage().getContentDisplay(), 0));
    }

}
