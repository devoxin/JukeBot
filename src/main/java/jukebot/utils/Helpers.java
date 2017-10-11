package jukebot.utils;

import com.sedmelluq.discord.lavaplayer.track.AudioTrack;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.managers.AudioManager;

import java.util.HashMap;
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

    public static VOICE_STATUS ConnectVoice(AudioManager manager, TextChannel channel, Member author) {

        if (!permissions.hasMutualVoiceChannel(author)) {
            channel.sendMessage(new EmbedBuilder()
                    .setColor(Bot.EmbedColour)
                    .setTitle("No Mutual VoiceChannel")
                    .setDescription("Join my VoiceChannel to use this command.")
                    .build()
            ).queue();
            return VOICE_STATUS.CANNOT_CONNECT;
        }

        Permissions.CONNECT_STATUS canConnect = permissions.canConnect(author.getVoiceState().getChannel());

        if (canConnect == Permissions.CONNECT_STATUS.NO_CONNECT_SPEAK) {
            channel.sendMessage(new EmbedBuilder()
                    .setColor(Bot.EmbedColour)
                    .setTitle("Invalid Channel Permissions")
                    .setDescription("Your VoiceChannel doesn't allow me to Connect/Speak\n\nPlease grant me the 'Connect' and 'Speak' permissions or move to another channel.")
                    .build()
            ).queue();
            return VOICE_STATUS.CANNOT_CONNECT;
        } else if (canConnect == Permissions.CONNECT_STATUS.USER_LIMIT) {
            channel.sendMessage(new EmbedBuilder()
                    .setColor(Bot.EmbedColour)
                    .setTitle("VoiceChannel Full")
                    .setDescription("Your VoiceChannel is full. Raise the user limit or grant me the 'Move Members' permission.")
                    .build()
            ).queue();
            return VOICE_STATUS.CANNOT_CONNECT;
        }

        if (!manager.isConnected() && !manager.isAttemptingToConnect()) {
            LOG.debug("Connecting to " + channel.getGuild().getId());
            manager.openAudioConnection(author.getVoiceState().getChannel());
            manager.setSelfDeafened(true);
            return VOICE_STATUS.CONNECTED;
        }

        return VOICE_STATUS.ALREADY_CONNECTED;
    }

    public static QUEUE_STATUS CanQueue(AudioTrack track, long userID) {
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

    public static void getAllDonators(TextChannel channel, HashMap<Long, String> donators) {
        executor.execute(() -> { // THREAD ABUUUUUSE
            Message m = channel.sendMessage("Please wait...").complete();

            StringBuilder t1 = new StringBuilder().append("\u200B"); // Fail-safe in case no donators exist in this tier
            StringBuilder t2 = new StringBuilder().append("\u200B"); // Fail-safe in case no donators exist in this tier
            StringBuilder t3 = new StringBuilder().append("\u200B"); // Fail-safe in case no donators exist in this tier

            for (HashMap.Entry<Long, String> entry : donators.entrySet()) {
                if ("1".equalsIgnoreCase(entry.getValue())) {
                    t1.append("`")
                            .append(entry.getKey()).append("` ")
                            .append(channel.getJDA().retrieveUserById(entry.getKey()).complete().getName())
                            .append("\n");
                } else if ("2".equalsIgnoreCase(entry.getValue())) {
                    t2.append("`")
                            .append(entry.getKey()).append("` ")
                            .append(channel.getJDA().retrieveUserById(entry.getKey()).complete().getName())
                            .append("\n");
                } else if ("3".equalsIgnoreCase(entry.getValue())) {
                    t3.append("`")
                            .append(entry.getKey()).append("` ")
                            .append(channel.getJDA().retrieveUserById(entry.getKey()).complete().getName())
                            .append("\n");
                }
            }

            m.editMessage(new EmbedBuilder()
                    .setColor(Bot.EmbedColour)
                    .addField("Tier 1", t1.toString(), true)
                    .addField("Tier 2", t2.toString(), true)
                    .addField("Tier 3", t3.toString(), true)
                    .build()
            ).queue();
        });
    }

    public enum QUEUE_STATUS {
        CAN_QUEUE,
        IS_STREAM,
        EXCEEDS_DURATION
    }

    public enum VOICE_STATUS {
        ALREADY_CONNECTED,
        CONNECTED,
        CANNOT_CONNECT
    }
}
