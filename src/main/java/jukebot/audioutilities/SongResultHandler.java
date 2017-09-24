package jukebot.audioutilities;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import jukebot.utils.Bot;
import jukebot.utils.Permissions;
import jukebot.utils.Time;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

import java.util.List;

public class SongResultHandler implements AudioLoadResultHandler {

    private final Permissions permissions = new Permissions();
    private final GuildMessageReceivedEvent e;
    private final GuildMusicManager musicManager;
    private final boolean useSelection;

    public SongResultHandler(GuildMessageReceivedEvent e, GuildMusicManager m, boolean UseSelection) {
        this.e = e;
        this.musicManager = m;
        this.useSelection = UseSelection;
    }

    @Override
    public void trackLoaded(AudioTrack track) {
        int result = musicManager.handler.queue(track, e.getAuthor().getId());
        if (result == 1) {
            e.getChannel().sendMessage(new EmbedBuilder()
                    .setColor(Bot.EmbedColour)
                    .setTitle("Song Enqueued")
                    .setDescription(track.getInfo().title)
                    .build()
            ).queue();
        } else if (result == -1) {
            e.getChannel().sendMessage(new EmbedBuilder()
                    .setColor(Bot.EmbedColour)
                    .setTitle("Song Unavailable")
                    .setDescription("The song is either a livestream or exceeds the duration limits.\nYou can queue longer songs & livestreams by [becoming a donator](https://www.patreon.com/Devoxin)")
                    .build()
            ).queue();
        }
    }

    @Override
    public void playlistLoaded(AudioPlaylist playlist) {

        if (playlist.isSearchResult()) {

            if (this.useSelection) {

                StringBuilder selector = new StringBuilder();
                final List<AudioTrack> tracks = playlist.getTracks().subList(0, (playlist.getTracks().size() > 5 ? 5 : playlist.getTracks().size()));

                for (int i = 0; i < tracks.size(); i++) {
                    selector.append("`").append(i + 1).append(".` ").append(tracks.get(i).getInfo().title).append(" `").append(Time.format(tracks.get(i).getDuration())).append("`\n");
                }

                e.getChannel().sendMessage(new EmbedBuilder()
                        .setColor(Bot.EmbedColour)
                        .setTitle("Select Song")
                        .setDescription(selector.toString().trim())
                        .build()
                ).queue(m -> Bot.waiter.AddAction(e.getAuthor().getId(), m, tracks, musicManager));

            } else {

                int result = musicManager.handler.queue(playlist.getTracks().get(0), e.getAuthor().getId());
                if (result == 1) {
                    e.getChannel().sendMessage(new EmbedBuilder()
                            .setColor(Bot.EmbedColour)
                            .setTitle("Song Enqueued")
                            .setDescription(playlist.getTracks().get(0).getInfo().title)
                            .build()
                    ).queue();
                } else if (result == -1) {
                    e.getChannel().sendMessage(new EmbedBuilder()
                            .setColor(Bot.EmbedColour)
                            .setTitle("Song Unavailable")
                            .setDescription("The song is either a livestream or exceeds the duration limits.\nYou can queue longer songs & livestreams by [becoming a donator](https://www.patreon.com/Devoxin)")
                            .build()
                    ).queue();
                }

            }

        } else {

            List<AudioTrack> tracks = playlist.getTracks();
            if (tracks.size() > 100 && !permissions.isBaller(e.getAuthor().getId(), 1))
                tracks = tracks.subList(0, 100);

            for (AudioTrack track : tracks) {
                musicManager.handler.queue(track, e.getAuthor().getId());
            }

            e.getChannel().sendMessage(new EmbedBuilder()
                    .setColor(Bot.EmbedColour)
                    .setTitle("Enqueued Playlist")
                    .setDescription(playlist.getName() + " - " + tracks.size() + " tracks.")
                    .build()
            ).queue();

        }
    }

    @Override
    public void noMatches() {
        e.getChannel().sendMessage(new EmbedBuilder()
                .setColor(Bot.EmbedColour)
                .setTitle("No results found.")
                .build()
        ).queue();
    }

    @Override
    public void loadFailed(FriendlyException ex) {
        e.getChannel().sendMessage(new EmbedBuilder()
                .setColor(Bot.EmbedColour)
                .setTitle("Failed to load track")
                .setDescription(ex.getMessage())
                .build()
        ).queue();
    }

}
