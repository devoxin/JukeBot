package jukebot

import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.CompletableFuture

class YouTubeAPI(private val key: String) {

    private val httpClient = OkHttpClient()

    public fun searchVideo(query: String, callback: (String?) -> Unit) {
        val request = Request.Builder()
                .url("https://www.googleapis.com/youtube/v3/search?q=$query&key=$key&type=video&maxResults=3&part=id")
                .addHeader("User-Agent", "JukeBot/v6.2 (https://www.google.co.uk)")
                .get()
                .build()

        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                callback(null)
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body()

                if (body == null) {
                    callback(null)
                } else {
                    val json = JSONObject(body.string())
                    val results = json.getJSONArray("items")

                    if (results.length() == 0) {
                        callback(null)
                    } else {
                        callback(results.getJSONObject(0).getJSONObject("id").getString("videoId"))
                    }
                }
            }
        })
    }

}