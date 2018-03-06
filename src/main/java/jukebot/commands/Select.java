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

@CommandProperties(description = "Search YouTube and select from up to 5 tracks", aliases = {"sel", "s"}, category = CommandProperties.category.CONTROLS)
public class Select implements Command {

    public void execute(GuildMessageReceivedEvent e, String query) {

        final Permissions permissions = new Permissions();

        if (query.length() == 0) {
            e.getChannel().sendMessage(new EmbedBuilder()
                    .setColor(JukeBot.embedColour)
                    .setTitle("No Search Query Specified")
                    .setDescription("Specify a term to search YouTube for")
                    .build()
            ).queue();
            return;
        }

        final AudioManager manager = e.getGuild().getAudioManager();
        final AudioHandler player = JukeBot.getPlayer(manager);

        if (!permissions.checkVoiceConnection(e.getMember())) {
            e.getChannel().sendMessage(new EmbedBuilder()
                    .setColor(JukeBot.embedColour)
                    .setTitle("No Mutual VoiceChannel")
                    .setDescription("Join my VoiceChannel to use this command.")
                    .build()
            ).queue();
            return;
        }

        if (!manager.isAttemptingToConnect() && !manager.isConnected()) {
            ConnectionError connectionStatus = permissions.canConnectTo(e.getMember().getVoiceState().getChannel());

            if (null != connectionStatus) {
                e.getChannel().sendMessage(new EmbedBuilder()
                        .setColor(JukeBot.embedColour)
                        .setTitle(connectionStatus.title)
                        .setDescription(connectionStatus.description)
                        .build()
                ).queue();
                return;
            }

            manager.openAudioConnection(e.getMember().getVoiceState().getChannel());
            player.setChannel(e.getChannel().getIdLong());
        }

        JukeBot.playerManager.loadItem("ytsearch:" + query, new SongResultHandler(e, player, true));
    }
}
