package jukebot.utils;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.managers.AudioManager;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import static jukebot.utils.Bot.LOG;

public class Helpers {
    private static ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private static Permissions permissions = new Permissions();

    private static int DURATION_LIMIT_NORMAL = 8000; // 2 hours
    private static int DURATION_LIMIT_PREMIUM = 20000; // 5 hours

    public static void DisconnectVoice(AudioManager manager) {
        if (!manager.isConnected() && !manager.isAttemptingToConnect())
            return;

        executor.execute(() -> {
            manager.closeAudioConnection();
            LOG.debug("Terminated AudioConnection in " + manager.getGuild().getId());
        });
    }

    public static boolean ConnectVoice(AudioManager manager, TextChannel channel, Member author) {

        if (!permissions.hasMutualVoiceChannel(author)) {
            channel.sendMessage(new EmbedBuilder()
                    .setColor(Bot.EmbedColour)
                    .setTitle("No Mutual VoiceChannel")
                    .setDescription("Join my VoiceChannel to use this command.")
                    .build()
            ).queue();
            return false;
        }

        Permissions.CONNECT_STATUS canConnect = permissions.canConnect(author.getVoiceState().getChannel());

        if (canConnect == Permissions.CONNECT_STATUS.NO_CONNECT_SPEAK) {
            channel.sendMessage(new EmbedBuilder()
                    .setColor(Bot.EmbedColour)
                    .setTitle("Invalid Channel Permissions")
                    .setDescription("Your VoiceChannel doesn't allow me to Connect/Speak\n\nPlease grant me the 'Connect' and 'Speak' permissions or move to another channel.")
                    .build()
            ).queue();
            return false;
        } else if (canConnect == Permissions.CONNECT_STATUS.USER_LIMIT) {
            channel.sendMessage(new EmbedBuilder()
                    .setColor(Bot.EmbedColour)
                    .setTitle("VoiceChannel Full")
                    .setDescription("Your VoiceChannel is full. Raise the user limit or grant me the 'Move Members' permission.")
                    .build()
            ).queue();
            return false;
        }

        if (!manager.isConnected() && !manager.isAttemptingToConnect()) {
            LOG.debug("Connecting to " + channel.getGuild().getId());
            manager.openAudioConnection(author.getVoiceState().getChannel());
            manager.setSelfDeafened(true);
        }

        return true;
    }

    public static QUEUE_STATUS CanQueue(AudioTrack track, String userID) {
        int permissionLevel = permissions.getTierLevel(userID);

        if (track.getInfo().isStream && permissionLevel < 1)
            return QUEUE_STATUS.IS_STREAM;

        if (!track.getInfo().isStream) {
            if (track.getDuration() / 1000 > DURATION_LIMIT_NORMAL && permissionLevel < 1)
                return QUEUE_STATUS.EXCEEDS_DURATION;
            if (track.getDuration() / 1000 > DURATION_LIMIT_PREMIUM)
                return QUEUE_STATUS.EXCEEDS_DURATION;
        }

        return QUEUE_STATUS.CAN_QUEUE;

    }

    public enum QUEUE_STATUS {
        CAN_QUEUE,
        IS_STREAM,
        EXCEEDS_DURATION
    }
}
