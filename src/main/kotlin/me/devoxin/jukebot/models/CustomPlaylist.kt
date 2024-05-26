package me.devoxin.jukebot.models

import me.devoxin.jukebot.Database
import me.devoxin.jukebot.ExtendedAudioPlayerManager
import me.devoxin.jukebot.Launcher

class CustomPlaylist(val title: String, val creator: Long, tracks: String) {
    private val decodedTracks = tracks.split("\n")
        .asSequence()
        .filter(String::isNotEmpty)
        .map { Launcher.playerManager.runCatching { toAudioTrack(it) }.getOrNull() }

    val hasIncompatibleTracks: Boolean
        get() = decodedTracks.any { it == null }

    val tracks = decodedTracks.filterNotNull().toMutableList()

    private val encodedTracks: List<String>
        get() = tracks.map(Launcher.playerManager::toBase64String)

    fun save() {
        Database.updatePlaylist(creator, title, encodedTracks.joinToString("\n"))
    }
}
