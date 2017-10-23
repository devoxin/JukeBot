package jukebot.commands;

import jukebot.JukeBot;
import jukebot.audioutilities.AudioHandler;
import jukebot.audioutilities.MusicManager;
import jukebot.utils.Bot;
import jukebot.utils.Command;
import jukebot.utils.Permissions;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

public class Repeat implements Command {

    private final Permissions permissions = new Permissions();

    public void execute(GuildMessageReceivedEvent e, String query) {

        final MusicManager manager = JukeBot.getMusicManager(e.getGuild().getAudioManager());

        if (!manager.isPlaying()) {
            e.getChannel().sendMessage(new EmbedBuilder()
                    .setColor(Bot.EmbedColour)
                    .setTitle("No playback activity")
                    .setDescription("There's nothing playing.")
                    .build()
            ).queue();
            return;
        }

        if (!permissions.checkVoiceChannel(e.getMember())) {
                e.getChannel().sendMessage(new EmbedBuilder()
                    .setColor(Bot.EmbedColour)
                        .setTitle("No Mutual VoiceChannel")
                        .setDescription("Join my VoiceChannel to use this command.")
                    .build()
            ).queue();
            return;
        }

        if (!permissions.isElevatedUser(e.getMember(), true)) {
            e.getChannel().sendMessage(new EmbedBuilder()
                    .setColor(Bot.EmbedColour)
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
                    manager.handler.setRepeat(AudioHandler.REPEATMODE.ALL);
                    break;
                case "s":
                case "single":
                    manager.handler.setRepeat(AudioHandler.REPEATMODE.SINGLE);
                    break;
                case "n":
                case "none":
                    manager.handler.setRepeat(AudioHandler.REPEATMODE.NONE);
                    break;
                default:
                    e.getChannel().sendMessage(new EmbedBuilder()
                            .setColor(Bot.EmbedColour)
                            .setTitle("Repeat Modes")
                            .setDescription("(**S**)ingle | (**A**)ll | (**N**)one")
                            .build()
                    ).queue();
                    return;
            }
        } else {
            e.getChannel().sendMessage(new EmbedBuilder()
                    .setColor(Bot.EmbedColour)
                    .setTitle("Repeat Modes")
                    .setDescription("(**S**)ingle | (**A**)ll | (**N**)one\n\nCurrent: " + manager.handler.getStringifiedRepeat())
                    .build()
            ).queue();
            return;
        }

        e.getChannel().sendMessage(new EmbedBuilder()
                .setColor(Bot.EmbedColour)
                .setTitle("Repeat")
                .setDescription("Repeat set to **" + manager.handler.getStringifiedRepeat() + "**")
                .build()
        ).queue();

    }
}
