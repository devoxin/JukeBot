package jukebot.apis.ksoft

import com.grack.nanojson.JsonObject

class TrackRecommendation(
    val id: String,
    val url: String,
    val title: String,
    val thumbnail: String,
    val description: String
) {
    companion object {
        fun fromJsonObject(jsonObject: JsonObject): TrackRecommendation {
            val id = jsonObject.getString("id")
            val link = jsonObject.getString("link")
            val title = jsonObject.getString("title")
            val thumbnail = jsonObject.getString("thumbnail")
            val description = jsonObject.getString("description")

            return TrackRecommendation(id, link, title, thumbnail, description)
        }
    }
}
