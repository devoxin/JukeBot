package jukebot.utils


import jukebot.Database
import jukebot.JukeBot
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.TextChannel
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.stream.Collectors
import kotlin.math.floor

object Helpers {
    private val timer = Executors.newSingleThreadScheduledExecutor { Thread(it, "JukeBot-Timer") }!!
    val monitor = Executors.newSingleThreadScheduledExecutor { Thread(it, "JukeBot-Pledge-Monitor") }!!

    fun createBar(v: Int, max: Int, barLength: Int, bar: Char = '\u25AC'): String {
        val percent = v.toFloat() / max
        val blocks = floor((barLength * percent).toDouble()).toInt()

        val sb = StringBuilder("[")

        for (i in 0 until barLength) {
            if (i == blocks) {
                sb.append("](http://jukebot.serux.pro)")
            }

            sb.append(bar)
        }

        if (blocks == barLength) {
            sb.append("](http://jukebot.serux.pro)")
        }

        return sb.toString()
    }

    fun parseNumber(num: String?, def: Int): Int {
        return num?.toIntOrNull() ?: def
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

    fun dm(userId: String, message: String) {
        val user = JukeBot.shardManager.getUserById(userId) ?: return

        user.openPrivateChannel().queue {
            it.sendMessage(message)
                    .submit()
                    .handle { _, _ -> it.close().queue() }
        }
    }

    fun monitorPledges() {
        JukeBot.LOG.info("Checking pledges...")

        JukeBot.patreonApi.fetchPledgesOfCampaign("750822").thenAccept { users ->
            if (users.isEmpty()) {
                dm("180093157554388993", "⚠  |  Unable to check pledges. Ensure key is valid!")
                return@thenAccept JukeBot.LOG.warn("Scheduled pledge clean failed: No users to check")
            }

            Database.getDonorIds().forEach { id ->
                val pledge = users.firstOrNull { it.discordId != null && it.discordId == id }

                if (pledge == null || pledge.isDeclined) {
                    Database.setTier(id, 0)
                    JukeBot.LOG.info("Removed $id from donors")
                } else {
                    val amount = pledge.pledgeCents.toDouble() / 100
                    val friendly = String.format("%1$,.2f", amount)
                    val tier = Database.getTier(id)
                    val calculatedTier = calculateTier(amount)

                    if (tier != calculatedTier) {
                        JukeBot.LOG.info("Adjusting $id's tier (saved: $tier, calculated: $calculatedTier, pledge: $$friendly)")
                        Database.setTier(id, calculatedTier)
                    }
                }
            }
        }
    }

    fun calculateTier(pledgeAmount: Double): Int {
        return when {
            pledgeAmount >= 1 && pledgeAmount < 2 -> 1
            pledgeAmount >= 2 -> 2
            else -> 0
        }
    }
}
