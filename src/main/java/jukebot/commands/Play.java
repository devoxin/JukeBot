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

@CommandProperties(description = "Find a track on YouTube and queue it", aliases = {"p"}, category = CommandProperties.category.CONTROLS)
public class Play implements Command {

    final Permissions permissions = new Permissions();

    public void execute(GuildMessageReceivedEvent e, String query) {

        if (query.length() == 0) {
            e.getChannel().sendMessage(new EmbedBuilder()
                    .setColor(JukeBot.embedColour)
                    .setTitle("Specify something")
                    .setDescription("YouTube: Search Term/URL\nSoundCloud: URL")
                    .build()
            ).queue();
            return;
        }

        final AudioManager manager = e.getGuild().getAudioManager();
        final AudioHandler player = JukeBot.getPlayer(manager);

        if (!permissions.checkVoiceChannel(e.getMember())) {
            e.getChannel().sendMessage(new EmbedBuilder()
                    .setColor(JukeBot.embedColour)
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
                        .setColor(JukeBot.embedColour)
                        .setTitle(connectionStatus.title)
                        .setDescription(connectionStatus.description)
                        .build()
                ).queue();
                return;
            }

            manager.openAudioConnection(e.getMember().getVoiceState().getChannel());
            player.setChannel(e.getChannel());
        }

        final String userQuery = query.replaceAll("[<>]", "");

        if (userQuery.startsWith("http"))
            JukeBot.playerManager.loadItem(userQuery, new SongResultHandler(e, player, false));
        else
            JukeBot.playerManager.loadItem( "ytsearch:" + userQuery, new SongResultHandler(e, player, false));

    }
}
