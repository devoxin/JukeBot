package jukebot.commands;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import jukebot.DatabaseHandler;
import jukebot.JukeBot;
import jukebot.audioutilities.AudioHandler;
import jukebot.audioutilities.GuildMusicManager;
import jukebot.utils.Bot;
import jukebot.utils.Command;
import jukebot.utils.Helpers;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

public class Queue implements Command {

    private final DatabaseHandler db = new DatabaseHandler();

    public void execute(GuildMessageReceivedEvent e, String query) {

        final GuildMusicManager musicManager = JukeBot.getGuildMusicManager(e.getGuild().getAudioManager());
        final AudioHandler handler = musicManager.handler;

        if (handler.queue.isEmpty()) {
            e.getChannel().sendMessage(new EmbedBuilder()
                    .setColor(Bot.EmbedColour)
                    .setTitle("No songs queued")
                    .setDescription("Use `" + db.getPrefix(e.getGuild().getIdLong()) + "now` to view the current track.")
                    .build()
            ).queue();
            return;
        }

        final String queueDuration = Helpers.fTime(handler.queue.stream().map(AudioTrack::getDuration).reduce(0L, (a, b) -> a + b));
        final StringBuilder queue = new StringBuilder();

        final int maxPages = (int) Math.ceil((double) handler.queue.size() / 10);
        int page = Helpers.ParseNumber(query, 1);

        if (page < 1)
            page = 1;

        if (page > maxPages)
            page = maxPages;

        int begin = (page - 1) * 10;
        int end = (begin + 10) > handler.queue.size() ? handler.queue.size() : (begin + 10);

        for (int i = begin; i < end; i++)
            queue.append("`")
                    .append(i + 1)
                    .append(".` **[")
                    .append(handler.queue.get(i).getInfo().title)
                    .append("](")
                    .append(handler.queue.get(i).getInfo().uri)
                    .append(")** <@")
                    .append(handler.queue.get(i).getUserData())
                    .append(">\n");

        e.getChannel().sendMessage(new EmbedBuilder()
                .setColor(Bot.EmbedColour)
                .setTitle("Queue (" + handler.queue.size() + " songs, " + queueDuration + ")")
                .setDescription(queue.toString().trim())
                .addField("\u200B", "**Repeat:** " + String.valueOf(musicManager.handler.repeat).toLowerCase() + " **/ Shuffle:** " + (musicManager.handler.shuffle ? "On" : "Off"), true)
                .setFooter("Viewing page " + (page) + "/" + (maxPages), null)
                .build()
        ).queue();

    }
}
