package jukebot.commands;

import jukebot.JukeBot;
import jukebot.audioutilities.AudioHandler;
import jukebot.utils.Command;
import jukebot.utils.CommandProperties;
import jukebot.utils.Helpers;
import jukebot.utils.Permissions;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

@CommandProperties(aliases = {"vol"}, description = "Adjust the player volume", category = CommandProperties.category.CONTROLS)
public class Volume implements Command {

    private final Permissions permissions = new Permissions();

    private final String brick = "\u25AC";
    private final int maxBricks = 10;

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

        if (query.length() == 0) {
            final int vol = player.player.getVolume();
            e.getChannel().sendMessage(new EmbedBuilder()
                    .setColor(JukeBot.embedColour)
                    .setTitle("Volume")
                    .setDescription(calculateBricks(vol) + " `" + vol + "%`")
                    .build()
            ).queue();
        } else {
            if (!permissions.isElevatedUser(e.getMember(), false)) {
                e.getChannel().sendMessage(new EmbedBuilder()
                        .setColor(JukeBot.embedColour)
                        .setTitle("Permission Error")
                        .setDescription("You need to have the DJ role.")
                        .build()
                ).queue();
                return;
            }

            player.player.setVolume(Helpers.parseNumber(query, 100));

            final int vol = player.player.getVolume();

            e.getChannel().sendMessage(new EmbedBuilder()
                    .setColor(JukeBot.embedColour)
                    .setTitle("Volume")
                    .setDescription(calculateBricks(vol) + " `" + vol + "%`")
                    .build()
            ).queue();
        }

    }

    private String calculateBricks(int volume) {
        final float percent = (float) volume / 150;
        final int blocks = (int) Math.floor(maxBricks * percent);

        final StringBuilder sb = new StringBuilder("[");

        for (int i = 0; i < maxBricks; i++) {
            if (i == blocks) {
                sb.append("](http://jukebot.xyz)");
            }

            sb.append(brick);
        }

        if (blocks == 10) {
            sb.append("](http://jukebot.xyz)");
        }

        return sb.toString();
    }
}
