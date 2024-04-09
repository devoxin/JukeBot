package me.devoxin.jukebot.integrations.patreon

import io.sentry.Sentry
import me.devoxin.jukebot.Database
import me.devoxin.jukebot.integrations.patreon.entities.Patron
import me.devoxin.jukebot.models.PremiumUser
import me.devoxin.jukebot.utils.HttpClient
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import org.json.JSONObject
import org.slf4j.LoggerFactory
import java.net.URI
import java.net.URLDecoder
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class PatreonAPI(private val accessToken: String) {
    private val httpClient = HttpClient()
    private val monitor = Executors.newSingleThreadScheduledExecutor { Thread(it, "JukeBot-Pledge-Monitor") }

    init {
        monitor.scheduleAtFixedRate(::sweep, 0, 1, TimeUnit.DAYS)
    }

    fun sweep(): CompletableFuture<SweepStats> {
        val currentPatrons = Database.getPatrons()

        return fetchPledges().thenApply { pledges ->
            val total = currentPatrons.size
            var changed = 0
            var removed = 0
            var fatal = 0

            for (patron in currentPatrons.filterNot(PremiumUser::override)) {
                try {
                    val userId = patron.id
                    val pledge = pledges.firstOrNull { it.discordUserId != null && it.discordUserId == userId }

                    if (pledge == null || pledge.isDeclined) {
                        patron.clearPremiumGuilds()
                        patron.remove()
                        removed++
                        continue
                    }

                    val pledging = pledge.entitledAmountCents

                    if (pledging != patron.pledgeAmountCents) {
                        patron.setPledgeAmount(pledging)

                        if (patron.tier < PatreonTier.SERVER) {
                            patron.clearPremiumGuilds()
                        }

//                        val entitledServers = patr.totalPremiumGuildQuota
//                        val activatedServers = entry.premiumGuildsList
//                        val exceedingLimitBy = activatedServers.size - entitledServers
//
//                        if (exceedingLimitBy > 0) {
//                            val remove = (0 until exceedingLimitBy)
//
//                            for (unused in remove) {
//                                activatedServers.firstOrNull()?.delete()
//                            }
//                        }

                        changed++
                    }
                } catch (e: Exception) {
                    Sentry.capture(e)
                    fatal++
                }
            }

            SweepStats(total, changed, removed, fatal)
        }
    }

    fun fetchPledges(campaignId: String = "750822") = fetchPledgesOfCampaign0(campaignId)

    private fun fetchPledgesOfCampaign0(campaignId: String): CompletableFuture<List<Patron>> {
        val initialUrl = baseUrl.newBuilder().apply {
            addPathSegments("/campaigns/$campaignId/members")
            setQueryParameter("include", "currently_entitled_tiers,user")
            setQueryParameter("fields[member]", "full_name,last_charge_date,last_charge_status,lifetime_support_cents,currently_entitled_amount_cents,patron_status,pledge_relationship_start")
            setQueryParameter("fields[user]", "social_connections")
            setQueryParameter("page[count]", "1000")
        }.build()

        return fetchPageOfPledgeRecursive(initialUrl, mutableListOf())
    }

    private fun fetchPageOfPledgeRecursive(url: HttpUrl, cache: MutableList<Patron>): CompletableFuture<List<Patron>> {
        return request { url(url) }.thenApply {
            val nextLink = getNextPage(it)
            val members = it.getJSONArray("data")
            val users = it.getJSONArray("included")
            val patrons = mutableListOf<Patron>()

            for (user in users) {
                val obj = user as JSONObject

                if (obj.getString("type") != "user") {
                    continue
                }

                val userId = obj.getString("id")
                val member = members.firstOrNull { m ->
                    val mObj = m as JSONObject
                    val userData = mObj.getJSONObject("relationships").getJSONObject("user").getJSONObject("data")
                    return@firstOrNull userData.getString("id") == userId
                }

                if (member != null) {
                    patrons.add(Patron.from(member as JSONObject, obj))
                }
            }

            cache.addAll(patrons)
            nextLink
        }.thenCompose {
            when {
                it != null -> fetchPageOfPledgeRecursive(it.toHttpUrl(), cache)
                else -> CompletableFuture.completedFuture(cache)
            }
        }
    }

    private fun getNextPage(json: JSONObject): String? {
        return json.optJSONObject("links")?.optString("next")
    }

    private fun request(requestOpts: Request.Builder.() -> Unit): CompletableFuture<JSONObject> {
        return httpClient.request {
            apply(requestOpts)
            header("Authorization", "Bearer $accessToken")
        }.submit().thenApply { it.body?.string()?.let(::JSONObject) }
    }

    companion object {
        private val log = LoggerFactory.getLogger(PatreonAPI::class.java)
        private val baseUrl = "https://www.patreon.com/api/oauth2/v2".toHttpUrl()
    }
}
