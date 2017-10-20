package jukebot.commands;

import jukebot.JukeBot;
import jukebot.audioutilities.GuildMusicManager;
import jukebot.audioutilities.SongResultHandler;
import jukebot.utils.Bot;
import jukebot.utils.Command;
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
        final GuildMusicManager gmanager = JukeBot.getGuildMusicManager(manager);

        if (!permissions.checkVoiceChannel(e.getMember())) {
            final String context = (manager.isAttemptingToConnect() || manager.isConnected()) ? "my" : "a";
            e.getChannel().sendMessage(new EmbedBuilder()
                    .setColor(Bot.EmbedColour)
                    .setTitle("No Mutual VoiceChannel")
                    .setDescription("Join " + context + " VoiceChannel to use this command.")
                    .build()
            ).queue();
            return;
        }

        if (!manager.isAttemptingToConnect() && !manager.isConnected()) {
            Permissions.CONNECT_STATUS canConnect = permissions.canConnect(e.getMember().getVoiceState().getChannel());

            if (Permissions.CONNECT_STATUS.NO_CONNECT_SPEAK == canConnect) {
                e.getChannel().sendMessage(new EmbedBuilder()
                        .setColor(Bot.EmbedColour)
                        .setTitle("Invalid Channel Permissions")
                        .setDescription("Your VoiceChannel doesn't allow me to Connect/Speak\n\nPlease grant me the 'Connect' and 'Speak' permissions or move to another channel.")
                        .build()
                ).queue();
                return;
            } else if (canConnect == Permissions.CONNECT_STATUS.USER_LIMIT) {
                e.getChannel().sendMessage(new EmbedBuilder()
                        .setColor(Bot.EmbedColour)
                        .setTitle("VoiceChannel Full")
                        .setDescription("Your VoiceChannel is full. Raise the user limit or grant me the 'Move Members' permission.")
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
