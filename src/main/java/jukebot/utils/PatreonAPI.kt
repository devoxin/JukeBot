package jukebot.utils

import jukebot.JukeBot
import okhttp3.OkHttpClient
import okhttp3.Request
import org.apache.http.client.utils.URIBuilder
import org.apache.http.client.utils.URLEncodedUtils
import org.json.JSONObject
import java.net.URI
import java.nio.charset.Charset
import java.util.concurrent.CompletableFuture

class PatreonAPI(private val accessToken: String) {
    private val httpClient = OkHttpClient()

    public fun fetchPledgesOfCampaign(campaignId: String, callback: CompletableFuture<List<PatreonUser>>) {
        callback.complete(getPageOfPledge(campaignId, null))
    }

    private fun getPageOfPledge(campaignId: String, offset: String?): List<PatreonUser> {
        val users = mutableSetOf<PatreonUser>()

        val url = URIBuilder("$BASE_URL/campaigns/$campaignId/pledges")

        url.addParameter("include", "pledge,patron")

        if (offset != null) {
            url.addParameter("page[cursor]", offset)
        }

        val request = Request.Builder()
                .addHeader("Authorization", "Bearer $accessToken")
                .url(url.build().toURL())
                .get()

        val response = httpClient.newCall(request.build()).execute()

        if (!response.isSuccessful) {
            JukeBot.LOG.error("Unable to get list of pledges (%d): %s", response.code(), response.message())
            return users.toList()
        }

        val json = response.json() ?: return users.toList()
        val pledges = json.getJSONArray("data")

        json.getJSONArray("included").forEachIndexed { index, user ->
            val obj = user as JSONObject

            if (obj.getString("type") == "user") {
                users.add(buildUser(obj, pledges.getJSONObject(index)))
            }
        }

        val nextPage = getNextPage(json) ?: return users.toList()

        users.addAll(getPageOfPledge(campaignId, nextPage))

        return users.toList()
    }

    private fun getNextPage(json: JSONObject): String? {
        val links = json.getJSONObject("links")

        if (!links.has("next")) {
            return null
        }

        val queryParams = URLEncodedUtils.parse(URI(links.getString("next")), Charset.forName("utf8"))
        return queryParams.firstOrNull { it.name == "page[cursor]" }?.value
    }

    private fun buildUser(user: JSONObject, pledge: JSONObject): PatreonUser {
        val userAttr = user.getJSONObject("attributes")
        val pledgeAttr = pledge.getJSONObject("attributes")

        val connections = userAttr.getJSONObject("social_connections")
        val discordId = if (!connections.isNull("discord")) {
            connections.getJSONObject("discord").getString("user_id")
        } else {
            null
        }

        return PatreonUser(
                userAttr.getString("first_name"),
                userAttr.getString("last_name"),
                userAttr.getString("email"),
                pledgeAttr.getInt("amount_cents"),
                !pledgeAttr.isNull("declined_since"),
                discordId
        )
    }

    companion object {
        private const val BASE_URL = "https://www.patreon.com/api/oauth2/api/"
    }
}


class PatreonUser(
        val firstName: String,
        val lastName: String,
        val email: String,
        val pledgeCents: Int,
        val isDeclined: Boolean,
        val discordId: String?
)
