package jukebot.audio.sourcemanagers.pornhub

import com.sedmelluq.discord.lavaplayer.container.mpeg.MpegAudioTrack
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.tools.JsonBrowser
import com.sedmelluq.discord.lavaplayer.tools.io.HttpClientTools
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface
import com.sedmelluq.discord.lavaplayer.tools.io.PersistentHttpStream
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo
import com.sedmelluq.discord.lavaplayer.track.DelegatedAudioTrack
import com.sedmelluq.discord.lavaplayer.track.playback.LocalAudioTrackExecutor
import org.apache.commons.io.IOUtils
import org.apache.http.client.methods.HttpGet
import java.net.URI
import java.nio.charset.StandardCharsets
import java.util.regex.Pattern

class PornHubAudioTrack(trackInfo: AudioTrackInfo, private val sourceManager: PornHubAudioSourceManager) : DelegatedAudioTrack(trackInfo) {

    override fun makeClone() = PornHubAudioTrack(trackInfo, sourceManager)

    override fun getSourceManager() = sourceManager

    @Throws(Exception::class)
    override fun process(localExecutor: LocalAudioTrackExecutor) {
        sourceManager.httpInterfaceManager.`interface`.use {
            processStatic(localExecutor, it)
        }
    }

    @Throws(Exception::class)
    private fun processStatic(localExecutor: LocalAudioTrackExecutor, httpInterface: HttpInterface) {
        val playbackUrl = getPlaybackUrl(httpInterface)

        PersistentHttpStream(httpInterface, URI(playbackUrl), Long.MAX_VALUE).use { stream ->
            processDelegate(MpegAudioTrack(trackInfo, stream), localExecutor)
        }
    }

    private fun getPlaybackUrl(httpInterface: HttpInterface): String {
        val info = getPageConfig(httpInterface)
            ?: throw FriendlyException("Failed to extract config from page", FriendlyException.Severity.SUSPICIOUS, null)

        return info.get("mediaDefinitions").values().stream()
            .filter { format -> format.get("videoUrl").text().isNotEmpty() }
            .findFirst()
            .orElseThrow { FriendlyException("No available stream formats", FriendlyException.Severity.SUSPICIOUS, null) }
            .get("videoUrl")
            .text()
    }

    private fun getPageConfig(httpInterface: HttpInterface): JsonBrowser? {
        httpInterface.execute(HttpGet(trackInfo.uri)).use { response ->
            val statusCode = response.statusLine.statusCode

            if (!HttpClientTools.isSuccessWithContent(statusCode)) {
                return null
            }

            val html = IOUtils.toString(response.entity.content, StandardCharsets.UTF_8)
            val match = VIDEO_INFO_REGEX.matcher(html)

            return if (match.find()) JsonBrowser.parse(match.group(1)) else null
        }
    }

    companion object {
        private val VIDEO_INFO_REGEX = Pattern.compile("var flashvars_\\d{7,9} = (\\{.+})")
    }

}