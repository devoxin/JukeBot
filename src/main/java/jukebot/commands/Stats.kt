package jukebot.commands

import jukebot.Database
import jukebot.JukeBot
import jukebot.utils.Command
import jukebot.utils.CommandProperties
import jukebot.utils.Context
import jukebot.utils.Helpers
import net.dv8tion.jda.core.JDA
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
        val servers = JukeBot.shardManager.guildCache.size()
        val users = JukeBot.shardManager.userCache.size()

        val shards = JukeBot.shardManager.shardsTotal
        val shardsOnline = JukeBot.shardManager.shards.stream().filter { s -> s.status == JDA.Status.CONNECTED }.count()
        val averageShardLatency = JukeBot.shardManager.shards
                .stream()
                .map { it.ping }
                .reduce { a, b -> a + b }.get() / shards

        toSend.append("```prolog\n")
                .append("Uptime            : ").append(Helpers.fTime(System.currentTimeMillis() - JukeBot.startTime)).append("\n")
                .append("RAM Usage         : ").append(usedMB).append("MB (").append(rPercent).append("%)\n")
                .append("Threads           : ").append(Thread.activeCount()).append("\n\n")
                .append("Guilds            : ").append(servers).append("\n")
                .append("Users             : ").append(users).append("\n")
                .append("Players           : ").append(players).append("\n\n")
                .append("Database Calls    : ").append(Database.calls).append("\n")
                .append("Shards Online     : ").append(shardsOnline).append("/").append(shards).append("\n")
                .append("Avg. Shard Latency: ").append(averageShardLatency).append("ms\n")
                .append("```")

        context.channel.sendMessage(toSend.toString()).queue()
    }
}
