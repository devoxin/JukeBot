package jukebot.audioutilities;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import jukebot.JukeBot;
import jukebot.utils.Context;
import jukebot.utils.Helpers;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.managers.AudioManager;

import java.util.List;
import java.util.regex.Pattern;

public class SongResultHandler implements AudioLoadResultHandler {

    private final Pattern command = Pattern.compile("(?:p(?:lay)?|s(?:el(?:ect)?)?|sc(?:search)?)\\s.+");

    private final Context e;
    private final AudioHandler musicManager;
    private final boolean useSelection;

    public SongResultHandler(Context e, AudioHandler m, boolean UseSelection) {
        this.e = e;
        this.musicManager = m;
        this.useSelection = UseSelection;
    }

    @Override
    public void trackLoaded(AudioTrack track) {
        if (!canQueueTrack(track)) {
            e.sendEmbed("Track Unavailable", "This track exceeds certain limits. [Remove these limits by donating!](https://patreon.com/Devoxin)");
            return;
        }

        if (musicManager.addToQueue(track, e.getAuthor().getIdLong())) {
            e.sendEmbed("Track Enqueued", track.getInfo().title);
        }
    }

    @Override
    public void playlistLoaded(AudioPlaylist playlist) {
        if (playlist.isSearchResult()) {

            if (useSelection) {

                StringBuilder selector = new StringBuilder();

                final List<AudioTrack> tracks = playlist.getTracks().subList(0, Math.min(playlist.getTracks().size(), 5));

                for (int i = 0; i < tracks.size(); i++) {
                    final AudioTrack track = tracks.get(i);
                    selector.append("`")
                            .append(i + 1)
                            .append(".` ")
                            .append(track.getInfo().title)
                            .append(" `")
                            .append(Helpers.Companion.fTime(track.getDuration()))
                            .append("`\n");
                }

                e.getChannel().sendMessage(new EmbedBuilder()
                        .setColor(JukeBot.embedColour)
                        .setTitle("Select Song")
                        .setDescription(selector.toString().trim())
                        .build()
                ).queue(m -> JukeBot.waiter.waitForSelection(e.getAuthor().getIdLong(), selected -> {
                    int s = Helpers.Companion.parseNumber(selected, 0);

                    if (s <= 0 || s > tracks.size()) {
                        m.delete().queue();

                        AudioManager manager = e.getGuild().getAudioManager();

                        if (!musicManager.isPlaying() && (selected == null || !command.matcher(selected.toLowerCase()).find()))
                            manager.closeAudioConnection();

                        return;
                    }

                    AudioTrack track = tracks.get(s - 1);

                    if (!canQueueTrack(track)) {
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
                if (playlist.getTracks().isEmpty()) {
                    noMatches();
                    return;
                }

                final AudioTrack track = playlist.getTracks().get(0);

                if (!canQueueTrack(track)) {
                    e.sendEmbed("Track Unavailable", "This track exceeds certain limits. [Remove these limits by donating!](https://patreon.com/Devoxin)");
                    return;
                }

                if (musicManager.addToQueue(track, e.getAuthor().getIdLong())) {
                    e.sendEmbed("Track Enqueued", track.getInfo().title);
                }
            }

        } else {

            final List<AudioTrack> tracks = playlist.getTracks().subList(0, Math.min(playlist.getTracks().size(), getPlaylistLimit()));

            for (AudioTrack track : tracks) {
                if (canQueueTrack(track)) {
                    musicManager.addToQueue(track, e.getAuthor().getIdLong());
                }
            }

            e.sendEmbed("Playlist Enqueued", playlist.getName() + " - " + tracks.size() + " tracks");
        }
    }

    @Override
    public void noMatches() {
        e.sendEmbed("No Results", "Nothing found related to the query.");

        if (!musicManager.isPlaying()) {
            e.getGuild().getAudioManager().closeAudioConnection();
        }
    }

    @Override
    public void loadFailed(FriendlyException ex) {
        e.sendEmbed("Track Unavailable", ex.getMessage());

        if (!musicManager.isPlaying()) {
            e.getGuild().getAudioManager().closeAudioConnection();
        }
    }

    private boolean canQueueTrack(AudioTrack track) {
        final int trackLength = (int) Math.ceil(track.getDuration() / 1000);
        int maxTrackDuration = 7500;

        /* 7500 = ~ 2 hours
         * 18500 = ~ 5 hours
         */

        if (e.getDonorTier() == 1) {
            maxTrackDuration = 18500;
        } else if (e.getDonorTier() >= 2) {
            maxTrackDuration = Integer.MAX_VALUE;
        }

        return JukeBot.isSelfHosted || (track.getInfo().isStream && e.getDonorTier() != 0) || trackLength <= maxTrackDuration;
    }

    private int getPlaylistLimit() {
        if (JukeBot.isSelfHosted || e.getDonorTier() >= 2) // Everything else
            return Integer.MAX_VALUE;

        if (e.getDonorTier() == 0) // Tier 0
            return 100;

        return 1000; // Tier 1
    }

}
