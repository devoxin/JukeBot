package jukebot.commands;

import jukebot.JukeBot;
import jukebot.audioutilities.AudioHandler;
import jukebot.utils.Command;
import jukebot.utils.CommandProperties;
import jukebot.utils.Permissions;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

@CommandProperties(description = "Plays the queue in random order", category = CommandProperties.category.CONTROLS)
public class Shuffle implements Command {

    private final Permissions permissions = new Permissions();

    public void execute(GuildMessageReceivedEvent e, String query) {

        final AudioHandler manager = JukeBot.getMusicManager(e.getGuild().getAudioManager());

        if (!manager.isPlaying()) {
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

        if (!permissions.isElevatedUser(e.getMember(), true)) {
            e.getChannel().sendMessage(new EmbedBuilder()
                    .setColor(JukeBot.EmbedColour)
                    .setTitle("Permission Error")
                    .setDescription("You need to have the DJ role.")
                    .build()
            ).queue();
            return;
        }

        e.getChannel().sendMessage(new EmbedBuilder()
                .setColor(JukeBot.EmbedColour)
                .setTitle("Shuffle")
                .setDescription("Shuffle **" + (manager.toggleShuffle() ? "enabled" : "disabled") + "**")
                .build()
        ).queue();

    }
}
