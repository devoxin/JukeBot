package me.devoxin.jukebot.patreon

import com.grack.nanojson.JsonObject
import io.sentry.Sentry
import me.devoxin.jukebot.Database
import me.devoxin.jukebot.JukeBot
import me.devoxin.jukebot.utils.Helpers
import me.devoxin.jukebot.utils.RequestUtil
import me.devoxin.jukebot.utils.json
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.URLDecoder
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class PatreonAPI(var accessToken: String) {
    private val monitor = Executors.newSingleThreadScheduledExecutor { Thread(it, "JukeBot-Pledge-Monitor") }

    init {
        monitor.scheduleAtFixedRate(::checkPledges, 0, 1, TimeUnit.DAYS)
    }

    private fun checkPledges() {
        logger.info("Checking pledges...")

        JukeBot.patreonApi.fetchPledgesOfCampaign("750822").thenAccept { users ->
            if (users.isEmpty()) {
                return@thenAccept logger.warn("Scheduled pledge clean failed: No users to check")
            }

            for (id in Database.getDonorIds()) {
                val pledge = users.firstOrNull { it.discordId != null && it.discordId == id }

                if (pledge == null || pledge.isDeclined) {
                    Database.setTier(id, 0)
                    Database.removePremiumServersOf(id)
                    logger.info("Removed $id from donors")
                    continue
                }

                val amount = pledge.pledgeCents.toDouble() / 100
                val friendly = String.format("%1$,.2f", amount)
                val tier = Database.getTier(id)
                val calculatedTier = Helpers.calculateTier(amount)

                if (tier != calculatedTier) {
                    if (calculatedTier < tier) {
                        val calculatedServerQuota = if (calculatedTier < 3) 0 else ((calculatedTier - 3) / 1) + 1
                        val allServers = Database.getPremiumServersOf(id)

                        if (allServers.size > calculatedServerQuota) {
                            logger.info("Removing some of $id's premium servers to meet quota (quota: $calculatedServerQuota, servers: ${allServers.size}")
                            val exceededQuotaBy = allServers.size - calculatedServerQuota
                            (0..exceededQuotaBy).onEach { allServers[it].remove() }
                        }
                    }
                    logger.info("Adjusting $id's tier (saved: $tier, calculated: $calculatedTier, pledge: $$friendly)")
                    Database.setTier(id, calculatedTier)
                }
            }
        }.exceptionally {
            Sentry.capture(it)
            return@exceptionally null
        }
    }

    fun fetchPledgesOfCampaign(campaignId: String): CompletableFuture<List<PatreonUser>> {
        val future = CompletableFuture<List<PatreonUser>>()
        getPageOfPledge(campaignId, cb = future::complete)
        return future
    }

    private fun getPageOfPledge(
        campaignId: String, offset: String? = null,
        users: MutableSet<PatreonUser> = mutableSetOf(), cb: (List<PatreonUser>) -> Unit
    ) {
        request {
            addPathSegments("campaigns/$campaignId/pledges")
            setQueryParameter("include", "pledge,patron")
            offset?.let { setQueryParameter("page[cursor]", it) }
        }.queue({
            if (!it.isSuccessful) {
                logger.error("Unable to get list of pledges ({}): {}", it.code, it.body?.string())
                it.close()

                return@queue cb(users.toList())
            }

            val json = it.json() ?: return@queue cb(users.toList())
            val pledges = json.getArray("data")

            json.getArray("included").forEachIndexed { index, user ->
                val obj = user as JsonObject

                if (obj.getString("type") == "user") {
                    val pledge = pledges.getObject(index)
                    users.add(PatreonUser.fromJsonObject(obj, pledge))
                }
            }

            val nextPage = getNextPage(json) ?: return@queue cb(users.toList())
            getPageOfPledge(campaignId, nextPage, users, cb)
        }, {
            logger.error("Unable to get list of pledges", it)
            return@queue cb(users.toList())
        })
    }

    private fun getNextPage(json: JsonObject): String? {
        val links = json.getObject("links")

        if (!links.has("next")) {
            return null
        }

        return parseQueryString(links.getString("next"))["page[cursor]"]
    }

    private fun parseQueryString(url: String): Map<String, String> {
        return URI(url).query
            .split("&")
            .map { it.split("=") }
            .associateBy({ decode(it[0]) }, { decode(it[1]) })
    }

    private fun decode(s: String) = URLDecoder.decode(s, Charsets.UTF_8)

    private fun request(urlOpts: HttpUrl.Builder.() -> Unit): RequestUtil.PendingRequest {
        val url = baseUrl.newBuilder().apply(urlOpts).build()
        return JukeBot.httpClient.request {
            url(url)
            header("Authorization", "Bearer $accessToken")
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(PatreonAPI::class.java)
        private val baseUrl = "https://www.patreon.com/api/oauth2/api".toHttpUrl()
    }
}
