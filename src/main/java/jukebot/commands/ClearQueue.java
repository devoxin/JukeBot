package jukebot.commands;

import jukebot.JukeBot;
import jukebot.audioutilities.AudioHandler;
import jukebot.utils.Command;
import jukebot.utils.CommandProperties;
import jukebot.utils.Permissions;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

@CommandProperties(description = "Removes all of the tracks from the queue", aliases = {"cq", "c", "clear", "empty"}, category = CommandProperties.category.MEDIA)
public class ClearQueue implements Command {

    private final Permissions permissions = new Permissions();

    public void execute(GuildMessageReceivedEvent e, String args) {

        final AudioHandler musicManager = JukeBot.getMusicManager(e.getGuild().getAudioManager());

        if (musicManager.getQueue().isEmpty()) {
            e.getChannel().sendMessage(new EmbedBuilder()
                    .setColor(JukeBot.EmbedColour)
                    .setTitle("Queue Already Empty")
                    .setDescription("The queue cannot be cleared as there is nothing queued.")
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

        musicManager.getQueue().clear();

        e.getChannel().sendMessage(new EmbedBuilder()
                .setColor(JukeBot.EmbedColour)
                .setTitle("Queue Cleared")
                .setDescription("The queue is now empty.")
                .build()
        ).queue();

    }

}
