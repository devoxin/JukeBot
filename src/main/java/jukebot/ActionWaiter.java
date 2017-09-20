package jukebot;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import jukebot.audioutilities.GuildMusicManager;
import jukebot.utils.Bot;
import jukebot.utils.Parsers;
import jukebot.utils.TrackAction;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ActionWaiter extends ListenerAdapter {

    private HashMap<String, TrackAction> U2M = new HashMap<>();

    public void AddAction(String userID, Message m, List<AudioTrack> tracks, GuildMusicManager manager) {
        if (!U2M.containsKey(userID)) {
            U2M.put(userID, new TrackAction(m, tracks, manager));
            ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

            executor.schedule(() -> {
                if (U2M.containsKey(userID)) {
                    U2M.remove(userID);
                    m.delete().queue();
                }
                executor.shutdown();
            }, 10, TimeUnit.SECONDS);
        }
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent e) {

        if (!U2M.containsKey(e.getAuthor().getId()) || e.getGuild() == null)
            return;

        TrackAction t = U2M.get(e.getAuthor().getId());
        U2M.remove(e.getAuthor().getId());

        int i = Parsers.Number(e.getMessage().getContent(), -1);

        if (i == -1)
            t.m.delete().queue();

        if (i >= 0 && i <= t.tracks.size()) {
            if (e.getGuild().getSelfMember().hasPermission(e.getTextChannel(), Permission.MESSAGE_MANAGE))
                e.getMessage().delete().queue();

            AudioTrack track = t.tracks.get(i - 1);
            int result = t.manager.handler.queue(track, e.getAuthor().getId());
            if (result == 1) {
                t.m.editMessage(new EmbedBuilder()
                        .setColor(Bot.EmbedColour)
                        .setTitle("Song Enqueued")
                        .setDescription(track.getInfo().title)
                        .build()
                ).queue();
            } else if (result == -1) {
                t.m.editMessage(new EmbedBuilder()
                        .setColor(Bot.EmbedColour)
                        .setTitle("Song Unavailable")
                        .setDescription("The song is either a livestream or exceeds the duration limits.\nYou can queue longer songs & livestreams by [becoming a donator](https://www.patreon.com/Devoxin)")
                        .build()
                ).queue();
            } else {
                t.m.delete().queue();
            }

        }

        super.onMessageReceived(e);
    }

}
