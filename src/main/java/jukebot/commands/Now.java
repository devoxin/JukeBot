package jukebot.commands;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import jukebot.JukeBot;
import jukebot.utils.Bot;
import jukebot.utils.Command;
import jukebot.utils.Time;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

public class Now implements Command {

    public void execute(GuildMessageReceivedEvent e, String query) {

        final AudioTrack current = JukeBot.getGuildMusicManager(e.getGuild().getAudioManager()).player.getPlayingTrack();

        if (current == null) {
            e.getChannel().sendMessage(new EmbedBuilder()
                    .setColor(Bot.EmbedColour)
                    .setTitle("No playback activity")
                    .setDescription("There's nothing playing.")
                    .build()
            ).queue();
            return;
        }

        e.getChannel().sendMessage(new EmbedBuilder()
                .setColor(Bot.EmbedColour)
                .setTitle("Now Playing")
                .setDescription("**[" + current.getInfo().title + "](" + current.getInfo().uri + ")**\n" +
                        "(" + Time.format(current.getPosition()) + "/" + (current.getInfo().isStream ? "LIVE)" : Time.format(current.getDuration()) + ") - <@" + current.getUserData() + ">"))
                .build()
        ).queue();

    }
}
