package jukebot.commands;

import jukebot.audio.AudioHandler;
import jukebot.utils.Command;
import jukebot.utils.CommandProperties;
import jukebot.utils.Context;

import java.text.DecimalFormat;

@CommandProperties(description = "Displays player statistics", aliases = {"ps"}, developerOnly = true)
public class PlayerStats implements Command {

    private DecimalFormat dpFormatter = new DecimalFormat("0.00");

    @Override
    public void execute(Context context) {
        AudioHandler player = context.getAudioPlayer();

        double packetLossPc = (double) player.trackPacketLoss / player.trackPackets * 100;

        context.getChannel().sendMessage("Dropped packets: " + player.trackPacketLoss +
                "\nSent packets: " + player.trackPackets +
                "\n% Lost: " + dpFormatter.format(packetLossPc)).queue();
    }

}
