package me.devoxin.jukebot.audio.sources.delegate

import com.sedmelluq.discord.lavaplayer.track.AudioTrack

interface DelegateSource {
    val name: String

    fun findByIsrc(isrc: String): AudioTrack?

    fun findBySearch(query: String, original: AudioTrack): AudioTrack?
}
