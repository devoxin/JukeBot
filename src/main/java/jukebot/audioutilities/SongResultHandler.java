package jukebot.audioutilities;

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler;
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException;
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist;
import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import jukebot.JukeBot;
import jukebot.utils.Context;
import jukebot.utils.Helpers;
import jukebot.utils.Permissions;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.managers.AudioManager;

import java.util.List;
import java.util.regex.Pattern;

public class SongResultHandler implements AudioLoadResultHandler {

    private final Pattern command = Pattern.compile("(?:p(?:lay)?|s(?:el(?:ect)?)?|sc(?:search)?)\\s.+");

    private final Permissions permissions = new Permissions();
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
        if (!canQueueTrack(track, e.getAuthor().getIdLong())) {
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
                        m.delete().queue();

                        AudioManager manager = e.getGuild().getAudioManager();

                        if (!musicManager.isPlaying() && (selected == null || !command.matcher(selected.toLowerCase()).find()))
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
                if (playlist.getTracks().isEmpty()) {
                    noMatches();
                    return;
                } // patch soundcloud searches not calling noMatches

                final AudioTrack track = playlist.getTracks().get(0);

                if (!canQueueTrack(track, e.getAuthor().getIdLong())) {
                    e.sendEmbed("Track Unavailable", "This track exceeds certain limits. [Remove these limits by donating!](https://patreon.com/Devoxin)");
                    return;
                }

                if (musicManager.addToQueue(track, e.getAuthor().getIdLong())) {
                    e.sendEmbed("Track Enqueued", track.getInfo().title);
                }
            }

        } else {

            final int importLimit = getPlaylistLimit(e.getAuthor().getIdLong());

            List<AudioTrack> tracks = playlist.getTracks();

            if (importLimit != -1 && tracks.size() > importLimit)
                tracks = tracks.subList(0, importLimit);

            for (AudioTrack track : tracks) {
                if (canQueueTrack(track, e.getAuthor().getIdLong())) {
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

    private boolean canQueueTrack(AudioTrack track, long requesterID) {
        final int trackLength = (int) Math.ceil(track.getDuration() / 1000);
        final int requesterTier = permissions.getTier(requesterID);

        /* 7500 = ~ 2 hours
         * 18500 = ~ 5 hours
         */

        int maxTrackDuration = 7500;
        if (requesterTier == 1)
            maxTrackDuration = 18500;
        if (requesterTier >= 2)
            maxTrackDuration = Integer.MAX_VALUE;

        return JukeBot.isSelfHosted || (track.getInfo().isStream && requesterTier != 0) || trackLength <= maxTrackDuration;
    }

    private int getPlaylistLimit(long requesterID) {
        final int requesterTier = permissions.getTier(requesterID);

        if (JukeBot.isSelfHosted || requesterTier >= 2) // Everything else
            return -1;

        if (requesterTier == 0) // Tier 0
            return 100;

        return 1000; // Tier 1
    }

}
