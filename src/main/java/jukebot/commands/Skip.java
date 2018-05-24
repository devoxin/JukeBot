package jukebot.commands;

import jukebot.audioutilities.AudioHandler;
import jukebot.utils.Command;
import jukebot.utils.CommandProperties;
import jukebot.utils.Context;
import jukebot.utils.Permissions;

@CommandProperties(description = "Vote to skip the track", category = CommandProperties.category.CONTROLS)
public class Skip implements Command {

    final Permissions permissions = new Permissions();

    public void execute(final Context context) {

        final AudioHandler player = context.getAudioPlayer();

        if (!player.isPlaying()) {
            context.sendEmbed("Not Playing", "Nothing is currently playing.");
            return;
        }

        if (!permissions.ensureMutualVoiceChannel(context.getMember())) {
            context.sendEmbed("No Mutual VoiceChannel", "Join my VoiceChannel to use this command.");
            return;
        }

        final int totalVotes = player.voteSkip(context.getAuthor().getIdLong());

        final int neededVotes = (int) Math.ceil(context.getGuild().getAudioManager().getConnectedChannel()
                .getMembers()
                .stream()
                .filter(u -> !u.getUser().isBot())
                .count() * 0.5);

        if (neededVotes - totalVotes <= 0) {
            player.playNext();
        } else {
            context.sendEmbed("Vote Acknowledged", (neededVotes - totalVotes) + " votes needed to skip.");
        }
    }
}