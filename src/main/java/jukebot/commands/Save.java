package jukebot.commands;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import jukebot.JukeBot;
import jukebot.audioutilities.GuildMusicManager;
import jukebot.utils.Bot;
import jukebot.utils.Command;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.PrivateChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;

public class Save implements Command {

    public void execute(MessageReceivedEvent e, String query) {

        final GuildMusicManager musicManager = JukeBot.getGuildMusicManager(e.getGuild());
        final AudioTrack currentTrack = musicManager.player.getPlayingTrack();

        if (currentTrack == null) {
            e.getTextChannel().sendMessage(new EmbedBuilder()
                    .setColor(Bot.EmbedColour)
                    .setTitle("No playback activity")
                    .setDescription("There's nothing playing.")
                    .build()
            ).queue();
            return;
        }

        final PrivateChannel DMChannel = e.getAuthor().openPrivateChannel().complete();
        DMChannel.sendMessage("I cannot send messages/embed links in " + e.getTextChannel().getAsMention() + "\nSwitch to another channel.")
                .queue(null,
                        error -> e.getTextChannel().sendMessage(new EmbedBuilder()
                                .setColor(Bot.EmbedColour)
                                .setTitle("Unable to DM")
                                .setDescription("I was unable to DM you.\nEnsure I'm not blocked and your DMs are enabled.")
                                .build()
                        ).queue());

        DMChannel.sendMessage(new EmbedBuilder()
                .setColor(Bot.EmbedColour)
                .setTitle(currentTrack.getInfo().title, currentTrack.getInfo().uri)
                .build()
        ).queue();

    }
}
