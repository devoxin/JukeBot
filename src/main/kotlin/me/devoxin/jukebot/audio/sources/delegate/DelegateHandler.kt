package me.devoxin.jukebot.audio.sources.delegate

import com.sedmelluq.discord.lavaplayer.track.AudioTrack

class DelegateHandler(private val delegates: Set<DelegateSource>) {
    fun findByIsrc(isrc: String, vararg excluding: String): AudioTrack? {
        return delegates.asSequence()
            .filter { it.name !in excluding }
            .map { it.runCatching { findByIsrc(isrc) }.getOrNull() }
            .firstOrNull()
    }

    fun findBySearch(query: String, original: AudioTrack, vararg excluding: String): AudioTrack? {
        return delegates.asSequence()
            .filter { it.name !in excluding }
            .map { it.runCatching { findBySearch(query, original) }.getOrNull() }
            .firstOrNull()
    }
}
