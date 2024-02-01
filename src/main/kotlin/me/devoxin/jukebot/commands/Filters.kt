package me.devoxin.jukebot.commands

import com.github.natanbc.lavadsp.lowpass.LowPassPcmAudioFilter
import me.devoxin.flight.api.CommandFunction
import me.devoxin.flight.api.annotations.Command
import me.devoxin.flight.api.annotations.Describe
import me.devoxin.flight.api.annotations.Range
import me.devoxin.flight.api.annotations.SubCommand
import me.devoxin.flight.api.context.Context
import me.devoxin.flight.api.entities.Cog
import me.devoxin.jukebot.annotations.Checks.DJ
import me.devoxin.jukebot.annotations.Checks.Playing
import me.devoxin.jukebot.annotations.Prerequisites.RequireMutualVoiceChannel
import me.devoxin.jukebot.audio.filters.ReverbFilter
import me.devoxin.jukebot.extensions.audioPlayer
import me.devoxin.jukebot.extensions.createProgressBar
import me.devoxin.jukebot.extensions.embed
import me.devoxin.lavadspx.HighPassFilter
import me.devoxin.lavadspx.LowPassFilter
import me.devoxin.lavadspx.NormalizationFilter

class Filters : Cog {
    override fun name() = "Filters"

    @Command(aliases = ["filter"], description = "Apply audio filters.", guildOnly = true)
    fun filters(ctx: Context) {
        val cmd = ctx.invokedCommand as? CommandFunction
            ?: return

        val padLength = cmd.subcommands.keys.maxOf { it.length }

        val subcommands = cmd.subcommands.values.sortedBy { it.name }.joinToString("\n") {
            "`${it.name.padEnd(padLength)}` — ${it.properties.description}"
        }

        ctx.embed("Subcommand Required", subcommands)
    }

    @SubCommand(aliases = ["bb"], description = "Boost low frequencies.")
    @DJ(alone = true)
    @Playing
    @RequireMutualVoiceChannel
    fun bassboost(ctx: Context,
                  @Describe("The percentage to boost bass by.")
                  @Range(long = [0, 200]) percent: Int?) {
        val player = ctx.audioPlayer
            ?: return ctx.embed("No Audio Player", "There's no audio player for this server.")

        val booster = player.bassBooster

        if (percent == null) {
            return ctx.embed("Audio Filters (Bass Boost)", "${booster.percentage.createProgressBar(200, 10)} `${booster.percentage}%`")
        }

        booster.boost(percent)
        ctx.embed("Audio Filters (Bass Boost)", "${percent.createProgressBar(200, 10)} `$percent%`")
    }

    @SubCommand(aliases = ["n", "norm"], description = "Attenuates volume peaks above a threshold.")
    @DJ(alone = true)
    @Playing
    @RequireMutualVoiceChannel
    fun normalization(ctx: Context,
                      @Describe("The maximum volume threshold.")
                      @Range(double = [0.0, 1.0]) maxAmplitude: Double) {
        val player = ctx.audioPlayer
            ?: return ctx.embed("No Audio Player", "There's no audio player for this server.")

        if (maxAmplitude == 0.0) {
            player.player.setFilterFactory(null)
            return ctx.embed("Audio Filters (Normalization)", "Audio filter disabled.")
        }

        player.player.setFilterFactory { _, _, output ->
            listOf(NormalizationFilter(output, maxAmplitude.toFloat(), true))
        }

        ctx.embed("Audio Filters (Normalization)", "Audio filter enabled.\nMax volume set to `${maxAmplitude * 100}%`.")
    }

    @SubCommand(aliases = ["lp"], description = "Filter out high frequencies.")
    @DJ(alone = true)
    @Playing
    @RequireMutualVoiceChannel
    fun lowPass(ctx: Context,
                @Describe("The frequency above which audio will be attenuated.")
                @Range(long = [30, 10000]) cutoffFrequency: Int?) {
        val player = ctx.audioPlayer
            ?: return ctx.embed("No Audio Player", "There's no audio player for this server.")

        if (cutoffFrequency == null) {
            player.player.setFilterFactory(null)
            return ctx.embed("Audio Filters (Low Pass)", "Audio filter disabled.")
        }

        player.player.setFilterFactory { _, format, output ->
            listOf(LowPassFilter(output, format.sampleRate, format.channelCount, cutoffFrequency))
        }

        ctx.embed("Audio Filters (Low Pass)", "Audio filter enabled.\nCut-off frequency set to `${cutoffFrequency} Hz`.")
    }

    @SubCommand(aliases = ["hp"], description = "Filter out low frequencies.")
    @DJ(alone = true)
    @Playing
    @RequireMutualVoiceChannel
    fun highPass(ctx: Context,
                 @Describe("The frequency below which audio will be attenuated.")
                 @Range(long = [1000, 18000]) cutoffFrequency: Int?) {
        val player = ctx.audioPlayer
            ?: return ctx.embed("No Audio Player", "There's no audio player for this server.")

        if (cutoffFrequency == null) {
            player.player.setFilterFactory(null)
            return ctx.embed("Audio Filters (High Pass)", "Audio filter disabled.")
        }

        player.player.setFilterFactory { _, format, output ->
            listOf(HighPassFilter(output, format.sampleRate, format.channelCount, cutoffFrequency))
        }

        ctx.embed("Audio Filters (High Pass)", "Audio filter enabled.\nCut-off frequency set to `${cutoffFrequency} Hz`.")
    }

    @SubCommand(aliases = ["r", "rev"], description = "Creates a reverberation effect.")
    @DJ(alone = true)
    @Playing
    @RequireMutualVoiceChannel
    fun reverb(ctx: Context,
               @Describe("How quickly the effect decays.")
               @Range(double = [0.0, 10.0]) decay: Double) {
        val player = ctx.audioPlayer
            ?: return ctx.embed("No Audio Player", "There's no audio player for this server.")

        if (decay == 0.0) {
            player.player.setFilterFactory(null)
            return ctx.embed("Audio Filters (Reverb)", "Audio filter disabled.")
        }

        player.player.setFilterFactory { _, format, output ->
            listOf(ReverbFilter(output, decay.toFloat(), format.channelCount))
        }

        ctx.embed("Audio Filters (Reverb)", "Audio filter enabled.\nDecay set to `${decay}`.")
    }

    @SubCommand(aliases = ["s"], description = "Flattens the audio.")
    @DJ(alone = true)
    @Playing
    @RequireMutualVoiceChannel
    fun smoothing(ctx: Context,
                  @Describe("How much to flatten the audio by.")
                  @Range(long = [0]) multiplier: Int) {
        val player = ctx.audioPlayer
            ?: return ctx.embed("No Audio Player", "There's no audio player for this server.")

        if (multiplier == 0) {
            player.player.setFilterFactory(null)
            return ctx.embed("Audio Filters (Smoothing)", "Audio filter disabled.")
        }

        player.player.setFilterFactory { _, format, output ->
            listOf(LowPassPcmAudioFilter(output, format.channelCount).setSmoothing(multiplier.toFloat()))
        }

        ctx.embed("Audio Filters (Smoothing)", "Audio filter enabled.\nMultiplier set to `${multiplier}`.")
    }
}
