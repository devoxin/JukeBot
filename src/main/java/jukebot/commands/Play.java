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

import static jukebot.utils.Bot.LOG;

public class Play implements Command {

    private final Permissions permissions = new Permissions();

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

        if (!e.getMember().getVoiceState().inVoiceChannel()) {
            e.getChannel().sendMessage(new EmbedBuilder()
                    .setColor(Bot.EmbedColour)
                    .setTitle("Join a voicechannel first")
                    .setDescription("You need to join a voicechannel before you can queue songs.")
                    .build()
            ).queue();
            return;
        }

        AudioManager audioManager = e.getGuild().getAudioManager();

        if (audioManager.isConnected() && !e.getMember().getVoiceState().getChannel().getId().equalsIgnoreCase(audioManager.getConnectedChannel().getId())) {
            e.getChannel().sendMessage(new EmbedBuilder()
                    .setColor(Bot.EmbedColour)
                    .setTitle("No Mutual VoiceChannel")
                    .setDescription("Join my voicechannel to use this command.")
                    .build()
            ).queue();
            return;
        }

        Permissions.CONNECT_STATUS canConnect = permissions.canConnect(e.getMember().getVoiceState().getChannel());

        if (canConnect == Permissions.CONNECT_STATUS.NO_CONNECT_SPEAK) {
            e.getChannel().sendMessage(new EmbedBuilder()
                    .setColor(Bot.EmbedColour)
                    .setTitle("Invalid Channel Permissions")
                    .setDescription("The target voicechannel doesn't allow me to Connect/Speak\n\nPlease reconfigure channel permissions or move to another channel.")
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

        final GuildMusicManager musicManager = JukeBot.getGuildMusicManager(e.getGuild());

        if (!audioManager.isConnected() && !audioManager.isAttemptingToConnect()) {
            LOG.debug("Connecting to " + e.getGuild().getId());
            audioManager.openAudioConnection(e.getMember().getVoiceState().getChannel());
            audioManager.setSelfDeafened(true);
            musicManager.handler.setChannel(e.getChannel());
        }

        final String userQuery = query.replaceAll("[<>]", "");

        if (userQuery.startsWith("http"))
            Bot.playerManager.loadItem(userQuery, new SongResultHandler(e, musicManager, false));
        else
            Bot.playerManager.loadItem( "ytsearch:" + userQuery, new SongResultHandler(e, musicManager, false));

    }
}
