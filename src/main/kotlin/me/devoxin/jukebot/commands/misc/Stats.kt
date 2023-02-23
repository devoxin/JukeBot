package me.devoxin.jukebot.commands.misc

import com.sun.management.OperatingSystemMXBean
import me.devoxin.jukebot.Database
import me.devoxin.jukebot.JukeBot
import me.devoxin.jukebot.audio.sourcemanagers.caching.CachingSourceManager
import me.devoxin.jukebot.framework.Command
import me.devoxin.jukebot.framework.CommandProperties
import me.devoxin.jukebot.framework.Context
import me.devoxin.jukebot.utils.toTimeString
import net.dv8tion.jda.api.JDA
import java.lang.management.ManagementFactory
import java.text.DecimalFormat
import kotlin.math.max

@CommandProperties(description = "Displays statistics about the bot")
class Stats : Command(ExecutionType.STANDARD) {
    private val dpFormatter = DecimalFormat("0.00")

    override fun execute(context: Context) {
        val rUsedRaw = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        val rPercent = dpFormatter.format(rUsedRaw.toDouble() / Runtime.getRuntime().totalMemory() * 100)
        val usedMB = dpFormatter.format(rUsedRaw.toDouble() / 1048576)

        val players = JukeBot.players.size
        val playingPlayers = JukeBot.players.values.count { it.isPlaying }
        val encodingPlayers = JukeBot.players.values.count {
                it.isPlaying && (it.bassBooster.isEnabled ||
                    it.player.volume != 100 ||
                    (it.player.playingTrack?.sourceManager?.sourceName?.let { name -> name !in OPUS_SOURCES) } ?: false)
        }

        val servers = JukeBot.shardManager.guildCache.size()
        val users = JukeBot.shardManager.guilds.sumOf { it.memberCount }

        val shards = JukeBot.shardManager.shardsTotal
        val shardsOnline = JukeBot.shardManager.shards.count { s -> s.status == JDA.Status.CONNECTED }
        val averageShardLatency = JukeBot.shardManager.averageGatewayPing.toInt()

        val osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean::class.java)
        val procCpuUsage = dpFormatter.format(osBean.processCpuLoad * 100)
        val sysCpuUsage = dpFormatter.format(osBean.systemCpuLoad * 100)

        val secondsSinceBoot = ((System.currentTimeMillis() - JukeBot.startTime) / 1000).toDouble()
        val callsPerSecond = Database.calls / secondsSinceBoot
        val formattedCPS = dpFormatter.format(callsPerSecond)

        val totalHits = CachingSourceManager.totalHits
        val successfulHits = CachingSourceManager.successfulHits
        val pcCached = successfulHits.toDouble() / max(1, totalHits).toDouble()
        val pcCachedFormatted = dpFormatter.format(pcCached * 100)

        context.channel.sendMessage(buildString {
            append("```asciidoc\n")
            append("= JVM =\n")
            append("Uptime          :: ").append((System.currentTimeMillis() - JukeBot.startTime).toTimeString())
                .append("\n")
            append("JVM CPU Usage   :: ").append(procCpuUsage).append("%\n")
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
            append("Database Calls  :: ").append(Database.calls).append(" (").append(formattedCPS).append("/sec)")
                .append("\n")
            append("Shards Online   :: ").append(shardsOnline).append("/").append(shards).append("\n")
            append("Average Latency :: ").append(averageShardLatency).append("ms\n")
            append("```")
        }).queue()
    }

    companion object {
        private val OPUS_SOURCES = setOf("youtube", "soundcloud")
    }
}
