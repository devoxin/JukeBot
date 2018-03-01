package jukebot.audioutilities

import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager
import com.sedmelluq.discord.lavaplayer.tools.ExceptionTools
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.tools.JsonBrowser
import com.sedmelluq.discord.lavaplayer.tools.io.HttpClientTools
import com.sedmelluq.discord.lavaplayer.tools.io.HttpConfigurable
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterfaceManager
import com.sedmelluq.discord.lavaplayer.track.AudioItem
import com.sedmelluq.discord.lavaplayer.track.AudioReference
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo
import org.apache.commons.io.IOUtils
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClientBuilder
import java.io.DataInput
import java.io.DataOutput
import java.io.IOException
import java.nio.charset.Charset
import java.util.function.Consumer
import java.util.function.Function
import java.util.regex.Pattern

import com.sedmelluq.discord.lavaplayer.tools.FriendlyException.Severity.FAULT


class PornHubAudioSourceManager : AudioSourceManager, HttpConfigurable {
    private val httpInterfaceManager: HttpInterfaceManager = HttpClientTools.createDefaultThreadLocalManager()

    val httpInterface: HttpInterface
        get() = httpInterfaceManager.`interface`

    override fun getSourceName(): String {
        return "pornhub"
    }

    override fun loadItem(manager: DefaultAudioPlayerManager, reference: AudioReference): AudioItem? {
        if (!VIDEO_REGEX.matcher(reference.identifier).matches())
            return null

        return try {
            loadItemOnce(reference)
        } catch (exception: FriendlyException) {
            // In case of a connection reset exception, try once more.
            if (HttpClientTools.isRetriableNetworkException(exception.cause)) {
                loadItemOnce(reference)
            } else {
                throw exception
            }
        }

    }

    override fun isTrackEncodable(track: AudioTrack): Boolean {
        return true
    }

    override fun encodeTrack(track: AudioTrack, output: DataOutput) {
        // No custom values that need saving
    }

    override fun decodeTrack(trackInfo: AudioTrackInfo, input: DataInput): AudioTrack {
        return PornHubAudioTrack(trackInfo, this)
    }

    override fun shutdown() {
        try {
            httpInterface.close()
        } catch (ignored: IOException) {}
    }

    override fun configureRequests(configurator: Function<RequestConfig, RequestConfig>) {
        httpInterfaceManager.configureRequests(configurator)
    }

    override fun configureBuilder(configurator: Consumer<HttpClientBuilder>) {
        httpInterfaceManager.configureBuilder(configurator)
    }

    private fun loadItemOnce(reference: AudioReference): AudioItem {
        try {
            httpInterface.use { httpInterface ->
                val info = getVideoInfo(httpInterface, reference.identifier) ?: return AudioReference.NO_TRACK

                val playbackURL = info.get("mediaDefinitions").values().stream()
                        .filter { format -> format.get("videoUrl").text().isNotEmpty() }
                        .findFirst()
                        .orElse(null)!!.get("videoUrl").text() ?: return AudioReference.NO_TRACK

                val videoTitle = info.get("video_title").text()
                val videoDuration = Integer.parseInt(info.get("video_duration").text()) * 1000 // PH returns seconds

                return buildTrackObject(reference.identifier, playbackURL, videoTitle, "Unknown Uploader", false, videoDuration.toLong())
            }
        } catch (e: Exception) {
            throw ExceptionTools.wrapUnfriendlyExceptions("Loading information for a PornHub track failed.", FAULT, e)
        }

    }

    @Throws(IOException::class)
    private fun getVideoInfo(httpInterface: HttpInterface, videoURL: String): JsonBrowser? {
        httpInterface.execute(HttpGet(videoURL)).use { response ->
            val statusCode = response.statusLine.statusCode
            if (statusCode != 200) {
                throw IOException("Invalid status code for video page response: " + statusCode)
            }

            val html = IOUtils.toString(response.entity.content, Charset.forName(CHARSET))
            val match = VIDEO_INFO_REGEX.matcher(html)

            return if (match.find()) JsonBrowser.parse(match.group(1)) else null
        }
    }

    private fun buildTrackObject(uri: String, identifier: String, title: String, uploader: String, isStream: Boolean, duration: Long): PornHubAudioTrack {
        return PornHubAudioTrack(AudioTrackInfo(title, uploader, duration, identifier, isStream, uri), this)
    }

    companion object {
        private const val CHARSET = "UTF-8"
        private val VIDEO_REGEX = Pattern.compile("^https?://www.pornhub.com/view_video.php\\?viewkey=[a-zA-Z0-9]{10,15}$")
        private val VIDEO_INFO_REGEX = Pattern.compile("var flashvars_\\d{7,9} = (\\{.+})")
    }

}
