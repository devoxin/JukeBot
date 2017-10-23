package jukebot.audioutilities;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import jukebot.ActionWaiter;
import jukebot.utils.Bot;
import jukebot.utils.Helpers;
import jukebot.utils.Permissions;
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
        if (!canQueueTrack(track, e.getAuthor().getIdLong())) {
            // TODO: Throw Error
        } else {
            musicManager.handler.addToQueue(track, e.getAuthor().getIdLong());
        }
    }

    @Override
    public void playlistLoaded(AudioPlaylist playlist) {
        if (playlist.isSearchResult()) {

            if (this.useSelection) {

                StringBuilder selector = new StringBuilder();
                final List<AudioTrack> tracks = playlist.getTracks()
                        .subList(0, (playlist.getTracks().size() > 5 ? 5 : playlist.getTracks().size()));

                for (int i = 0; i < tracks.size(); i++)
                    selector.append("`")
                            .append(i + 1)
                            .append(".` ")
                            .append(tracks.get(i).getInfo().title)
                            .append(" `")
                            .append(Helpers.fTime(tracks.get(i).getDuration()))
                            .append("`\n");

                e.getChannel().sendMessage(new EmbedBuilder()
                        .setColor(Bot.EmbedColour)
                        .setTitle("Select Song")
                        .setDescription(selector.toString().trim())
                        .build()
                ).queue(m -> {
                    Bot.waiter.waitForSelection(e.getAuthor().getIdLong(), selected -> {
                        if (selected <= 0 || selected > tracks.size()) {
                            m.editMessage(new EmbedBuilder()
                                    .setColor(Bot.EmbedColour)
                                    .setTitle("Selection Cancelled")
                                    .setDescription("An invalid option was specified")
                                    .build()
                            ).queue();
                            return;
                        }

                        AudioTrack track = tracks.get(selected - 1);

                        if (!canQueueTrack(track, e.getAuthor().getIdLong())) {
                            m.editMessage(new EmbedBuilder()
                                    .setColor(Bot.EmbedColour)
                                    .setTitle("Track Unavailable")
                                    .setDescription("This track exceeds certain limits. [Remove these limits by donating!](https://patreon.com/Devoxin)")
                                    .build()
                            ).queue();
                            return;
                        }

                        m.editMessage(new EmbedBuilder()
                                .setColor(Bot.EmbedColour)
                                .setTitle("Track Enqueued")
                                .setDescription(track.getInfo().title)
                                .build()
                        ).queue();

                        musicManager.handler.addToQueue(track, e.getAuthor().getIdLong());
                    });
                });

            } else {
                AudioTrack track = playlist.getTracks().get(0);

                if (!canQueueTrack(track, e.getAuthor().getIdLong())) {
                    // TODO: Throw Error
                } else {
                    musicManager.handler.addToQueue(track, e.getAuthor().getIdLong());
                }
            }

        } else {

            List<AudioTrack> tracks = playlist.getTracks().subList(0, getPlaylistLimit(e.getAuthor().getIdLong()));

            for (AudioTrack track : tracks)
                if (canQueueTrack(track, e.getAuthor().getIdLong()))
                    musicManager.handler.addToQueue(track, e.getAuthor().getIdLong());

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

        if (!this.musicManager.isPlaying() && this.musicManager.handler.getQueue().isEmpty())
            Helpers.DisconnectVoice(this.e.getGuild().getAudioManager());
    }

    @Override
    public void loadFailed(FriendlyException ex) {
        e.getChannel().sendMessage(new EmbedBuilder()
                .setColor(Bot.EmbedColour)
                .setTitle("Failed to load track")
                .setDescription(ex.getMessage())
                .build()
        ).queue();

        if (!this.musicManager.isPlaying() && this.musicManager.handler.getQueue().isEmpty())
            Helpers.DisconnectVoice(this.e.getGuild().getAudioManager());
    }

    public boolean canQueueTrack(AudioTrack track, long requesterID) {
        final int trackLength = (int) Math.ceil(track.getDuration() / 1000);
        final int requesterTier = permissions.getTierLevel(requesterID);

        return trackLength <= 7500 || requesterTier >= 1;
    }

    public int getPlaylistLimit(long requesterID) {
        final int requesterTier = permissions.getTierLevel(requesterID);

        if (requesterTier < 1)
            return 100;

        if (requesterTier < 2)
            return 1000;

        return Integer.MAX_VALUE;
    }

}
