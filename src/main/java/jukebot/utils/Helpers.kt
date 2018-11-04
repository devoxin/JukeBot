package jukebot.utils


import jukebot.Database
import jukebot.JukeBot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.future.await
import java.io.BufferedReader
import java.io.FileReader
import java.io.IOException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.stream.Collectors
import kotlin.coroutines.EmptyCoroutineContext

class Helpers {

    companion object {

        private val timer = Executors.newSingleThreadScheduledExecutor { Thread(it, "JukeBot-Timer") }!!
        val monitor = Executors.newSingleThreadScheduledExecutor { Thread(it, "JukeBot-Pledge-Monitor") }!!

        fun parseNumber(num: String?, def: Int): Int {
            return num?.toIntOrNull() ?: def
        }

        public fun schedule(task: Runnable, delay: Int, unit: TimeUnit) {
            timer.schedule(task, delay.toLong(), unit)
        }

        public fun schedule(task: () -> Unit, delay: Int, unit: TimeUnit) {
            timer.schedule(task, delay.toLong(), unit)
        }

        fun readFile(path: String, def: String): String {
            try {
                FileReader(path).use { file -> BufferedReader(file).use { reader -> return reader.lines().collect(Collectors.joining("\n")) } }
            } catch (e: IOException) {
                return def
            }
        }

        fun dm(userId: String, message: String) {
            val user = JukeBot.shardManager.getUserById(userId) ?: return

            user.openPrivateChannel().queue {
                it.sendMessage(message)
                        .submit()
                        .handle { _, _ -> it.close().queue() }
            }
        }

        fun monitorPledges() {
            CoroutineScope(EmptyCoroutineContext).async {
                JukeBot.LOG.info("Checking pledges...")

                val future = CompletableFuture<List<PatreonUser>>()
                JukeBot.patreonApi.fetchPledgesOfCampaign("750822", future)

                val users = future.await()

                if (users.isEmpty()) {
                    dm("180093157554388993", "⚠  |  Unable to check pledges. Ensure key is valid!")
                    return@async JukeBot.LOG.warn("Scheduled pledge clean failed: No users to check")
                }

                Database.getDonorIds().forEach { id ->
                    val pledge = users.firstOrNull { it.discordId != null && it.discordId.toLong() == id }

                    if (pledge == null || pledge.isDeclined) {
                        Database.setTier(id, 0)
                        JukeBot.LOG.info("Removed $id from donors")
                    }
                }
            }
        }

    }

}
