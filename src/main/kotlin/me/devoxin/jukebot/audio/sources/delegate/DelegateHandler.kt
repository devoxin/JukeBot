package me.devoxin.jukebot.audio.sources.delegate

import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import org.slf4j.LoggerFactory

@Suppress("LoggingSimilarMessage")
class DelegateHandler(private val delegates: Set<DelegateSource>) {
    fun findByIsrc(isrc: String, prefer: Class<out DelegateSource>?, vararg excluding: String): AudioTrack? {
        log.debug("Finding delegate for isrc \"{}\" (prefer = {}, excluding = {})", isrc, prefer?.simpleName, excluding.joinToString(", "))

        return getDelegates(prefer)
            .onEach { log.debug("Matching delegate \"{}\" for isrc \"{}\"", it.name, isrc) }
            .filter { it.name !in excluding }
            .mapNotNull { it.runCatching { findByIsrc(isrc) }.getOrNull().also { t -> log.debug("Delegate \"{}\" yielded {}", it.name, t) } }
            .firstOrNull()
    }

    fun findBySearch(query: String, original: AudioTrack, prefer: Class<out DelegateSource>?, vararg excluding: String): AudioTrack? {
        log.debug("Finding delegate for query \"{}\" (prefer = {}, excluding = {})", query, prefer?.simpleName, excluding.joinToString(", "))

        return getDelegates(prefer)
            .onEach { log.debug("Matching delegate \"{}\" for query \"{}\"", it.name, query) }
            .filter { it.name !in excluding }
            .mapNotNull { it.runCatching { findBySearch(query, original) }.getOrNull().also { t -> log.debug("Delegate \"{}\" yielded {}", it.name, t) } }
            .firstOrNull()
    }

    private fun getDelegates(prefer: Class<out DelegateSource>?): Sequence<DelegateSource> = sequence {
        val preferredDelegate = prefer?.let { p -> delegates.firstOrNull { p.isAssignableFrom(it::class.java) } }

        if (preferredDelegate != null) {
            yield(preferredDelegate)
        }

        for (delegate in delegates) {
            if (prefer == null || delegate::class.java.isAssignableFrom(prefer)) {
                yield(delegate)
            }
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(DelegateHandler::class.java)
    }
}
