package me.devoxin.jukebot.audio.sources.delegate

import com.sedmelluq.discord.lavaplayer.track.AudioTrack

class DelegateHandler(private val delegates: Set<DelegateSource>) {
    fun findByIsrc(isrc: String, prefer: Class<out DelegateSource>?, vararg excluding: String): AudioTrack? {
        return getDelegates(prefer)
            .filter { it.name !in excluding }
            .map { it.runCatching { findByIsrc(isrc) }.getOrNull() }
            .firstOrNull()
    }

    fun findBySearch(query: String, original: AudioTrack, prefer: Class<out DelegateSource>?, vararg excluding: String): AudioTrack? {
        return getDelegates(prefer)
            .filter { it.name !in excluding }
            .map { it.runCatching { findBySearch(query, original) }.getOrNull() }
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
}
