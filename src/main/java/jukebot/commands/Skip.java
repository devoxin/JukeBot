package jukebot.commands;

import jukebot.JukeBot;
import jukebot.audioutilities.MusicManager;
import jukebot.utils.Command;
import jukebot.utils.CommandProperties;
import jukebot.utils.Permissions;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

@CommandProperties(description = "Vote to skip the track", category = CommandProperties.category.CONTROLS)
public class Skip implements Command {

    final Permissions permissions = new Permissions();

    public void execute(GuildMessageReceivedEvent e, String query) {

        final MusicManager musicManager = JukeBot.getMusicManager(e.getGuild().getAudioManager());

        if (!musicManager.isPlaying()) {
            e.getChannel().sendMessage(new EmbedBuilder()
                    .setColor(JukeBot.EmbedColour)
                    .setTitle("No playback activity")
                    .setDescription("There's nothing playing.")
                    .build()
            ).queue();
            return;
        }

        if (!permissions.checkVoiceChannel(e.getMember())) {
            e.getChannel().sendMessage(new EmbedBuilder()
                    .setColor(JukeBot.EmbedColour)
                    .setTitle("No Mutual VoiceChannel")
                    .setDescription("Join my VoiceChannel to use this command.")
                    .build()
            ).queue();
            return;
        }

        final int totalVotes = musicManager.handler.voteSkip(e.getAuthor().getIdLong());

        final int neededVotes = (int) Math.ceil(e.getGuild().getAudioManager().getConnectedChannel()
                .getMembers().stream().filter(u -> !u.getUser().isBot()).count() * 0.5);

        if (neededVotes - totalVotes <= 0)
            musicManager.handler.playNext();
        else
            e.getChannel().sendMessage(new EmbedBuilder()
                    .setColor(JukeBot.EmbedColour)
                    .setTitle("Vote Acknowledged")
                    .setDescription((neededVotes - totalVotes) + " votes needed to skip.")
                    .build()
            ).queue();
    }
}