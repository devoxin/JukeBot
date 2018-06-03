package jukebot.commands

import com.sun.management.OperatingSystemMXBean
import jukebot.Database
import jukebot.JukeBot
import jukebot.utils.Command
import jukebot.utils.CommandProperties
import jukebot.utils.Context
import jukebot.utils.Helpers
import net.dv8tion.jda.core.JDA
import java.lang.management.ManagementFactory
import java.text.DecimalFormat

@CommandProperties(description = "Bot statistics")
class Stats : Command {

    private val dpFormatter = DecimalFormat("0.00")

    override fun execute(context: Context) {
        val toSend = StringBuilder()
        val rUsedRaw = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        val rPercent = dpFormatter.format(rUsedRaw.toDouble() / Runtime.getRuntime().totalMemory() * 100)
        val usedMB = dpFormatter.format(rUsedRaw.toDouble() / 1048576)

        val players = JukeBot.getPlayers().values.stream().filter { it.isPlaying }.count()
        val encodingPlayers = JukeBot.getPlayers().values.stream().filter { it.isPlaying && (it.isBassBoosted || it.player.volume != 100) }.count()
        val servers = JukeBot.shardManager.guildCache.size()
        val users = JukeBot.shardManager.userCache.size()

        val shards = JukeBot.shardManager.shardsTotal
        val shardsOnline = JukeBot.shardManager.shards.stream().filter { s -> s.status == JDA.Status.CONNECTED }.count()
        val averageShardLatency = JukeBot.shardManager.shards.stream().map { it.ping }.reduce { a, b -> a + b }.get() / shards

        val osBean: OperatingSystemMXBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean::class.java)
        val procCpuUsage = dpFormatter.format(osBean.processCpuLoad * 100)
        val sysCpuUsage = dpFormatter.format(osBean.systemCpuLoad * 100)

        toSend.append("```ini\n")
                .append("[ JVM ]\n")
                .append("Uptime          = ").append(Helpers.fTime(System.currentTimeMillis() - JukeBot.startTime)).append("\n")
                .append("JVM_CPU_Usage   = ").append(procCpuUsage).append("%\n")
                .append("System_CPU_Usage= ").append(sysCpuUsage).append("%\n")
                .append("RAM_Usage       = ").append(usedMB).append("MB (").append(rPercent).append("%)\n")
                .append("Threads         = ").append(Thread.activeCount()).append("\n\n")
                .append("[ JukeBot ]\n")
                .append("Guilds          = ").append(servers).append("\n")
                .append("Users           = ").append(users).append("\n")
                .append("Players         = ").append(players).append(" (").append(encodingPlayers).append(" encoding)\n\n")
                .append("Database_Calls  = ").append(Database.calls).append("\n")
                .append("Shards_Online   = ").append(shardsOnline).append("/").append(shards).append("\n")
                .append("Average_Latency = ").append(averageShardLatency).append("ms\n")
                .append("```")

        context.channel.sendMessage(toSend.toString()).queue()
    }
}
