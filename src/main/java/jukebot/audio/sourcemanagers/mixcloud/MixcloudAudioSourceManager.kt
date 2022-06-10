/*
   Copyright 2022 Devoxin

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

// ==========
// Basically, don't steal this and we won't have a problem.
// ==========

package jukebot.audio.sourcemanagers.mixcloud

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
import jukebot.audio.sourcemanagers.mixcloud.Utils.urlDecoded
import jukebot.audio.sourcemanagers.mixcloud.Utils.urlEncoded
import org.apache.commons.io.IOUtils
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.impl.client.HttpClientBuilder
import java.io.DataInput
import java.io.DataOutput
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.function.Consumer
import java.util.function.Function
import java.util.regex.Matcher
import java.util.regex.Pattern

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

            val content = IOUtils.toString(it.entity.content, StandardCharsets.UTF_8)
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

    fun getTrackInfo(uri: String): JsonBrowser? {
        makeHttpRequest(HttpGet(uri)).use {
            val statusCode = it.statusLine.statusCode

            if (statusCode != 200) {
                if (statusCode == 404) {
                    return null
                }
                throw IOException("Invalid status code for Mixcloud track page response: $statusCode")
            }

            val content = IOUtils.toString(it.entity.content, StandardCharsets.UTF_8)

            val matcher = JSON_REGEX.matcher(content)
            check(matcher.find()) { "Missing MixCloud track JSON" }

            val json = JsonBrowser.parse(matcher.group(1).replace("&quot;", "\""))

            for (node in json.values()) {
                val info = node.get("cloudcastLookup").get("data").get("cloudcastLookup")

                if (!info.isNull && !info.get("streamInfo").isNull) {
                    val jsMatcher = JS_REGEX.matcher(content)

                    if (jsMatcher.find()) {
                        info.put("jsUrl", jsMatcher.group(1))
                    }

                    return info
                }
            }

            throw IllegalStateException("No nodes found matching path cloudcastLookup.data.cloudcastLookup")
        }
    }

    fun getStreamKey(json: JsonBrowser): String {
        check(!json.get("jsUrl").isNull) { "jsUrl is missing from json" }

        makeHttpRequest(HttpGet(json.get("jsUrl").text())).use {
            check(it.statusLine.statusCode == 200) { "Invalid status code while fetching JS" }

            val content = IOUtils.toString(it.entity.content, StandardCharsets.UTF_8)
            val keyMatcher = KEY_REGEX.matcher(content)

            if (keyMatcher.find()) {
                return keyMatcher.group(1) + keyMatcher.group(2)
            }

            throw IllegalStateException("Missing key in JS")
        }
    }

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
        private val URL_REGEX =
            Pattern.compile("https?://(?:(?:www|beta|m)\\.)?mixcloud\\.com/([^/]+)/(?!stream|uploads|favorites|listens|playlists)([^/]+)/?")
        private val JSON_REGEX = Pattern.compile("<script id=\"relay-data\" type=\"text/x-mixcloud\">([^<]+)</script>")
        private val JS_REGEX =
            Pattern.compile("<script[^>]+src=\"(https://(?:www\\.)?mixcloud\\.com/media/(?:js2/www_js_4|js/www)\\.[^>]+\\.js)")
        private val KEY_REGEX = Pattern.compile("\\{return *?[\"']([^\"']+)[\"']\\.concat\\([\"']([^\"']+)[\"']\\)}")

        internal const val DECRYPTION_KEY = "IFYOUWANTTHEARTISTSTOGETPAIDDONOTDOWNLOADFROMMIXCLOUD"
    }
}
