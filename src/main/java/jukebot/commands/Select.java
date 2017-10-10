package jukebot.commands;

import jukebot.JukeBot;
import jukebot.audioutilities.GuildMusicManager;
import jukebot.audioutilities.SongResultHandler;
import jukebot.utils.Bot;
import jukebot.utils.Command;
import jukebot.utils.Helpers;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

public class Select implements Command {

    public void execute(GuildMessageReceivedEvent e, String query) {

        if (query.length() == 0) {
            e.getChannel().sendMessage(new EmbedBuilder()
                    .setColor(Bot.EmbedColour)
                    .setTitle("No Search Query Specified")
                    .setDescription("Specify a term to search YouTube for")
                    .build()
            ).queue();
            return;
        }

        Helpers.VOICE_STATUS status = Helpers.ConnectVoice(e.getGuild().getAudioManager(), e.getChannel(), e.getMember());

        if (status == Helpers.VOICE_STATUS.CANNOT_CONNECT)
            return;

        final GuildMusicManager musicManager = JukeBot.getGuildMusicManager(e.getGuild());
        if (status == Helpers.VOICE_STATUS.CONNECTED)
            musicManager.handler.setChannel(e.getChannel());

        Bot.playerManager.loadItem("ytsearch:" + query, new SongResultHandler(e, musicManager, true));
    }
}
