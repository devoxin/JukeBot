//package me.devoxin.jukebot.commands.old.misc
//
//import me.devoxin.jukebot.audio.AudioHandler
//import me.devoxin.jukebot.annotations.Command
//import me.devoxin.jukebot.annotations.Checks
//import me.devoxin.jukebot.annotations.CommandProperties
//import me.devoxin.jukebot.annotations.Context
//import java.text.DecimalFormat
//import kotlin.math.abs
//
//@CommandProperties(description = "Displays player statistics", aliases = ["pm"], developerOnly = true)
//@Checks.Playing
//class PlayerStats : Command(ExecutionType.REQUIRE_MUTUAL) {
//    private val dpFormatter = DecimalFormat("0.00")
//
//    override fun execute(context: Context) {
//        val player = context.audioPlayer
//
//        val packetsLost = player.trackPacketLost.toDouble() / player.trackPacketsSent * 100
//        val packetLossPc = dpFormatter.format(packetsLost)
//
//        val trackProgress = player.player.playingTrack.position.toDouble() / 60000
//        val totalPackets = player.trackPacketsSent + player.trackPacketLost
//        val expectedPackets = AudioHandler.EXPECTED_PACKET_COUNT_PER_MIN * trackProgress
//        val framesDeficit = abs(expectedPackets - totalPackets).toInt()
//
//        context.send(false, {
//            "Dropped packets: ${player.trackPacketLost} ($packetLossPc%)\n" +
//                "Deficit packets: $framesDeficit\n" +
//                "Sent packets: ${player.trackPacketsSent}"
//        })
//    }
//}
