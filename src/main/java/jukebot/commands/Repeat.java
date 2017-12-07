package jukebot.commands;

import jukebot.JukeBot;
import jukebot.audioutilities.AudioHandler;
import jukebot.utils.Command;
import jukebot.utils.CommandProperties;
import jukebot.utils.Permissions;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

@CommandProperties(description = "Loop the queue, track or nothing",category = CommandProperties.category.CONTROLS)
public class Repeat implements Command {

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

        if (query.length() > 0) {
            switch (query.toLowerCase()) {
                case "a":
                case "all":
                    manager.setRepeat(AudioHandler.REPEATMODE.ALL);
                    break;
                case "s":
                case "single":
                    manager.setRepeat(AudioHandler.REPEATMODE.SINGLE);
                    break;
                case "n":
                case "none":
                    manager.setRepeat(AudioHandler.REPEATMODE.NONE);
                    break;
                default:
                    e.getChannel().sendMessage(new EmbedBuilder()
                            .setColor(JukeBot.EmbedColour)
                            .setTitle("Repeat Modes")
                            .setDescription("(**S**)ingle | (**A**)ll | (**N**)one")
                            .build()
                    ).queue();
                    return;
            }
        } else {
            e.getChannel().sendMessage(new EmbedBuilder()
                    .setColor(JukeBot.EmbedColour)
                    .setTitle("Repeat Modes")
                    .setDescription("(**S**)ingle | (**A**)ll | (**N**)one\n\nCurrent: " + manager.getStringifiedRepeat())
                    .build()
            ).queue();
            return;
        }

        e.getChannel().sendMessage(new EmbedBuilder()
                .setColor(JukeBot.EmbedColour)
                .setTitle("Repeat")
                .setDescription("Repeat set to **" + manager.getStringifiedRepeat() + "**")
                .build()
        ).queue();

    }
}
