package jukebot.commands;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import jukebot.JukeBot;
import jukebot.audioutilities.GuildMusicManager;
import jukebot.utils.Bot;
import jukebot.utils.Command;
import jukebot.utils.Permissions;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

public class Reset implements Command {

    final Permissions permissions = new Permissions();

    public void execute(GuildMessageReceivedEvent e, String query) {

        if (!permissions.isElevatedUser(e.getMember(), true)) {
            e.getChannel().sendMessage(new EmbedBuilder()
                    .setColor(Bot.EmbedColour)
                    .setTitle("Permission Error")
                    .setDescription("You need to have the 'DJ' role!")
                    .build()
            ).queue();
            return;
        }

        Message m = e.getChannel().sendMessage(new EmbedBuilder()
                .setColor(Bot.EmbedColour)
                .setTitle("Resetting Audio")
                .setDescription("Please wait...")
                .build()
        ).complete();

        final GuildMusicManager musicManager = JukeBot.getGuildMusicManager(e.getGuild());

        AudioTrack t = musicManager.player.getPlayingTrack() != null ? musicManager.player.getPlayingTrack().makeClone() : null;
        VoiceChannel vc = e.getGuild().getAudioManager().getConnectedChannel();

        if (t != null)
            t.setUserData(musicManager.player.getPlayingTrack().getUserData());

        if (t != null && t.isSeekable())
            t.setPosition(musicManager.player.getPlayingTrack().getPosition());

        if (vc != null)
            e.getGuild().getAudioManager().closeAudioConnection();

        musicManager.ResetPlayer();

        if (vc != null && permissions.canConnect(vc))
            e.getGuild().getAudioManager().openAudioConnection(vc);

        if (t != null && (e.getGuild().getAudioManager().isAttemptingToConnect() || e.getGuild().getAudioManager().isConnected()))
            musicManager.handler.queue(t, null);

        m.editMessage(new EmbedBuilder()
                .setColor(Bot.EmbedColour)
                .setTitle("Audio Reset")
                .setDescription("Everything should now be in working order.")
                .build()
        ).queue();

    }

}
