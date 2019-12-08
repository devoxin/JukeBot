package jukebot.audio.sourcemanagers.youtube

import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeHttpContextFilter
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.client.protocol.HttpClientContext

class YoutubeCvFilter : YoutubeHttpContextFilter() {

    override fun onRequest(context: HttpClientContext, request: HttpUriRequest, isRepetition: Boolean) {
        super.onRequest(context, request, isRepetition)

        request.setHeader("x-youtube-client-version", "2.20191206.06.00")
    }

}
