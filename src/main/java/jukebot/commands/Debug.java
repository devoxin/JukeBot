package jukebot.commands;

import jukebot.JukeBot;
import jukebot.Shard;
import jukebot.audioutilities.MusicManager;
import jukebot.utils.Command;
import jukebot.utils.Helpers;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

import java.util.Arrays;

public class Debug implements Command {

    private final int padLength = String.valueOf(JukeBot.getShards().length).length();

    public void execute(GuildMessageReceivedEvent e, String query) {

        final StringBuilder toSend = new StringBuilder();
        final long streams = JukeBot.getMusicManagers().values().stream().filter(MusicManager::isPlaying).count();
        final long servers = Arrays.stream(JukeBot.getShards())
                .filter(s ->  s != null && s.jda != null)
                .map(s -> s.jda.getGuilds().size())
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
                .append("MB\n\n");

        for (Shard s : JukeBot.getShards()) {
            if (s == null || s.jda == null) {
                toSend.append(" [] SHARD_NOT_CREATED\n");
                continue;
            }
            toSend.append(s.jda.getShardInfo().getShardId() == e.getJDA().getShardInfo().getShardId() ? "*[" : " [")
                    .append(Helpers.PadLeft(" ", Integer.toString(s.jda.getShardInfo().getShardId() + 1), padLength))
                    .append("] ")
                    .append(s.jda.getStatus().toString())
                    .append(" G: ")
                    .append(s.jda.getStatus() == JDA.Status.CONNECTED ? s.jda.getGuilds().size() : "0")
                    .append(" U: ")
                    .append(s.jda.getStatus() == JDA.Status.CONNECTED ? s.jda.getUsers().size() : "0")
                    .append(" L: ")
                    .append(s.jda.getPing())
                    .append("ms\n");
        }

        e.getChannel().sendMessage("```prolog\n" + toSend.toString().trim() + "\n```").queue();

    }

}
