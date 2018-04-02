package jukebot.commands;

import jukebot.JukeBot;
import jukebot.audioutilities.AudioHandler;
import jukebot.utils.Command;
import jukebot.utils.CommandProperties;
import jukebot.utils.Permissions;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

@CommandProperties(description = "Loop the queue, track or nothing", category = CommandProperties.category.CONTROLS, enabled = false)
public class Repeat implements Command {

    private final Permissions permissions = new Permissions();

    public void execute(GuildMessageReceivedEvent e, String query) {

        final AudioHandler player = JukeBot.getPlayer(e.getGuild().getAudioManager());

        if (!player.isPlaying()) {
            e.getChannel().sendMessage(new EmbedBuilder()
                    .setColor(JukeBot.embedColour)
                    .setTitle("No playback activity")
                    .setDescription("There's nothing playing.")
                    .build()
            ).queue();
            return;
        }

        if (!permissions.ensureMutualVoiceChannel(e.getMember())) {
            e.getChannel().sendMessage(new EmbedBuilder()
                    .setColor(JukeBot.embedColour)
                    .setTitle("No Mutual VoiceChannel")
                    .setDescription("Join my VoiceChannel to use this command.")
                    .build()
            ).queue();
            return;
        }

        if (!permissions.isElevatedUser(e.getMember(), true)) {
            e.getChannel().sendMessage(new EmbedBuilder()
                    .setColor(JukeBot.embedColour)
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
                    player.setRepeat(AudioHandler.repeatMode.ALL);
                    break;
                case "s":
                case "single":
                    player.setRepeat(AudioHandler.repeatMode.SINGLE);
                    break;
                case "n":
                case "none":
                    player.setRepeat(AudioHandler.repeatMode.NONE);
                    break;
                default:
                    e.getChannel().sendMessage(new EmbedBuilder()
                            .setColor(JukeBot.embedColour)
                            .setTitle("Repeat Modes")
                            .setDescription("(**S**)ingle | (**A**)ll | (**N**)one")
                            .build()
                    ).queue();
                    return;
            }
        } else {
            e.getChannel().sendMessage(new EmbedBuilder()
                    .setColor(JukeBot.embedColour)
                    .setTitle("Repeat Modes")
                    .setDescription("(**S**)ingle | (**A**)ll | (**N**)one\n\nCurrent: " + player.getRepeatMode())
                    .build()
            ).queue();
            return;
        }

        e.getChannel().sendMessage(new EmbedBuilder()
                .setColor(JukeBot.embedColour)
                .setTitle("Repeat")
                .setDescription("Repeat set to **" + player.getRepeatMode() + "**")
                .build()
        ).queue();

    }
}
