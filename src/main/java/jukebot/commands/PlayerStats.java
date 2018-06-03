package jukebot.commands;

import jukebot.audioutilities.AudioHandler;
import jukebot.utils.Command;
import jukebot.utils.CommandProperties;
import jukebot.utils.Context;

@CommandProperties(description = "Displays player statistics", aliases = {"ps"})
public class PlayerStats implements Command {

    @Override
    public void execute(Context context) {
        AudioHandler player = context.getAudioPlayer();
        context.getChannel().sendMessage("Dropped packets: " + player.trackPacketLoss + "\nSent packets: " + player.trackPackets).queue();
    }

}
