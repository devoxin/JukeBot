package jukebot.audio.sourcemanagers.youtube

import com.sedmelluq.discord.lavaplayer.container.MediaContainerDetection
import com.sedmelluq.discord.lavaplayer.container.MediaContainerDetectionResult
import com.sedmelluq.discord.lavaplayer.container.MediaContainerDetectionResult.refer
import com.sedmelluq.discord.lavaplayer.container.MediaContainerHints
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.http.HttpAudioSourceManager
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.tools.io.HttpClientTools
import com.sedmelluq.discord.lavaplayer.tools.io.HttpClientTools.getHeaderValue
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface
import com.sedmelluq.discord.lavaplayer.tools.io.PersistentHttpStream
import com.sedmelluq.discord.lavaplayer.track.AudioItem
import com.sedmelluq.discord.lavaplayer.track.AudioReference
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo
import com.sedmelluq.discord.lavaplayer.track.info.AudioTrackInfoBuilder
import org.apache.commons.io.IOUtils
import org.apache.http.HttpStatus
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern


class YouTube : AudioSourceManager, HttpAudioSourceManager() {

    override fun getSourceName() = "youtube"

    override fun loadItem(manager: DefaultAudioPlayerManager, reference: AudioReference): AudioItem? {
        if (!isValidIdentifier(reference.identifier)) {
            return null
        }

        val proc = ProcessBuilder("youtube-dl", "-ge", "--no-playlist", reference.identifier).start()

        if (!proc.waitFor(10, TimeUnit.SECONDS)) {
            return null
        }

        val out = IOUtils.toString(proc.inputStream, Charsets.UTF_8)
            .split("\n")
            .filter { it.isNotEmpty() }

        if (out.isEmpty()) {
            return null
        }

        val title = out.first()
        val playbackUrl = out.last()

        val ref = AudioReference(playbackUrl, title)

        return if (ref.containerDescriptor != null) {
            createTrack(AudioTrackInfoBuilder.create(ref, null).build(), ref.containerDescriptor)
        } else {
            val container = detectContainer(ref)
                ?: return null
            checkContainer(container)

            val data = container.trackInfo

            val trackInfo = AudioTrackInfo(title, data.author, data.length, data.identifier, data.isStream, data.uri)
            createTrack(trackInfo, container.containerDescriptor)
        }
    }

    private fun detectContainer(reference: AudioReference): MediaContainerDetectionResult? {
        var result: MediaContainerDetectionResult? = null

        try {
            httpInterface.use { httpInterface -> result = detectContainerWithClient(httpInterface, reference) }
        } catch (e: IOException) {
            throw FriendlyException("Connecting to the URL failed.", FriendlyException.Severity.SUSPICIOUS, e)
        }

        return result
    }

    @Throws(IOException::class)
    private fun detectContainerWithClient(httpInterface: HttpInterface, reference: AudioReference): MediaContainerDetectionResult? {
        try {
            PersistentHttpStream(httpInterface, URI(reference.identifier), java.lang.Long.MAX_VALUE).use { inputStream ->
                val statusCode = inputStream.checkStatusCode()
                val redirectUrl = HttpClientTools.getRedirectLocation(reference.identifier, inputStream.currentResponse)

                if (redirectUrl != null) {
                    return refer(null, AudioReference(redirectUrl, null))
                } else if (statusCode == HttpStatus.SC_NOT_FOUND) {
                    return null
                } else if (!HttpClientTools.isSuccessWithContent(statusCode)) {
                    throw FriendlyException("That URL is not playable.", FriendlyException.Severity.COMMON, IllegalStateException("Status code $statusCode"))
                }

                val hints = MediaContainerHints.from(getHeaderValue(inputStream.currentResponse, "Content-Type"), null)
                return MediaContainerDetection(containerRegistry, reference, inputStream, hints).detectContainer()
            }
        } catch (e: URISyntaxException) {
            throw FriendlyException("Not a valid URL.", FriendlyException.Severity.COMMON, e)
        }
    }

    fun checkContainer(result: MediaContainerDetectionResult) {
        if (!result.isContainerDetected) {
            throw FriendlyException("Unknown file format.", FriendlyException.Severity.COMMON, null)
        } else if (!result.isSupportedFile) {
            throw FriendlyException(result.unsupportedReason, FriendlyException.Severity.COMMON, null)
        }
    }

    fun isValidIdentifier(identifier: String): Boolean {
        if (identifier.startsWith("ytsearch:")) {
            return true
        }

        return validPatterns.any { it.matcher(identifier).matches() }
    }

    companion object {
        private val PROTOCOL_REGEX = "(?:https?://)"
        private val DOMAIN_REGEX = "(?:www\\.|m\\.|music\\.|)youtube\\.com"
        private val SHORT_DOMAIN_REGEX = "(?:www\\.|)youtu\\.be"
        private val VIDEO_ID_REGEX = "(?<v>[a-zA-Z0-9_-]{11})"

        private val validPatterns = listOf(
            Pattern.compile("^$PROTOCOL_REGEX$DOMAIN_REGEX/.*"),
            Pattern.compile("^$PROTOCOL_REGEX$SHORT_DOMAIN_REGEX/.*")
        )
    }

}
