package jukebot.commands

import jukebot.audio.AudioHandler
import jukebot.utils.Command
import jukebot.utils.CommandProperties
import jukebot.utils.Context

import java.text.DecimalFormat

@CommandProperties(description = "Displays player statistics", aliases = ["ps"], developerOnly = true)
class PlayerStats : Command(ExecutionType.REQUIRE_MUTUAL) {

    private val dpFormatter = DecimalFormat("0.00")

    override fun execute(context: Context) {
        val player = context.getAudioPlayer()

        val packetLossPc = player.trackPacketLoss.toDouble() / player.trackPackets * 100

        context.channel.sendMessage("Dropped packets: " + player.trackPacketLoss +
                "\nSent packets: " + player.trackPackets +
                "\n% Lost: " + dpFormatter.format(packetLossPc)).queue()
    }

}
