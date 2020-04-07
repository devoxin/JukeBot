package jukebot.utils


import jukebot.Database
import jukebot.JukeBot
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.TextChannel
import org.apache.commons.io.IOUtils
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.stream.Collectors
import kotlin.math.floor
import kotlin.math.round

object Helpers {
    val version by lazy {
        val stream = Helpers::class.java.classLoader.getResourceAsStream("version.txt")
        IOUtils.toString(stream, Charsets.UTF_8)
    }
    private val timer: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor { Thread(it, "JukeBot-Timer") }
    val monitor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor { Thread(it, "JukeBot-Pledge-Monitor") }

    fun createBar(v: Int, max: Int, barLength: Int, bar: Char = '\u25AC', link: String = "https://jukebot.serux.pro"): String {
        val percent = v.toFloat() / max
        val blocks = floor((barLength * percent).toDouble()).toInt()

        return buildString {
            append("[")
            val hasTerminator = (0 until barLength).any { it == blocks }
            val segments = (0 until barLength).map<Int, Any> { if (it == blocks) "]($link)" else bar }

            for (segment in segments) {
                append(segment)
            }

            if (!hasTerminator) {
                append("]($link)")
            }
        }
    }

    fun pad(s: String): String {
        return String.format("%-12s", s).replace(" ", " \u200B")
    }

    fun canSendTo(channel: TextChannel): Boolean {
        return channel.canTalk() && channel.guild.selfMember.hasPermission(channel, Permission.MESSAGE_EMBED_LINKS)
    }

    fun schedule(task: () -> Unit, delay: Int, unit: TimeUnit) {
        timer.schedule(task, delay.toLong(), unit)
    }

    fun truncate(content: String, maxLength: Int): String {
        return if (content.length > maxLength) {
            content.substring(0, maxLength - 3) + "..."
        } else content
    }

    fun readFile(path: String, def: String): String {
        return try {
            Files.lines(Paths.get(path)).collect(Collectors.joining("\n"))
        } catch (e: Exception) {
            def
        }
    }

    fun monitorPledges() {
        JukeBot.LOG.info("Checking pledges...")

        JukeBot.patreonApi.fetchPledgesOfCampaign("750822").thenAccept { users ->
            if (users.isEmpty()) {
                return@thenAccept JukeBot.LOG.warn("Scheduled pledge clean failed: No users to check")
            }

            for (id in Database.getDonorIds()) {
                val pledge = users.firstOrNull { it.discordId != null && it.discordId == id }

                if (pledge == null || pledge.isDeclined) {
                    Database.setTier(id, 0)
                    Database.removePremiumServersOf(id)
                    JukeBot.LOG.info("Removed $id from donors")
                    continue
                }

                val amount = pledge.pledgeCents.toDouble() / 100
                val friendly = String.format("%1$,.2f", amount)
                val tier = Database.getTier(id)
                val calculatedTier = calculateTier(amount)

                if (tier != calculatedTier) {
                    if (calculatedTier < tier) {
                        val calculatedServerQuota = if (calculatedTier < 3) 0 else ((calculatedTier - 3) / 1) + 1
                        val allServers = Database.getPremiumServersOf(id)

                        if (allServers.size > calculatedServerQuota) {
                            JukeBot.LOG.info("Removing some of $id's premium servers to meet quota (quota: $calculatedServerQuota, servers: ${allServers.size}")
                            val exceededQuotaBy = allServers.size - calculatedServerQuota

                            for (i in 0..exceededQuotaBy) {
                                allServers[i].remove()
                            }
                        }
                    }
                    JukeBot.LOG.info("Adjusting $id's tier (saved: $tier, calculated: $calculatedTier, pledge: $$friendly)")
                    Database.setTier(id, calculatedTier)
                }
            }
        }
    }

    fun calculateTier(pledgeAmount: Double): Int {
        return when {
            pledgeAmount >= 3 -> round(pledgeAmount).toInt()
            pledgeAmount >= 2 -> 2
            pledgeAmount >= 1 -> 1
            else -> 0
        }
    }

    fun rootCauseOf(ex: Throwable): Throwable {
        val cause = ex.cause

        if (cause != null) {
            return rootCauseOf(cause)
        }

        return ex
    }

}
