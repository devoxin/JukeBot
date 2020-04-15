package jukebot.commands.misc

import com.sun.management.OperatingSystemMXBean
import jukebot.Database
import jukebot.JukeBot
import jukebot.audio.sourcemanagers.caching.CachingSourceManager
import jukebot.framework.Command
import jukebot.framework.CommandProperties
import jukebot.framework.Context
import jukebot.utils.toTimeString
import net.dv8tion.jda.api.JDA
import java.lang.management.ManagementFactory
import java.text.DecimalFormat
import kotlin.math.max

@CommandProperties(description = "Displays JukeBot statistics")
class Stats : Command(ExecutionType.STANDARD) {

    private val dpFormatter = DecimalFormat("0.00")

    override fun execute(context: Context) {
        val toSend = StringBuilder()
        val rUsedRaw = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        val rPercent = dpFormatter.format(rUsedRaw.toDouble() / Runtime.getRuntime().totalMemory() * 100)
        val usedMB = dpFormatter.format(rUsedRaw.toDouble() / 1048576)

        val players = JukeBot.players.size
        val playingPlayers = JukeBot.players.values.filter { it.isPlaying }.size
        val encodingPlayers = JukeBot.players.values.filter { it.isPlaying && (it.bassBooster.isEnabled || it.player.volume != 100) }.size

        val servers = JukeBot.shardManager.guildCache.size()
        val users = JukeBot.shardManager.guilds.sumBy { it.memberCount }

        val shards = JukeBot.shardManager.shardsTotal
        val shardsOnline = JukeBot.shardManager.shards.filter { s -> s.status == JDA.Status.CONNECTED }.size
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

        toSend.append("```asciidoc\n")
            .append("= JVM =\n")
            .append("Uptime          :: ").append((System.currentTimeMillis() - JukeBot.startTime).toTimeString()).append("\n")
            .append("JVM CPU Usage   :: ").append(procCpuUsage).append("%\n")
            .append("System CPU Usage:: ").append(sysCpuUsage).append("%\n")
            .append("RAM Usage       :: ").append(usedMB).append("MB (").append(rPercent).append("%)\n")
            .append("Threads         :: ").append(Thread.activeCount()).append("\n\n")
            .append("== JukeBot ==\n")
            .append("Guilds          :: ").append(servers).append("\n")
            .append("Users           :: ").append(users).append("\n")
            .append("Total Players   :: ").append(players).append("\n")
            .append("  Playing        : ").append(playingPlayers).append("\n")
            .append("  Encoding       : ").append(encodingPlayers).append("\n")
            .append("Queries         :: ").append(totalHits).append("\n")
            .append("  Cache Hits     : ").append(successfulHits).append(" ($pcCachedFormatted%)").append("\n")
            .append("Database Calls  :: ").append(Database.calls).append(" (").append(formattedCPS).append("/sec)").append("\n")
            .append("Shards Online   :: ").append(shardsOnline).append("/").append(shards).append("\n")
            .append("Average Latency :: ").append(averageShardLatency).append("ms\n")
            .append("```")

        context.channel.sendMessage(toSend.toString()).queue()
    }
}
