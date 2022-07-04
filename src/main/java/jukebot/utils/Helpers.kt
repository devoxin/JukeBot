package jukebot.utils

import org.apache.commons.io.IOUtils
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
        val stream = Helpers::class.java.classLoader.getResourceAsStream("version.txt")
        IOUtils.toString(stream, Charsets.UTF_8)
    }
    private val timer: ScheduledExecutorService =
        Executors.newSingleThreadScheduledExecutor { Thread(it, "JukeBot-Timer") }

    fun createBar(v: Int, max: Int, barLength: Int, bar: Char = '\u25AC', link: String = Constants.WEBSITE): String {
        val percent = v.toFloat() / max
        val blocks = floor((barLength * percent).toDouble()).toInt()
        val barChar = bar.toString()

        return buildString {
            append("[")
            val hasTerminator = (0 until barLength).any { it == blocks }
            val segments = (0 until barLength).map { if (it == blocks) "]($link)" else barChar }

            for (segment in segments) {
                append(segment)
            }

            if (!hasTerminator) {
                append("]($link)")
            }
        }
    }

    fun pad(s: String) = String.format("%-12s", s).replace(" ", " \u200B")

    fun schedule(task: () -> Unit, delay: Int, unit: TimeUnit) {
        timer.schedule(task, delay.toLong(), unit)
    }

    fun truncate(content: String, maxLength: Int): String {
        return if (content.length > maxLength) {
            content.substring(0, maxLength - 3) + "..."
        } else content
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
        return ex.cause?.let(::rootCauseOf) ?: ex
    }
}
