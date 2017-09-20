package jukebot.commands;

import jukebot.JukeBot;
import jukebot.Shard;
import jukebot.utils.Command;
import jukebot.utils.Time;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

import java.util.Arrays;

public class Debug implements Command {

    public void execute(MessageReceivedEvent e, String query) {

        final StringBuilder toSend = new StringBuilder();
        final long streams = JukeBot.getMusicManagers().values().stream().filter(m -> m.handler != null && m.player.getPlayingTrack() != null).count();
        final long servers = Arrays.stream(JukeBot.getShards()).map(s -> s.jda.getGuilds().size()).reduce(0, (a, b) -> a + b);

        toSend.append("S: ")
                .append(servers)
                .append(" | V: ")
                .append(streams)
                .append(" | T: ")
                .append(Thread.activeCount())
                .append(" | ")
                .append(Time.format(System.currentTimeMillis() - JukeBot.startTime))
                .append(" | ")
                .append((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1048576)
                .append("MB\n\n");

        for (Shard s : JukeBot.getShards())
            toSend.append(s.jda.getShardInfo().getShardId() == e.getJDA().getShardInfo().getShardId() ? "*[" : " [")
                    .append(s.jda.getShardInfo().getShardId() + 1)
                    .append("] ")
                    .append(s.jda.getStatus().toString())
                    .append(" G: ")
                    .append(s.jda.getGuilds().size())
                    .append(" U: ")
                    .append(s.jda.getUsers().size())
                    .append(" L: ")
                    .append(s.jda.getPing())
                    .append("ms\n");

        e.getTextChannel().sendMessage("```prolog\n" + toSend.toString().trim() + "\n```").queue();

    }

}
