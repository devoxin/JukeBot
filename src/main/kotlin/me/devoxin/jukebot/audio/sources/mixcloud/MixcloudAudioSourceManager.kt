/*
   Copyright 2023 Devoxin

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

package me.devoxin.jukebot.audio.sources.mixcloud

import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager
import com.sedmelluq.discord.lavaplayer.tools.ExceptionTools
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.tools.JsonBrowser
import com.sedmelluq.discord.lavaplayer.tools.io.HttpClientTools
import com.sedmelluq.discord.lavaplayer.tools.io.HttpConfigurable
import com.sedmelluq.discord.lavaplayer.track.AudioItem
import com.sedmelluq.discord.lavaplayer.track.AudioReference
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo
import me.devoxin.jukebot.audio.sources.mixcloud.Utils.urlDecoded
import me.devoxin.jukebot.audio.sources.mixcloud.Utils.urlEncoded
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.util.EntityUtils
import java.io.DataInput
import java.io.DataOutput
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.function.Consumer
import java.util.function.Function
import java.util.regex.Matcher

class MixcloudAudioSourceManager : AudioSourceManager, HttpConfigurable {
    val httpInterfaceManager = HttpClientTools.createDefaultThreadLocalManager()!!

    override fun getSourceName() = "mixcloud"

    override fun loadItem(manager: AudioPlayerManager, reference: AudioReference): AudioItem? {
        val matcher = URL_REGEX.matcher(reference.identifier)

        if (!matcher.matches()) {
            return null
            // Null when URL not recognised, AudioReference.NO_TRACK if URL recognised but no track.
        }

        return try {
            loadItemOnce(reference, matcher)
        } catch (exception: FriendlyException) {
            // In case of a connection reset exception, try once more.
            if (HttpClientTools.isRetriableNetworkException(exception.cause)) {
                loadItemOnce(reference, matcher)
            } else {
                throw exception
            }
        }
    }

    override fun isTrackEncodable(track: AudioTrack) = true

    override fun encodeTrack(track: AudioTrack, output: DataOutput) {
    }

    override fun decodeTrack(trackInfo: AudioTrackInfo, input: DataInput) = MixcloudAudioTrack(trackInfo, this)

    override fun shutdown() {
        httpInterfaceManager.close()
    }

    override fun configureRequests(configurator: Function<RequestConfig, RequestConfig>) {
        httpInterfaceManager.configureRequests(configurator)
    }

    override fun configureBuilder(configurator: Consumer<HttpClientBuilder>) {
        httpInterfaceManager.configureBuilder(configurator)
    }

    private fun loadItemOnce(reference: AudioReference, matcher: Matcher): AudioItem? {
        try {
            val username = matcher.group(1).urlDecoded()
            val slug = matcher.group(2).urlDecoded()
            val trackInfo = extractTrackInfoGraphQl(username, slug)
                ?: return AudioReference.NO_TRACK
            //val trackInfo = getTrackInfo(reference.identifier) ?: return AudioReference.NO_TRACK

//            if ("false".equals(trackInfo.get("isPlayable").text(), true)) {
//                throw FriendlyException(trackInfo.get("restrictedReason").text(), FriendlyException.Severity.COMMON, null)
//            }

            val url = reference.identifier
            //val id = trackInfo.get("slug").text()
            val id = matcher.group(2)
            val title = trackInfo.get("name").text()
            val duration = trackInfo.get("audioLength").`as`(Long::class.java) * 1000
            val uploader = trackInfo.get("owner").get("username").text() // displayName

            return buildTrackObject(url, id, title, uploader, false, duration)
        } catch (e: Exception) {
            throw ExceptionTools.wrapUnfriendlyExceptions(
                "Loading information for a MixCloud track failed.",
                FriendlyException.Severity.FAULT,
                e
            )
        }
    }

    internal fun extractTrackInfoGraphQl(username: String, slug: String? = null): JsonBrowser? {
        val slugFormatted = if (slug != null) String.format(", slug: \"%s\"", slug) else ""
        val query = String.format(
            "{\n  cloudcastLookup(lookup: {username: \"%s\"%s}) {\n    %s\n  }\n}",
            username,
            slugFormatted,
            requestStructure
        )
        val encodedQuery = query.urlEncoded()

        makeHttpRequest(HttpGet("https://www.mixcloud.com/graphql?query=$encodedQuery")).use {
            val statusCode = it.statusLine.statusCode

            if (statusCode != 200) {
                if (statusCode == 404) {
                    return null
                }
                throw IOException("Invalid status code for Mixcloud track page response: $statusCode")
            }

            val content = EntityUtils.toString(it.entity, StandardCharsets.UTF_8)
            val json = JsonBrowser.parse(content).get("data").get("cloudcastLookup")

            if (!json.get("streamInfo").isNull) {
                return json
            }

            return null
        }
    }

    private val requestStructure = """audioLength
    name
    owner {
      username
    }
    streamInfo {
      dashUrl
      hlsUrl
      url
    }
    """.trim()

    private fun buildTrackObject(
        uri: String,
        identifier: String,
        title: String,
        uploader: String,
        isStream: Boolean,
        duration: Long
    ): MixcloudAudioTrack {
        return MixcloudAudioTrack(AudioTrackInfo(title, uploader, duration, identifier, isStream, uri), this)
    }

    private fun makeHttpRequest(request: HttpUriRequest): CloseableHttpResponse {
        return httpInterfaceManager.`interface`.use {
            it.execute(request)
        }
    }

    companion object {
        private val URL_REGEX = "https?://(?:(?:www|beta|m)\\.)?mixcloud\\.com/([^/]+)/(?!stream|uploads|favorites|listens|playlists)([^/]+)/?".toPattern()
        private val JSON_REGEX = "<script id=\"relay-data\" type=\"text/x-mixcloud\">([^<]+)</script>".toPattern()
        private val JS_REGEX = "<script[^>]+src=\"(https://(?:www\\.)?mixcloud\\.com/media/(?:js2/www_js_4|js/www)\\.[^>]+\\.js)".toPattern()
        private val KEY_REGEX = "\\{return *?[\"']([^\"']+)[\"']\\.concat\\([\"']([^\"']+)[\"']\\)}".toPattern()

        internal const val DECRYPTION_KEY = "IFYOUWANTTHEARTISTSTOGETPAIDDONOTDOWNLOADFROMMIXCLOUD"
    }
}
