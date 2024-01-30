package me.devoxin.jukebot.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

object Scopes {
    val IO = CoroutineScope(Dispatchers.IO)
}
