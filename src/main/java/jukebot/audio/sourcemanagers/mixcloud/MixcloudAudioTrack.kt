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

import com.sedmelluq.discord.lavaplayer.container.mpeg.MpegAudioTrack
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManager
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.tools.io.HttpInterface
import com.sedmelluq.discord.lavaplayer.tools.io.PersistentHttpStream
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackInfo
import com.sedmelluq.discord.lavaplayer.track.DelegatedAudioTrack
import com.sedmelluq.discord.lavaplayer.track.playback.LocalAudioTrackExecutor
import jukebot.audio.sourcemanagers.mixcloud.Utils.urlDecoded
import java.net.URI

class MixcloudAudioTrack(trackInfo: AudioTrackInfo, private val sourceManager: MixcloudAudioSourceManager) :
    DelegatedAudioTrack(trackInfo) {
    override fun makeClone(): AudioTrack {
        return MixcloudAudioTrack(trackInfo, sourceManager)
    }

    override fun getSourceManager(): AudioSourceManager {
        return sourceManager
    }

    @Throws(Exception::class)
    override fun process(localExecutor: LocalAudioTrackExecutor) {
        sourceManager.httpInterfaceManager.`interface`.use {
            processStatic(localExecutor, it)
        }
    }

    @Throws(Exception::class)
    private fun processStatic(localExecutor: LocalAudioTrackExecutor, httpInterface: HttpInterface) {
        val playbackUrl = getPlaybackUrl()

        PersistentHttpStream(httpInterface, URI(playbackUrl), Long.MAX_VALUE).use { stream ->
            processDelegate(MpegAudioTrack(trackInfo, stream), localExecutor)
        }
    }

    private fun getPlaybackUrl(): String {
        val json = sourceManager.extractTrackInfoGraphQl(trackInfo.author, trackInfo.identifier.urlDecoded())
            ?: throw FriendlyException("This track is unplayable", FriendlyException.Severity.SUSPICIOUS, null)

        val streamInfo = json.get("streamInfo")
        val mp4Url = streamInfo.get("url").text()

        return Utils.decryptUrl(MixcloudAudioSourceManager.DECRYPTION_KEY, mp4Url)
    }
}
