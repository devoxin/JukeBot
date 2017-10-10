package jukebot.commands;

import jukebot.JukeBot;
import jukebot.audioutilities.GuildMusicManager;
import jukebot.audioutilities.SongResultHandler;
import jukebot.utils.Bot;
import jukebot.utils.Command;
import jukebot.utils.Helpers;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

public class ScSearch implements Command {

    public void execute(GuildMessageReceivedEvent e, String query) {

        if (query.length() == 0) {
            e.getChannel().sendMessage(new EmbedBuilder()
                    .setColor(Bot.EmbedColour)
                    .setTitle("No Search Query Specified")
                    .setDescription("Specify a term to search SoundCloud for")
                    .build()
            ).queue();
            return;
        }

        if (!Helpers.ConnectVoice(e.getGuild().getAudioManager(), e.getChannel(), e.getMember()))
            return;

        final GuildMusicManager musicManager = JukeBot.getGuildMusicManager(e.getGuild());
        musicManager.handler.setChannel(e.getChannel());

        Bot.playerManager.loadItem("scsearch:" + query, new SongResultHandler(e, musicManager, false));

    }
}
