package jukebot.audioutilities;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import jukebot.JukeBot;
import jukebot.utils.Helpers;
import jukebot.utils.Permissions;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.managers.AudioManager;

import java.util.List;

public class SongResultHandler implements AudioLoadResultHandler {

    private final Permissions permissions = new Permissions();
    private final GuildMessageReceivedEvent e;
    private final AudioHandler musicManager;
    private final boolean useSelection;

    public SongResultHandler(GuildMessageReceivedEvent e, AudioHandler m, boolean UseSelection) {
        this.e = e;
        this.musicManager = m;
        this.useSelection = UseSelection;
    }

    @Override
    public void trackLoaded(AudioTrack track) {
        if (!canQueueTrack(track, e.getAuthor().getIdLong())) {
            e.getChannel().sendMessage(new EmbedBuilder()
                    .setColor(JukeBot.embedColour)
                    .setTitle("Track Unavailable")
                    .setDescription("This track exceeds certain limits. [Remove these limits by donating!](https://patreon.com/Devoxin)")
                    .build()
            ).queue();
            return;
        }

        if (musicManager.addToQueue(track, e.getAuthor().getIdLong()))
            e.getChannel().sendMessage(new EmbedBuilder()
                    .setColor(JukeBot.embedColour)
                    .setTitle("Track Enqueued")
                    .setDescription(track.getInfo().title)
                    .build()
            ).queue();
    }

    @Override
    public void playlistLoaded(AudioPlaylist playlist) {
        if (playlist.isSearchResult()) {

            if (useSelection) {

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
                        .setColor(JukeBot.embedColour)
                        .setTitle("Select Song")
                        .setDescription(selector.toString().trim())
                        .build()
                ).queue(m -> JukeBot.waiter.waitForSelection(e.getAuthor().getIdLong(), selected -> {
                    int s = Helpers.parseNumber(selected, 0);

                    if (s <= 0 || s > tracks.size()) {
                        m.delete().queue(null, e -> System.out.println("Failed to delete selection menu"));

                        AudioManager manager = e.getGuild().getAudioManager();

                        if (!musicManager.isPlaying() && !selected.toLowerCase().contains("sel"))
                            manager.closeAudioConnection();

                        return;
                    }

                    AudioTrack track = tracks.get(s - 1);

                    if (!canQueueTrack(track, e.getAuthor().getIdLong())) {
                        m.editMessage(new EmbedBuilder()
                                .setColor(JukeBot.embedColour)
                                .setTitle("Track Unavailable")
                                .setDescription("This track exceeds certain limits. [Remove these limits by donating!](https://patreon.com/Devoxin)")
                                .build()
                        ).queue();
                        return;
                    }

                    m.editMessage(new EmbedBuilder()
                            .setColor(JukeBot.embedColour)
                            .setTitle("Track Selected")
                            .setDescription(track.getInfo().title)
                            .build()
                    ).queue();

                    musicManager.addToQueue(track, e.getAuthor().getIdLong());
                }));

            } else {
                AudioTrack track = playlist.getTracks().get(0);

                if (!canQueueTrack(track, e.getAuthor().getIdLong())) {
                    e.getChannel().sendMessage(new EmbedBuilder()
                            .setColor(JukeBot.embedColour)
                            .setTitle("Track Unavailable")
                            .setDescription("This track exceeds certain limits. [Remove these limits by donating!](https://patreon.com/Devoxin)")
                            .build()
                    ).queue();
                    return;
                }

                if (musicManager.addToQueue(track, e.getAuthor().getIdLong()))
                    e.getChannel().sendMessage(new EmbedBuilder()
                            .setColor(JukeBot.embedColour)
                            .setTitle("Track Enqueued")
                            .setDescription(track.getInfo().title)
                            .build()
                    ).queue();
            }

        } else {

            final int importLimit = getPlaylistLimit(e.getAuthor().getIdLong());

            List<AudioTrack> tracks = playlist.getTracks();

            if (importLimit != -1 && playlist.getTracks().size() > importLimit)
                tracks = tracks.subList(0, importLimit);

            for (AudioTrack track : tracks)
                if (canQueueTrack(track, e.getAuthor().getIdLong()))
                    musicManager.addToQueue(track, e.getAuthor().getIdLong());

            e.getChannel().sendMessage(new EmbedBuilder()
                    .setColor(JukeBot.embedColour)
                    .setTitle("Enqueued Playlist")
                    .setDescription(playlist.getName() + " - " + tracks.size() + " tracks.")
                    .build()
            ).queue();

        }
    }

    @Override
    public void noMatches() {
        e.getChannel().sendMessage(new EmbedBuilder()
                .setColor(JukeBot.embedColour)
                .setTitle("No results found")
                .build()
        ).queue();

        if (!musicManager.isPlaying())
            e.getGuild().getAudioManager().closeAudioConnection();
    }

    @Override
    public void loadFailed(FriendlyException ex) {
        e.getChannel().sendMessage(new EmbedBuilder()
                .setColor(JukeBot.embedColour)
                .setTitle("Failed to load track")
                .setDescription(ex.getMessage())
                .build()
        ).queue();

        if (!musicManager.isPlaying())
            e.getGuild().getAudioManager().closeAudioConnection();
    }

    private boolean canQueueTrack(AudioTrack track, long requesterID) {
        final int trackLength = (int) Math.ceil(track.getDuration() / 1000);
        final int requesterTier = permissions.getTierLevel(requesterID);

        /* 7500 = ~ 2 hours
         * 18500 = ~ 5 hours
         */

        int maxTrackDuration = 7500;
        if (requesterTier == 1)
            maxTrackDuration = 18500;
        if (requesterTier >= 2)
            maxTrackDuration = Integer.MAX_VALUE;

        return !JukeBot.limitationsEnabled || (track.getInfo().isStream && requesterTier != 0) || trackLength <= maxTrackDuration;
    }

    private int getPlaylistLimit(long requesterID) {
        final int requesterTier = permissions.getTierLevel(requesterID);

        if (!JukeBot.limitationsEnabled || requesterTier >= 2) // Everything else
            return -1;

        if (requesterTier == 0) // Tier 0
            return 100;

        return 1000; // Tier 1
    }

}
