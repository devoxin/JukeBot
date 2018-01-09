package jukebot.commands;

import jukebot.JukeBot;
import jukebot.audioutilities.AudioHandler;
import jukebot.utils.Command;
import jukebot.utils.CommandProperties;
import jukebot.utils.Helpers;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

import java.text.DecimalFormat;
import java.util.Collections;

@CommandProperties(description = "Provides an insight into bot stats", category = CommandProperties.category.MISC)
public class Debug implements Command {

    private final DecimalFormat dpFormatter = new DecimalFormat("0.00");

    public void execute(GuildMessageReceivedEvent e, String query) {

        final StringBuilder toSend = new StringBuilder();
        final long rUsedRaw = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
        final String rPercent = dpFormatter.format((double) rUsedRaw / Runtime.getRuntime().totalMemory() * 100);
        final long streams = JukeBot.getPlayers().values().stream().filter(AudioHandler::isPlaying).count();
        final long servers = JukeBot.shardManager.getShards()
                .stream()
                .map(s -> s.getGuilds().size())
                .reduce(0, (a, b) -> a + b);

        toSend.append("S: ")
                .append(servers)
                .append(" | V: ")
                .append(streams)
                .append(" | T: ")
                .append(Thread.activeCount()) // muh special thread leaks
                .append(" | ")
                .append(Helpers.fTime(System.currentTimeMillis() - JukeBot.startTime))
                .append(" | ")
                .append((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1048576)
                .append("MB (").append(rPercent).append("%)\n\n");

        for (int i = 0; i < JukeBot.shardManager.getShardsTotal(); i++) {
            final JDA s = JukeBot.shardManager.getShardById(i);
            toSend.append(s.getShardInfo().getShardId() == e.getJDA().getShardInfo().getShardId() ? "*[" : " [")
                    .append(Helpers.padLeft(" ", Integer.toString(s.getShardInfo().getShardId() + 1), 2))
                    .append("] ")
                    .append(s.getStatus().toString())
                    .append(" G: ")
                    .append(s.getGuilds().size())
                    .append(" U: ")
                    .append(s.getUsers().size())
                    .append(" L: ")
                    .append(s.getPing())
                    .append("ms\n");
        }

        e.getChannel().sendMessage("```prolog\n" + toSend.toString().trim() + "\n```").queue();

    }

}
