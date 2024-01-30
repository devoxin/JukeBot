package me.devoxin.jukebot.models

import me.devoxin.jukebot.Database
import me.devoxin.jukebot.Launcher

class CustomPlaylist(val title: String, val creator: Long, tracks: String) {
    val tracks = tracks.split("\n")
        .asSequence()
        .filter(String::isNotEmpty)
        .map(Launcher.playerManager::toAudioTrack)
        .toMutableList()

    private val encodedTracks: List<String>
        get() = tracks.map(Launcher.playerManager::toBase64String)

    fun save() {
        Database.updatePlaylist(creator, title, encodedTracks.joinToString("\n"))
    }
}
