package jukebot.audio.sourcemanagers.spotify.loaders

import com.sedmelluq.discord.lavaplayer.track.AudioItem
import jukebot.audio.sourcemanagers.spotify.SpotifyAudioSourceManager
import java.util.regex.Matcher
import java.util.regex.Pattern

interface Loader {

    /**
     * Returns the pattern used to match URLs for this loader.
     */
    fun pattern(): Pattern

    /**
     * Loads an AudioItem from the given regex match.
     */
    fun load(sourceManager: SpotifyAudioSourceManager, matcher: Matcher): AudioItem

}
