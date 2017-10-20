package jukebot;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import jukebot.audioutilities.AudioHandler;
import jukebot.audioutilities.GuildMusicManager;
import jukebot.utils.Bot;
import jukebot.utils.Helpers;
import jukebot.utils.TrackAction;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;

import java.util.HashMap;
import java.util.List;

public class ActionWaiter extends ListenerAdapter {

    public static HashMap<Long, TrackAction> UserManagers = new HashMap<>();

    public void AddAction(Long userID, Message m, List<AudioTrack> tracks, GuildMusicManager manager) {
        UserManagers.computeIfAbsent(userID, v -> new TrackAction(m, tracks, manager, userID));
    }

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent e) {

        if (!UserManagers.containsKey(e.getAuthor().getIdLong()))
            return;

        TrackAction t = UserManagers.remove(e.getAuthor().getIdLong());
        t.waiter.shutdownNow();

        int i = Helpers.ParseNumber(e.getMessage().getContent(), -1);

        if (i <= 0 || i > t.tracks.size()) {
            if (!e.getMessage().getContent().toLowerCase().contains("sel"))
                t.m.editMessage(new EmbedBuilder().setColor(Bot.EmbedColour).setTitle("Selection Cancelled").setDescription("An invalid option was specified").build()).queue();
            else
                t.m.delete().queue();
            return;
        }

        if (e.getGuild().getSelfMember().hasPermission(e.getChannel(), Permission.MESSAGE_MANAGE))
            e.getMessage().delete().queue();

        AudioTrack track = t.tracks.get(i - 1);
        AudioHandler.TRACK_STATUS result = t.manager.handler.queue(track, e.getAuthor().getIdLong());
        if (AudioHandler.TRACK_STATUS.QUEUED == result) {
            t.m.editMessage(new EmbedBuilder()
                    .setColor(Bot.EmbedColour)
                    .setTitle("Song Enqueued")
                    .setDescription(track.getInfo().title)
                    .build()
            ).queue();
        } else if (AudioHandler.TRACK_STATUS.LIMITED == result) {
            t.m.editMessage(new EmbedBuilder()
                    .setColor(Bot.EmbedColour)
                    .setTitle("Song Unavailable")
                    .setDescription("Livestreams/Long tracks are unavailable.\nYou can queue them by [becoming a donator](https://www.patreon.com/Devoxin)")
                    .build()
            ).queue();
        } else if (AudioHandler.TRACK_STATUS.PLAYING == result){
            t.m.delete().queue();
        }

    }

}
