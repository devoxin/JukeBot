package jukebot.commands;

import jukebot.JukeBot;
import jukebot.utils.Command;
import jukebot.utils.Helpers;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

import java.text.DecimalFormat;
import java.util.Arrays;

public class Stats implements Command {

    DecimalFormat dpFormatter = new DecimalFormat("0.00");

    public void execute(GuildMessageReceivedEvent e, String args) {

        final long rUsedRaw = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
        final long rUsed = rUsedRaw / 1048576;
        final String rPercent = dpFormatter.format((double) rUsedRaw / Runtime.getRuntime().totalMemory() * 100);
        final int serverCount = Arrays.stream(JukeBot.getShards()).filter(s ->  s != null && s.jda != null).map(s -> s.jda.getGuilds().size()).reduce(0, (a, b) -> a + b);
        final int userCount = Arrays.stream(JukeBot.getShards()).filter(s ->  s != null && s.jda != null).map(s -> s.jda.getUsers().size()).reduce(0, (a, b) -> a + b);

        e.getChannel().sendMessage(new EmbedBuilder()
                .setColor(JukeBot.EmbedColour)
                .setTitle("JukeBot v" + JukeBot.VERSION, "https://github.com/Devoxin/JukeBot")
                .setDescription("**Uptime:** " + Helpers.fTime(System.currentTimeMillis() - JukeBot.startTime) +
                                "\n**RAM Usage:** " + rUsed + "MB (" + rPercent + "%)" +
                                "\n**Threads:** " + Thread.activeCount() +
                                "\n**Servers:** " + serverCount +
                                "\n**Users:** " + userCount +
                                "\n**Commands Calls:** " + JukeBot.commandCount)
                .setFooter("Developer: Kromatic#0387", null)
                .build()
        ).queue();
    }

}
