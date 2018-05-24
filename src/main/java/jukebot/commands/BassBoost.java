package jukebot.commands;

import jukebot.audioutilities.AudioHandler;
import jukebot.utils.Command;
import jukebot.utils.CommandProperties;
import jukebot.utils.Context;
import jukebot.utils.Permissions;

@CommandProperties(aliases = {"bb"}, description = "Bass boosts the audio", category = CommandProperties.category.CONTROLS)
public class BassBoost implements Command {

    private final Permissions permissions = new Permissions();

    @Override
    public void execute(final Context context) {
        final AudioHandler handler = context.getAudioPlayer();
        final String[] args = context.getArgs();

        if (!handler.isPlaying()) {
            context.sendEmbed("Not Playing", "Nothing is currently playing.");
            return;
        }

        if (!permissions.ensureMutualVoiceChannel(context.getMember())) {
            context.sendEmbed("No Mutual VoiceChannel", "Join my VoiceChannel to use this command.");
            return;
        }

        if (!context.isDJ(true)) {
            context.sendEmbed("Not a DJ", "You need to be a DJ to use this command.\n[See here on how to become a DJ](https://jukebot.xyz/faq)");
            return;
        }

        /*
        if (!JukeBot.isSelfHosted && permissions.getTier(e.getAuthor().getIdLong()) < 2) {
            e.getChannel().sendMessage(new EmbedBuilder()
                    .setColor(JukeBot.embedColour)
                    .setTitle("Upvote-locked command") // eww
                    .setDescription("Upvote [here] to unlock this command!") // TODO check upvotes
                    .setFooter("", null)
                    .build()
            ).queue();
            return;
        }
        */

        if (context.getArgString().isEmpty()) {
            context.sendEmbed("BassBoost Presets",
                    "Current Setting: `" + handler.getBassBoostSetting() + "`\n\nValid presets: `Off`, `Low`, `Medium`, `High`, `Insane`",
                    "Distortion may occur on higher presets and damage hearing during prolonged listening periods");
            return;
        }

        switch (args[0]) {
            case "o":
            case "off":
                handler.bassBoost(AudioHandler.bassBoost.OFF);
                break;
            case "l":
            case "low":
                handler.bassBoost(AudioHandler.bassBoost.LOW);
                break;
            case "m":
            case "medium":
                handler.bassBoost(AudioHandler.bassBoost.MEDIUM);
                break;
            case "h":
            case "high":
                handler.bassBoost(AudioHandler.bassBoost.HIGH);
                break;
            case "i":
            case "insane":
                handler.bassBoost(AudioHandler.bassBoost.INSANE);
                break;
            default:
                context.sendEmbed("BassBoost", args[0] + " is not a recognised preset");
                return;
        }

        context.sendEmbed("BassBoost", "Set bass boost to `" + handler.getBassBoostSetting() + "`");
    }
}
