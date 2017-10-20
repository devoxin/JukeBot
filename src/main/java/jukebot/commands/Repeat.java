package jukebot.commands;

import jukebot.JukeBot;
import jukebot.audioutilities.AudioHandler;
import jukebot.audioutilities.GuildMusicManager;
import jukebot.utils.Bot;
import jukebot.utils.Command;
import jukebot.utils.Permissions;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

public class Repeat implements Command {

    private final Permissions permissions = new Permissions();

    public void execute(GuildMessageReceivedEvent e, String query) {

        if (!e.getGuild().getAudioManager().isConnected()) {
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
                    .setTitle("Repeat")
                    .setDescription("You need to be in my voicechannel to toggle repeat.")
                    .build()
            ).queue();
            return;
        }

        if (!permissions.isElevatedUser(e.getMember(), true)) {
            e.getChannel().sendMessage(new EmbedBuilder()
                    .setColor(Bot.EmbedColour)
                    .setTitle("Permission Error")
                    .setDescription("You need to have the DJ role and also be [a Donator!](https://www.patreon.com/Devoxin)")
                    .build()
            ).queue();
            return;
        }

        GuildMusicManager manager = JukeBot.getGuildMusicManager(e.getGuild().getAudioManager());

        if (query.length() > 0) {
            switch (query.toLowerCase()) {
                case "a":
                case "all":
                    manager.handler.repeat = AudioHandler.REPEATMODE.ALL;
                    break;
                case "s":
                case "single":
                    manager.handler.repeat = AudioHandler.REPEATMODE.SINGLE;
                    break;
                case "n":
                case "none":
                    manager.handler.repeat = AudioHandler.REPEATMODE.NONE;
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
                    .setDescription("(**S**)ingle | (**A**)ll | (**N**)one\n\nCurrent: " + String.valueOf(manager.handler.repeat).toLowerCase())
                    .build()
            ).queue();
            return;
        }

        e.getChannel().sendMessage(new EmbedBuilder()
                .setColor(Bot.EmbedColour)
                .setTitle("Repeat")
                .setDescription("Repeat set to **" + String.valueOf(manager.handler.repeat).toLowerCase() + "**")
                .build()
        ).queue();

    }
}
