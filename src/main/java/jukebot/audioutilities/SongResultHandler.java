package jukebot.audioutilities;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import jukebot.utils.Bot;
import jukebot.utils.Helpers;
import jukebot.utils.Permissions;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

import java.util.List;

public class SongResultHandler implements AudioLoadResultHandler {

    private final Permissions permissions = new Permissions();
    private final GuildMessageReceivedEvent e;
    private final MusicManager musicManager;
    private final boolean useSelection;

    public SongResultHandler(GuildMessageReceivedEvent e, MusicManager m, boolean UseSelection) {
        this.e = e;
        this.musicManager = m;
        this.useSelection = UseSelection;
    }

    @Override
    public void trackLoaded(AudioTrack track) {
        if (!canQueueTrack(track, this.e.getAuthor().getIdLong())) {
            this.e.getChannel().sendMessage(new EmbedBuilder()
                    .setColor(Bot.EmbedColour)
                    .setTitle("Track Unavailable")
                    .setDescription("This track exceeds certain limits. [Remove these limits by donating!](https://patreon.com/Devoxin)")
                    .build()
            ).queue();
            return;
        }

        if (musicManager.handler.addToQueue(track, this.e.getAuthor().getIdLong()))
            e.getChannel().sendMessage(new EmbedBuilder()
                    .setColor(Bot.EmbedColour)
                    .setTitle("Track Enqueued")
                    .setDescription(track.getInfo().title)
                    .build()
            ).queue();
    }

    @Override
    public void playlistLoaded(AudioPlaylist playlist) {
        if (playlist.isSearchResult()) {

            if (this.useSelection) {

                StringBuilder selector = new StringBuilder();
                final List<AudioTrack> tracks = playlist.getTracks()
                        .subList(0, (playlist.getTracks().size() > 5 ? 5 : playlist.getTracks().size()));

                for (int i = 0; i < tracks.size(); i++) {
                    final AudioTrack track = tracks.get(i);
                    selector.append("`")
                            .append(i + 1)
                            .append(".` ")
                            .append(track.getInfo().title)
                            .append(" `")
                            .append(Helpers.fTime(track.getDuration()))
                            .append("`\n");
                }

                e.getChannel().sendMessage(new EmbedBuilder()
                        .setColor(Bot.EmbedColour)
                        .setTitle("Select Song")
                        .setDescription(selector.toString().trim())
                        .build()
                ).queue(m -> Bot.waiter.waitForSelection(this.e.getAuthor().getIdLong(), selected -> {
                    if (selected <= 0 || selected > tracks.size()) {
                        m.delete().queue();
                        return;
                    }

                    AudioTrack track = tracks.get(selected - 1);

                    if (!canQueueTrack(track, this.e.getAuthor().getIdLong())) {
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
                            .setTitle("Track Selected")
                            .setDescription(track.getInfo().title)
                            .build()
                    ).queue();

                    musicManager.handler.addToQueue(track, this.e.getAuthor().getIdLong());
                }));

            } else {
                AudioTrack track = playlist.getTracks().get(0);

                if (!canQueueTrack(track, this.e.getAuthor().getIdLong())) {
                    this.e.getChannel().sendMessage(new EmbedBuilder()
                            .setColor(Bot.EmbedColour)
                            .setTitle("Track Unavailable")
                            .setDescription("This track exceeds certain limits. [Remove these limits by donating!](https://patreon.com/Devoxin)")
                            .build()
                    ).queue();
                    return;
                }

                if (musicManager.handler.addToQueue(track, this.e.getAuthor().getIdLong()))
                    e.getChannel().sendMessage(new EmbedBuilder()
                            .setColor(Bot.EmbedColour)
                            .setTitle("Track Enqueued")
                            .setDescription(track.getInfo().title)
                            .build()
                    ).queue();
            }

        } else {

            final int importLimit = getPlaylistLimit(this.e.getAuthor().getIdLong());

            List<AudioTrack> tracks = playlist.getTracks();

            if (importLimit != -1 && playlist.getTracks().size() > importLimit)
                tracks = tracks.subList(0, importLimit);

            for (AudioTrack track : tracks)
                if (canQueueTrack(track, this.e.getAuthor().getIdLong()))
                    musicManager.handler.addToQueue(track, this.e.getAuthor().getIdLong());

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
        this.e.getChannel().sendMessage(new EmbedBuilder()
                .setColor(Bot.EmbedColour)
                .setTitle("No results found")
                .build()
        ).queue();

        if (!this.musicManager.isPlaying())
            Helpers.DisconnectVoice(this.e.getGuild().getAudioManager());
    }

    @Override
    public void loadFailed(FriendlyException ex) {
        this.e.getChannel().sendMessage(new EmbedBuilder()
                .setColor(Bot.EmbedColour)
                .setTitle("Failed to load track")
                .setDescription(ex.getMessage())
                .build()
        ).queue();

        if (!this.musicManager.isPlaying())
            Helpers.DisconnectVoice(this.e.getGuild().getAudioManager());
    }

    private boolean canQueueTrack(AudioTrack track, long requesterID) {
        final int trackLength = (int) Math.ceil(track.getDuration() / 1000);
        final int requesterTier = permissions.getTierLevel(requesterID);

        if (track.getInfo().isStream && requesterTier < 1)
            return false;

        /* 7500 = ~ 2 hours
         * 18500 = ~ 5 hours
         */

        int maxTrackDuration = 7500;
        if (requesterTier == 1)
            maxTrackDuration = 18500;
        if (requesterTier >= 2)
            maxTrackDuration = Integer.MAX_VALUE;

        return track.getInfo().isStream || trackLength <= maxTrackDuration;
    }

    private int getPlaylistLimit(long requesterID) {
        final int requesterTier = permissions.getTierLevel(requesterID);

        if (requesterTier < 1)
            return 100;

        if (requesterTier < 2)
            return 1000;

        return -1;
    }

}
