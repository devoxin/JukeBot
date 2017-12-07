package jukebot.commands;

import jukebot.JukeBot;
import jukebot.audioutilities.AudioHandler;
import jukebot.audioutilities.SongResultHandler;
import jukebot.utils.Command;
import jukebot.utils.CommandProperties;
import jukebot.utils.ConnectionError;
import jukebot.utils.Permissions;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.managers.AudioManager;

@CommandProperties(description = "Search SoundCloud and queue the top result", aliases = {"sc"}, category = CommandProperties.category.CONTROLS)
public class ScSearch implements Command {

    final Permissions permissions = new Permissions();

    public void execute(GuildMessageReceivedEvent e, String query) {

        if (query.length() == 0) {
            e.getChannel().sendMessage(new EmbedBuilder()
                    .setColor(JukeBot.EmbedColour)
                    .setTitle("No Search Query Specified")
                    .setDescription("Specify a term to search SoundCloud for")
                    .build()
            ).queue();
            return;
        }

        final AudioManager manager = e.getGuild().getAudioManager();
        final AudioHandler gmanager = JukeBot.getMusicManager(manager);

        if (!permissions.checkVoiceChannel(e.getMember())) {
            e.getChannel().sendMessage(new EmbedBuilder()
                    .setColor(JukeBot.EmbedColour)
                    .setTitle("No Mutual VoiceChannel")
                    .setDescription("Join my VoiceChannel to use this command.")
                    .build()
            ).queue();
            return;
        }

        if (!manager.isAttemptingToConnect() && !manager.isConnected()) {
            ConnectionError connectionStatus = permissions.canConnect(e.getMember().getVoiceState().getChannel());

            if (null != connectionStatus) {
                e.getChannel().sendMessage(new EmbedBuilder()
                        .setColor(JukeBot.EmbedColour)
                        .setTitle(connectionStatus.title)
                        .setDescription(connectionStatus.description)
                        .build()
                ).queue();
                return;
            }

            manager.openAudioConnection(e.getMember().getVoiceState().getChannel());
            gmanager.setChannel(e.getChannel());
        }

        JukeBot.playerManager.loadItem("scsearch:" + query, new SongResultHandler(e, gmanager, false));

    }
}
