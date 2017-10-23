package jukebot;

import jukebot.utils.Helpers;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class ActionWaiter extends ListenerAdapter {

    private HashMap<Long, Consumer<Integer>> selectionMenus = new HashMap<>();
    // TODO: Incorporate schedulers into the <Long, Consumer> HashMap for ease?

    //private ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    /* TODO: Schedulers retain variables as they were when it was setup, which is a problem when checking
     * TODO: for users in the selectionMenus hashmap 10 seconds later
     */

    public void waitForSelection(long userID, Consumer<Integer> selection) {
        if (!selectionMenus.containsKey(userID)) {
            selectionMenus.put(userID, selection);
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
                .accept(Helpers.ParseNumber(e.getMessage().getContent(), 0));
    }

}
