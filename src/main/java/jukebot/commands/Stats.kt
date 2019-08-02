package jukebot.commands

import com.sun.management.OperatingSystemMXBean
import jukebot.Database
import jukebot.JukeBot
import jukebot.utils.Command
import jukebot.utils.CommandProperties
import jukebot.utils.Context
import jukebot.utils.toTimeString
import net.dv8tion.jda.api.JDA
import java.lang.management.ManagementFactory
import java.text.DecimalFormat

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
        val users = JukeBot.shardManager.userCache.size()

        val shards = JukeBot.shardManager.shardsTotal
        val shardsOnline = JukeBot.shardManager.shards.filter { s -> s.status == JDA.Status.CONNECTED }.size
        val averageShardLatency = JukeBot.shardManager.averageGatewayPing.toInt()

        val osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean::class.java)
        val procCpuUsage = dpFormatter.format(osBean.processCpuLoad * 100)
        val sysCpuUsage = dpFormatter.format(osBean.systemCpuLoad * 100)

        val secondsSinceBoot = ((System.currentTimeMillis() - JukeBot.startTime) / 1000).toDouble()
        val callsPerSecond = Database.calls / secondsSinceBoot
        val formattedCPS = dpFormatter.format(callsPerSecond)

        toSend.append("```ini\n")
                .append("[ JVM ]\n")
                .append("Uptime          = ").append((System.currentTimeMillis() - JukeBot.startTime).toTimeString()).append("\n")
                .append("JVM_CPU_Usage   = ").append(procCpuUsage).append("%\n")
                .append("System_CPU_Usage= ").append(sysCpuUsage).append("%\n")
                .append("RAM_Usage       = ").append(usedMB).append("MB (").append(rPercent).append("%)\n")
                .append("Threads         = ").append(Thread.activeCount()).append("\n\n")
                .append("[ JukeBot ]\n")
                .append("Guilds          = ").append(servers).append("\n")
                .append("Users           = ").append(users).append("\n")
                .append("Total_Players   = ").append(players).append("\n")
                .append("  Playing       = ").append(playingPlayers).append("\n")
                .append("  Encoding      = ").append(encodingPlayers).append("\n")
                .append("Database_Calls  = ").append(Database.calls).append(" (").append(formattedCPS).append("/sec)").append("\n")
                .append("Shards_Online   = ").append(shardsOnline).append("/").append(shards).append("\n")
                .append("Average_Latency = ").append(averageShardLatency).append("ms\n")
                .append("```")

        context.channel.sendMessage(toSend.toString()).queue()
    }
}
