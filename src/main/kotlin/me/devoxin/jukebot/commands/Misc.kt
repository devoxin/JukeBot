package me.devoxin.jukebot.commands

import com.sedmelluq.discord.lavaplayer.tools.PlayerLibrary
import com.sun.management.OperatingSystemMXBean
import me.devoxin.flight.FlightInfo
import me.devoxin.flight.api.annotations.Command
import me.devoxin.flight.api.context.Context
import me.devoxin.flight.api.entities.Cog
import me.devoxin.jukebot.Database
import me.devoxin.jukebot.Launcher
import me.devoxin.jukebot.audio.sources.caching.CachingSourceManager
import me.devoxin.jukebot.extensions.embed
import me.devoxin.jukebot.extensions.toTimeString
import me.devoxin.jukebot.utils.Constants
import me.devoxin.jukebot.utils.Helpers
import net.dv8tion.jda.api.JDA
import net.dv8tion.jda.api.JDAInfo
import java.lang.management.ManagementFactory
import java.text.DecimalFormat
import kotlin.math.max

class Misc : Cog {
    override fun name() = "Misc"

    @Command(aliases = ["info"], description = "Displays some information about the bot.")
    fun about(ctx: Context) {
        val commitHash = Helpers.version
        val commitUrl = "https://github.com/devoxin/JukeBot/commit/$commitHash"

        ctx.embed {
            setTitle("JukeBot #$commitHash", commitUrl)
            addField("Using...", "JDA ${JDAInfo.VERSION}\nLavaplayer ${PlayerLibrary.VERSION}\nFlight ${FlightInfo.VERSION}", false)
            addField("\u200b", "JukeBot is [open-source software](https://github.com/devoxin/JukeBot) licensed under Apache-2.0.", false)
        }
    }

    @Command(description = "Displays the invite link for the bot.")
    fun invite(ctx: Context) {
        ctx.embed(
            "Invite Links",
            "[**Add ${Constants.BOT_NAME}**](${Constants.DEFAULT_INVITE_URL})\n[**Get Support**](${Constants.HOME_SERVER})"
        )
    }

    @Command(description = "Displays statistics for the bot.")
    fun stats(ctx: Context) {
        val rUsedRaw = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        val rPercent = dpFormatter.format(rUsedRaw.toDouble() / Runtime.getRuntime().totalMemory() * 100)
        val usedMB = dpFormatter.format(rUsedRaw.toDouble() / 1048576)

        val players = Launcher.playerManager.players.size
        val playingPlayers = Launcher.playerManager.players.values.count { it.isPlaying }
        val encodingPlayers = Launcher.playerManager.players.values.count { it.isPlaying && it.isEncoding }

        val servers = Launcher.shardManager.guildCache.size()
        val users = Launcher.shardManager.guilds.sumOf { it.memberCount }

        val shards = Launcher.shardManager.shardsTotal
        val shardsOnline = Launcher.shardManager.shards.count { s -> s.status == JDA.Status.CONNECTED }
        val averageShardLatency = Launcher.shardManager.averageGatewayPing.toInt()

        val osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean::class.java)
        val processCpuUsage = dpFormatter.format(osBean.processCpuLoad * 100)
        val sysCpuUsage = dpFormatter.format(osBean.systemCpuLoad * 100)

        val secondsSinceBoot = ((System.currentTimeMillis() - Launcher.launchTime) / 1000).toDouble()
        val callsPerSecond = Database.calls / secondsSinceBoot
        val formattedCPS = dpFormatter.format(callsPerSecond)

        val totalHits = CachingSourceManager.totalHits
        val successfulHits = CachingSourceManager.successfulHits
        val pcCached = successfulHits.toDouble() / max(1, totalHits).toDouble()
        val pcCachedFormatted = dpFormatter.format(pcCached * 100)

        ctx.respond(buildString {
            append("```asciidoc\n")
            append("= JVM =\n")
            append("Uptime          :: ").append((System.currentTimeMillis() - Launcher.launchTime).toTimeString()).append("\n")
            append("JVM CPU Usage   :: ").append(processCpuUsage).append("%\n")
            append("System CPU Usage:: ").append(sysCpuUsage).append("%\n")
            append("RAM Usage       :: ").append(usedMB).append("MB (").append(rPercent).append("%)\n")
            append("Threads         :: ").append(Thread.activeCount()).append("\n\n")
            append("== JukeBot ==\n")
            append("Guilds          :: ").append(servers).append("\n")
            append("Users           :: ").append(users).append("\n")
            append("Total Players   :: ").append(players).append("\n")
            append("  Playing        : ").append(playingPlayers).append("\n")
            append("  Encoding       : ").append(encodingPlayers).append("\n")
            append("Queries         :: ").append(totalHits).append("\n")
            append("  Cache Hits     : ").append(successfulHits).append(" ($pcCachedFormatted%)").append("\n")
            append("Database Calls  :: ").append(Database.calls).append(" (").append(formattedCPS).append("/sec)").append("\n")
            append("Shards Online   :: ").append(shardsOnline).append("/").append(shards).append("\n")
            append("Average Latency :: ").append(averageShardLatency).append("ms\n")
            append("```")
        })
    }

    companion object {
        private val dpFormatter = DecimalFormat("0.00")
    }
}
