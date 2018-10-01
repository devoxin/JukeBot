package jukebot.commands;

import jukebot.Database;
import jukebot.audioutilities.AudioHandler;
import jukebot.utils.Command;
import jukebot.utils.CommandProperties;
import jukebot.utils.Context;
import jukebot.utils.Permissions;

@CommandProperties(description = "Vote to skip the track", category = CommandProperties.category.CONTROLS)
public class Skip implements Command {

    private final Permissions permissions = new Permissions();

    public void execute(final Context context) {

        final AudioHandler player = context.getAudioPlayer();

        if (!player.isPlaying()) {
            context.embed("Not Playing", "Nothing is currently playing.");
            return;
        }

        if (!permissions.ensureMutualVoiceChannel(context.getMember())) {
            context.embed("No Mutual VoiceChannel", "Join my VoiceChannel to use this command.");
            return;
        }

        final int totalVotes = player.voteSkip(context.getAuthor().getIdLong());
        final double voteThreshold = Database.getSkipThreshold(context.getGuild().getIdLong());

        final int neededVotes = (int) Math.ceil(context.getGuild().getAudioManager().getConnectedChannel()
                .getMembers()
                .stream()
                .filter(u -> !u.getUser().isBot())
                .count() * voteThreshold);

        if (neededVotes - totalVotes <= 0) {
            player.playNext();
        } else {
            context.embed("Vote Acknowledged", (neededVotes - totalVotes) + " votes needed to skip.");
        }
    }
}