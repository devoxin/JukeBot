package me.devoxin.jukebot.utils

import java.io.InputStreamReader
import java.nio.file.Files
import java.nio.file.Paths
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.stream.Collectors
import kotlin.math.floor
import kotlin.math.round

object Helpers {
    val version: String by lazy {
        val stream = Helpers::class.java.classLoader.getResourceAsStream("version.txt")!!
        InputStreamReader(stream, Charsets.UTF_8).readText()
    }

    private val timer: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor { Thread(it, "JukeBot-Timer") }

    fun createBar(v: Int, max: Int, barLength: Int, bar: Char = '\u25AC', link: String = Constants.WEBSITE): String {
        val percent = v.toFloat() / max
        val blocks = floor((barLength * percent).toDouble()).toInt()
        val barChar = bar.toString()

        return buildString {
            val hasBlocks = blocks > 0

            if (hasBlocks) {
                append("[")
            }

            (0 until barLength).forEach {
                if (hasBlocks && it == blocks) {
                    append("]($link)")
                } else {
                    append(barChar)
                }
            }

            if (hasBlocks && (0 until barLength).none { it == blocks }) {
                append("]($link)")
            }
        }
    }

    fun schedule(task: () -> Unit, delay: Int, unit: TimeUnit) {
        timer.schedule(task, delay.toLong(), unit)
    }

    fun readFile(path: String, def: String): String {
        return try {
            Files.lines(Paths.get(path)).collect(Collectors.joining("\n"))
        } catch (e: Exception) {
            def
        }
    }

    fun calculateTier(pledgeAmount: Double): Int {
        return when {
            pledgeAmount >= 3 -> round(pledgeAmount).toInt()
            pledgeAmount >= 2 -> 2
            pledgeAmount >= 1 -> 1
            else -> 0
        }
    }

    fun rootCauseOf(ex: Throwable): Throwable {
        return ex.cause?.let(Helpers::rootCauseOf) ?: ex
    }
}
