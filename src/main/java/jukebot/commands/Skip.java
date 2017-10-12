package jukebot.commands;

import jukebot.JukeBot;
import jukebot.audioutilities.GuildMusicManager;
import jukebot.utils.Bot;
import jukebot.utils.Command;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;

public class Skip implements Command {

    public void execute(GuildMessageReceivedEvent e, String query) {

        final GuildMusicManager musicManager = JukeBot.getGuildMusicManager(e.getGuild());

        if (musicManager.player.getPlayingTrack() == null) {
            e.getChannel().sendMessage(new EmbedBuilder()
                    .setColor(Bot.EmbedColour)
                    .setTitle("No playback activity")
                    .setDescription("There's nothing playing.")
                    .build()
            ).queue();
            return;
        }

        if (!e.getMember().getVoiceState().inVoiceChannel() ||
                e.getGuild().getAudioManager().isConnected() && !e.getMember().getVoiceState().getChannel().getId().equalsIgnoreCase(e.getGuild().getAudioManager().getConnectedChannel().getId())) {
            e.getChannel().sendMessage(new EmbedBuilder()
                    .setColor(Bot.EmbedColour)
                    .setTitle("Vote Skip")
                    .setDescription("You need to be in my voicechannel to skip.")
                    .build()
            ).queue();
            return;
        }

        final boolean skipAdded = musicManager.handler.voteSkip(e.getAuthor().getId());

        final int Votes = musicManager.handler.getVotes();
        final long NeededVotes = Math.round(((double) e.getMember().getVoiceState().getChannel().getMembers().stream().filter(u -> !u.getUser().isBot()).count()) / 2);

        if (NeededVotes - Votes <= 0)
            musicManager.handler.playNext(null);
        else
            e.getChannel().sendMessage(new EmbedBuilder()
                    .setColor(Bot.EmbedColour)
                    .setTitle(skipAdded ? "Vote Acknowledged" : "Already Voted")
                    .setDescription((NeededVotes - Votes) + " votes needed to skip.")
                    .build()
            ).queue();
    }
}