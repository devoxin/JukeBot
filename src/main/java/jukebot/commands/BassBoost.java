package jukebot.commands;

import jukebot.JukeBot;
import jukebot.audioutilities.AudioHandler;
import jukebot.utils.Command;
import jukebot.utils.CommandProperties;
import jukebot.utils.Permissions;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

@CommandProperties(aliases = {"bb", "bassboostmedaddy", "needsmorebass", "hnnng"}, description = "Bass boosts the audio")
public class BassBoost implements Command {

    private final Permissions permissions = new Permissions();

    @Override
    public void execute(GuildMessageReceivedEvent e, String query) {
        AudioHandler handler = JukeBot.getPlayer(e.getGuild().getAudioManager());


        if (!handler.isPlaying()) {
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
                    .setDescription("You need to be a DJ")
                    .build()
            ).queue();
            return;
        }

        if (permissions.getTier(e.getAuthor().getIdLong()) < 2) {
            e.getChannel().sendMessage(new EmbedBuilder()
                    .setColor(JukeBot.embedColour)
                    .setTitle("Donator-only command")
                    .setDescription("This command requires **Tier 2** or higher.\n[Click here to donate](https://www.patreon.com/Devoxin)")
                    .build()
            ).queue();
            return;
        }

        if (query.length() == 0) {
            e.getChannel().sendMessage(new EmbedBuilder()
                    .setColor(JukeBot.embedColour)
                    .setTitle("Bass Boost Presets")
                    .setDescription("Off\nLow\nMedium\nHigh\nInsane")
                    .setFooter("Higher presets may cause distortion and damage hearing during prolonged listening periods", null)
                    .build()
            ).queue();
            return;
        }

        if (query.equalsIgnoreCase("off")) {
            handler.equalizer.setGain(0, 0F);
            handler.equalizer.setGain(1, 0F);
        } else if (query.equalsIgnoreCase("low")) {
            handler.equalizer.setGain(0, 0.25F);
            handler.equalizer.setGain(1, 0.15F);
        } else if (query.equalsIgnoreCase("medium")) {
            handler.equalizer.setGain(0, 0.50F);
            handler.equalizer.setGain(1, 0.25F);
        } else if (query.equalsIgnoreCase("high")) {
            handler.equalizer.setGain(0, 0.75F);
            handler.equalizer.setGain(1, 0.50F);
        } else if (query.equalsIgnoreCase("insane")) {
            handler.equalizer.setGain(0, 1F);
            handler.equalizer.setGain(1, 0.75F);
        }

        e.getChannel().sendMessage(new EmbedBuilder()
                .setColor(JukeBot.embedColour)
                .setTitle("Bass Boost")
                .setDescription("Set bass boost to `" + query.toLowerCase() + "`")
                .build()
        ).queue();
    }
}
