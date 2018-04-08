package jukebot.commands;

import jukebot.JukeBot;
import jukebot.audioutilities.AudioHandler;
import jukebot.utils.Command;
import jukebot.utils.CommandProperties;
import jukebot.utils.Helpers;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

import java.text.DecimalFormat;

@CommandProperties(description = "Provides an insight into bot stats", category = CommandProperties.category.MISC)
public class Debug implements Command {

    private final DecimalFormat dpFormatter = new DecimalFormat("0.00");

    public void execute(GuildMessageReceivedEvent e, String query) {

        String[] args = query.split("\\s+");

        if (args[0].equalsIgnoreCase("preload")) {
            if (JukeBot.isSelfHosted) {
                e.getChannel().sendMessage("Command unavailable").queue();
                return;
            }
            if (args.length < 3) {
                e.getChannel().sendMessage("Missing arg `key`").queue();
            } else {
                JukeBot.recreatePatreonApi(args[1]);
                e.getMessage().addReaction("\uD83D\uDC4C").queue();
            }
        } else if (args[0].equalsIgnoreCase("stats")) {
            final StringBuilder toSend = new StringBuilder();
            final long rUsedRaw = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
            final String rPercent = dpFormatter.format((double) rUsedRaw / Runtime.getRuntime().totalMemory() * 100);
            final String usedMB = dpFormatter.format((double) rUsedRaw / 1048576);

            final long players = JukeBot.getPlayers().values().stream().filter(AudioHandler::isPlaying).count();
            final long servers = JukeBot.shardManager.getGuildCache().size();
            final long users = JukeBot.shardManager.getUserCache().size();

            final int shards = JukeBot.shardManager.getShardsTotal();
            final long shardsOnline = JukeBot.shardManager.getShards().stream().filter(s -> s.getStatus() == JDA.Status.CONNECTED).count();
            final long averageShardLatency = JukeBot.shardManager.getShards()
                    .stream()
                    .map(JDA::getPing)
                    .reduce((a, b) -> a + b).get() / shards;

            toSend.append("```prolog\n")
                    .append("Uptime            : ").append(Helpers.fTime(System.currentTimeMillis() - JukeBot.startTime)).append("\n")
                    .append("RAM Usage         : ").append(usedMB).append("MB (").append(rPercent).append("%)\n")
                    .append("Threads           : ").append(Thread.activeCount()).append("\n\n")
                    .append("Guilds            : ").append(servers).append("\n")
                    .append("Users             : ").append(users).append("\n")
                    .append("Players           : ").append(players).append("\n\n")
                    .append("Shards Online     : ").append(shardsOnline).append("/").append(shards).append("\n")
                    .append("Avg. Shard Latency: ").append(averageShardLatency).append("ms\n")
                    .append("```");

            e.getChannel().sendMessage(toSend.toString()).queue();
        } else {
            e.getChannel().sendMessage(new EmbedBuilder()
                    .setColor(JukeBot.embedColour)
                    .setTitle("Debug Subcommands")
                    .setDescription("`->` stats\n`->` preload <key>")
                    .build()
            ).queue();
        }
    }

}
