package jukebot.commands;

import jukebot.JukeBot;
import jukebot.audioutilities.MusicManager;
import jukebot.audioutilities.SongResultHandler;
import jukebot.utils.Bot;
import jukebot.utils.Command;
import jukebot.utils.ConnectionError;
import jukebot.utils.Permissions;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.managers.AudioManager;

public class Play implements Command {

    final Permissions permissions = new Permissions();

    public void execute(GuildMessageReceivedEvent e, String query) {

        if (query.length() == 0) {
            e.getChannel().sendMessage(new EmbedBuilder()
                    .setColor(Bot.EmbedColour)
                    .setTitle("Specify something")
                    .setDescription("YouTube: Search Term/URL\nSoundCloud: URL")
                    .build()
            ).queue();
            return;
        }

        final AudioManager manager = e.getGuild().getAudioManager();
        final MusicManager gmanager = JukeBot.getMusicManager(manager);

        if (!permissions.checkVoiceChannel(e.getMember())) {
            e.getChannel().sendMessage(new EmbedBuilder()
                    .setColor(Bot.EmbedColour)
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
                        .setColor(Bot.EmbedColour)
                        .setTitle(connectionStatus.title)
                        .setDescription(connectionStatus.description)
                        .build()
                ).queue();
                return;
            }

            manager.openAudioConnection(e.getMember().getVoiceState().getChannel());
            manager.setSelfDeafened(true);
            gmanager.handler.setChannel(e.getChannel());
        }

        final String userQuery = query.replaceAll("[<>]", "");

        if (userQuery.startsWith("http"))
            Bot.playerManager.loadItem(userQuery, new SongResultHandler(e, gmanager, false));
        else
            Bot.playerManager.loadItem( "ytsearch:" + userQuery, new SongResultHandler(e, gmanager, false));

    }
}
